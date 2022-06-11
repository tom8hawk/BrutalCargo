package ru.baronessdev.personal.brutalcargo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import ru.baronessdev.personal.brutalcargo.data.Config;
import ru.baronessdev.personal.brutalcargo.data.Messages;
import ru.baronessdev.personal.brutalcargo.installation.CargoManager;
import ru.baronessdev.personal.brutalcargo.listener.Explosion;
import ru.baronessdev.personal.brutalprotect.region.Region;
import ru.baronessdev.personal.brutalprotect.selection.Selection;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.baronessdev.personal.brutalcargo.Main.executor;
import static ru.baronessdev.personal.brutalcargo.Main.getPlayers;

public class CargoSpawner {
    private static final Random random = new Random();

    public static void schedule() {
        executor.execute(() -> {
            List<World> queue = new ArrayList<>();

            List<String> configWorlds = Config.inst.getKeys().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            Bukkit.getWorlds().parallelStream()
                    .filter(world -> configWorlds.contains(world.getName().toLowerCase()))
                    .forEach(queue::add);

            String leftTime = Messages.inst.getMessage("left-time-message");

            if (!queue.isEmpty()) {
                while (true) {
                    Collections.shuffle(queue);

                    World world = queue.get(0);
                    List<String> ignoredWorlds = Config.inst.getList(world.getName() + ".ignored-worlds");

                    int time = Config.inst.getInt(world.getName() + ".cooldown");
                    long hours = TimeUnit.MINUTES.toHours(time);

                    while (hours > 0) {
                        try {
                            Thread.sleep(3600000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        String message = leftTime.replace("%time", WordDeclensionUtil.HOURS.getWordInDeclension(hours));

                        getPlayers(ignoredWorlds).parallelStream().forEach(p -> p.sendMessage(message));
                        hours -= 1;
                    }

                    try {
                        Thread.sleep(3600000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    CargoSpawner.spawn(world);
                }
            }
        });
    }

    public static void spawn(World world) {
        String worldName = world.getName();
        String cargoName = Config.inst.getMessage(worldName + ".name");

        List<Integer> mins = Arrays.stream(Config.inst.getMessage(worldName + ".min-cords").split(" "))
                .map(Integer::valueOf)
                .collect(Collectors.toList());

        int first = mins.get(0);
        int second = mins.get(1);

        Location temp;
        CargoManager cargoManager;

        List<Region> regions = new ArrayList<>(Region.getRegions());

        while (true) {
            temp = new Location(world, random(first, 5000), 70, random(second, 5000)); // Получаем рандомные координаты
            int highest = temp.getWorld().getHighestBlockAt(temp).getY(); // Получаем высоту первого нормального блока на этих координатах

            if (highest > 2) {
                temp.setY(highest);
                Region tempRegion = Selection.setBlock(null, temp, 60).get();

                if (regions.stream().noneMatch(rg -> rg.isRegionConcernsRegion(tempRegion))) {
                    cargoManager = new CargoManager(temp);
                    cargoManager.setRegion(tempRegion);

                    break;
                } else {
                    tempRegion.remove();
                }
            }
        }

        Location loc = temp.clone();
        Bukkit.getScheduler().runTask(Main.inst, () -> loc.getBlock().setType(Material.RESPAWN_ANCHOR)); // Ставим на локацию якорь возрождения

        List<String> message = Messages.inst.getList("time-message"); // Получаем сообщение
        String coordinates = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ(); // Преобразуем X Y Z координаты в строку

        List<String> ignoredWorlds = Config.inst.getList(worldName + ".ignored-worlds"); // Получаем игнорируемые для сообщений миры
        AtomicInteger minutesTime = new AtomicInteger(10); // Получаем минуты кд

        IntStream.of(0, 5, 4).forEach(t -> sleepAndCompleteTask(t, TimeUnit.MINUTES, () -> { // Запускаем сообщения по кд
            String time =  WordDeclensionUtil.MINUTES.getWordInDeclension(minutesTime.addAndGet(-t));
            List<Player> players = getPlayers(ignoredWorlds);

            message.stream()
                    .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                    .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
        }));

        AtomicInteger secondsTime = new AtomicInteger(60); // Получаем секунды кд

        IntStream.of(30, 15, 5, 5, 1, 1, 1, 1).forEach(t -> sleepAndCompleteTask(t, TimeUnit.SECONDS, () -> { // Запускаем сообщения по кд
            String time = WordDeclensionUtil.SECONDS.getWordInDeclension(secondsTime.addAndGet(-t));
            List<Player> players = getPlayers(ignoredWorlds);

            message.stream()
                    .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                    .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
        }));

        Bukkit.getScheduler().runTask(Main.inst, () -> loc.getBlock().setType(Material.AIR)); // Убираем якорь возрождения
        AtomicReference<Explosion> explosion = new AtomicReference<>();

        Bukkit.getScheduler().runTask(Main.inst, () -> { // Взрываем ТНТ
            TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);
            tnt.setFuseTicks(1);

            explosion.set(new Explosion(tnt));
        });

        while (true) // Ожидаем взрыв
            if (explosion.get() != null && !explosion.get().getExplodedBlocks().isEmpty())
                break;

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        explosion.get().getExplodedBlocks().stream() // Получаем BlockState взорванных блоков
                .map(Block::getState)
                .forEach(cargoManager.getExplodedBlocksStates()::add);

        List<Location> substrate = explosion.get().getExplodedBlocks().stream() // Получаем взорванные блоки
                .map(Block::getLocation)
                .collect(Collectors.toList());

        // Ставим рандомные блоки
        setRandomBlocks(substrate, Material.SOUL_SAND, 13);
        setRandomBlocks(substrate, Material.MAGMA_BLOCK, 10);
        setRandomBlocks(substrate, Material.BLACKSTONE, 8);
        setRandomBlocks(substrate, Material.NETHER_WART_BLOCK, 5);

        Bukkit.getScheduler().runTask(Main.inst, () -> loc.getBlock().setType(Material.RED_SHULKER_BOX));
        cargoManager.createContent(ignoredWorlds); // Наполняем шалкер вещами

        sleepAndCompleteTask(15, TimeUnit.MINUTES, () -> Bukkit.getScheduler().runTask(Main.inst, () ->
                loc.getBlock().setType(Material.AIR))); // Убираем шалкер

        cargoManager.getExplodedBlocksStates().stream()
                .map(state -> (Runnable) () -> state.update(true, true))
                .forEach(task -> Bukkit.getScheduler().runTask(Main.inst, task));
    }

    private static void setRandomBlocks(List<Location> substrate, Material block, int chance) {
        substrate.parallelStream()
                .map(l -> (Runnable) () -> l.getBlock().setType(block, true))
                .filter(run -> (random.nextInt(100) + 1) <= chance)
                .forEach(run -> Bukkit.getScheduler().runTask(Main.inst, run));
    }

    private static void sleepAndCompleteTask(long time, TimeUnit unit, Runnable task) {
        try {
            Thread.sleep(unit.toMillis(time));
        } catch (InterruptedException ignored) { }

        Bukkit.getScheduler().runTask(Main.inst, task);
    }

    public static int random(int min, int max) {
        max -= min;
        return (int) (Math.random() * ++max) + min;
    }
}
