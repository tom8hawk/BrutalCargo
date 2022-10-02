package ru.baronessdev.personal.brutalcargo;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import ru.baronessdev.personal.brutalcargo.config.Config;
import ru.baronessdev.personal.brutalcargo.config.Messages;
import ru.baronessdev.personal.brutalcargo.managers.CargoManager;
import ru.baronessdev.personal.brutalcargo.listener.Explosion;
import ru.baronessdev.personal.brutalprotect.region.Region;
import ru.baronessdev.personal.brutalprotect.selection.Selection;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.baronessdev.personal.brutalcargo.Main.executor;
import static ru.baronessdev.personal.brutalcargo.Main.getPlayers;

public class CargoSpawner {
    private static final Random random = new Random();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private CargoSpawner() {
        throw new IllegalStateException("Utility class");
    }

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
                    AtomicLong hours = new AtomicLong(TimeUnit.MINUTES.toHours(time));

                    while (hours.get() > 0) {
                        runTaskLater(1, TimeUnit.HOURS, () -> {
                            String message = leftTime.replace("%time", WordDeclensionUtil.HOURS.getWordInDeclension(hours.get()));
                            getPlayers(ignoredWorlds).parallelStream().forEach(p -> p.sendMessage(message));

                            hours.addAndGet(-1);
                        });
                    }

                    runTaskLater(1, TimeUnit.HOURS, () -> CargoSpawner.spawn(world));
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
            temp = new Location(world, randomCoords(first), 70, randomCoords(second)); // Получаем рандомные координаты
            PaperLib.getChunkAtAsync(temp, true).join();
            int highest = temp.getWorld().getHighestBlockAt(temp).getY(); // Получаем высоту первого нормального блока на этих координатах

            if (highest > 2) {
                temp.setY(highest);
                Region tempRegion = Selection.setBlock(null, temp, 60).get();

                if (temp.getBlock().getType() != Material.WATER && temp.getBlock().getType() != Material.LAVA &&
                        regions.stream().noneMatch(rg -> rg.isRegionConcernsRegion(tempRegion))) {
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

        String coordinates = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ(); // X Y Z в строку
        List<String> ignoredWorlds = Config.inst.getList(worldName + ".ignored-worlds"); // Получаем игнорируемые для сообщений миры

        AtomicInteger minutesTime = new AtomicInteger(10); // Минуты кд
        AtomicInteger secondsTime = new AtomicInteger(60); // Секунды кд

        IntStream.of(0, 5, 4).forEach(t -> runTaskLater(t, TimeUnit.MINUTES, () -> { // Запускаем сообщения по кд
            String time =  WordDeclensionUtil.MINUTES.getWordInDeclension(minutesTime.addAndGet(-t));
            List<Player> players = getPlayers(ignoredWorlds);

            message.stream()
                    .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                    .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
        }));

        IntStream.of(30, 15, 5, 5, 1, 1, 1, 1).forEach(t -> runTaskLater(t, TimeUnit.SECONDS, () -> { // Запускаем сообщения по кд
            String time = WordDeclensionUtil.SECONDS.getWordInDeclension(secondsTime.addAndGet(-t));
            List<Player> players = getPlayers(ignoredWorlds);

            message.stream()
                    .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                    .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
        }));

        AtomicReference<Explosion> explosionRes = new AtomicReference<>();

        // Взрываем ТНТ
        Bukkit.getScheduler().runTask(Main.inst, () -> {
            loc.getBlock().setType(Material.AIR);
            loc.clone().add(0, -1, 0).getBlock().setType(Material.DIRT);

            TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);
            tnt.setFuseTicks(1);

            explosionRes.set(new Explosion(loc));
        });

        Explosion explosion;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while ((explosion = explosionRes.get()) == null);

        List<Location> substrate = explosion.getExplodedBlocks().stream() // Получаем взорванные блоки
                .map(Block::getLocation)
                .collect(Collectors.toList());

        explosion.delete();
        cargoManager.getExplodedBlocks().addAll(substrate);

        // Ставим рандомные блоки
        setBlocks(substrate, Material.SOUL_SAND, 30);
        setBlocks(substrate, Material.MAGMA_BLOCK, 25);
        setBlocks(substrate, Material.BLACKSTONE, 20);
        setBlocks(substrate, Material.NETHER_WART_BLOCK, 15);
        setBlocks(substrate, Material.AIR, 10);
        setBlocks(substrate, Material.NETHERRACK);

        Bukkit.getScheduler().runTask(Main.inst, () -> loc.getBlock().setType(Material.RED_SHULKER_BOX));
        cargoManager.createContent(ignoredWorlds); // Наполняем шалкер вещами

        runTaskLater(15, TimeUnit.MINUTES, () -> Bukkit.getScheduler().runTask(Main.inst, () ->
                loc.getBlock().setType(Material.AIR))); // Убираем шалкер
    }

    private static void setBlocks(List<Location> allBlocks, Material block, int chance) {
        List<Location> toDelete = allBlocks.stream()
                .filter(run -> (random.nextInt(100) + 1) <= chance)
                .collect(Collectors.toList());

        allBlocks.removeAll(toDelete);
        setBlocks(toDelete, block);
    }

    private static void setBlocks(List<Location> toSet, Material block) {
        Bukkit.getScheduler().runTask(Main.inst, () ->
                toSet.forEach(l -> l.getBlock().setType(block, true)));
    }

    private static void runTaskLater(long time, TimeUnit unit, Runnable task) {
        try {
            scheduler.schedule(task, time, unit).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static int randomCoords(int first) {
        int second = random.nextBoolean() ? 5000 : -5000;

        int min = Math.min(first, second);
        int max = Math.max(first, second);

        max -= min;
        return (int) (Math.random() * ++max) + min;
    }
}
