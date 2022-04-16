package ru.baronessdev.personal.brutalcargo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import ru.baronessdev.personal.brutalcargo.config.Config;
import ru.baronessdev.personal.brutalcargo.config.Messages;
import ru.baronessdev.personal.brutalcargo.installation.CargoManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

                    CargoSpawner.spawn(world).join();
                }
            }
        });
    }

    public static CompletableFuture<Void> spawn(World world) {
        return CompletableFuture.runAsync(() -> {
            String worldName = world.getName();
            String cargoName = Config.inst.getMessage(worldName + ".name");

            int highest;
            Location temp;

            List<Integer> mins = Arrays.stream(Config.inst.getMessage(worldName + ".min-cords").split(" "))
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());

            while (true) {
                temp = new Location(world,
                        random.ints(mins.get(0), 2000).findFirst().orElse(random.nextInt(2000)), 1,
                        random.ints(mins.get(1), 2000).findFirst().orElse(random.nextInt(2000))); // Получаем рандомные координаты
                highest = temp.getWorld().getHighestBlockAt(temp).getY(); // Получаем высоту первого нормального блока на этих координатах

                if (highest > 10 && getArea(temp, 60).parallelStream().noneMatch(l -> ru.baronessdev.personal.brutalprotect.region.Region.getByLocation(l).isPresent()))
                    break;
            }

            Location loc = temp.clone();
            loc.setY(highest);

            CargoManager cargoManager = new CargoManager(loc);
            cargoManager.createRegion(); // Создаём приват
            List<Location> substrate = getArea(loc, 4); // Создаём подложку

            substrate.parallelStream() // Ставим подложку
                    .map(l -> (Runnable) () -> l.getBlock().setType(Material.NETHERRACK))
                    .forEach(run -> Bukkit.getScheduler().runTask(Main.inst, run));

            // Ставим рандомные блоки
            setRandomBlocks(substrate, Material.SOUL_SAND, 13);
            setRandomBlocks(substrate, Material.MAGMA_BLOCK, 10);
            setRandomBlocks(substrate, Material.BLACKSTONE, 8);
            setRandomBlocks(substrate, Material.NETHER_WART_BLOCK, 5);

            getArea(loc.clone().add(0, 1, 0), 5).parallelStream() // Убираем блоки над грузом
                    .map(l -> (Runnable) () -> l.getBlock().setType(Material.AIR))
                    .forEach(run -> Bukkit.getScheduler().runTask(Main.inst, run));

            Bukkit.getScheduler().runTask(Main.inst, () -> loc.getBlock().setType(Material.RESPAWN_ANCHOR)); // Ставим на локацию якорь возрождения

            List<String> message = Messages.inst.getList("time-message"); // Получаем сообщение
            String coordinates = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ(); // Преобразуем X Y Z координаты в строку

            List<String> ignoredWorlds = Config.inst.getList(worldName + ".ignored-worlds"); // Получаем игнорируемые для сообщений миры
            AtomicInteger minutesTime = new AtomicInteger(10); // Получаем минуты кд

            IntStream.of(0, 5, 4).forEach(t -> laterTaskAsync(t, TimeUnit.MINUTES, () -> { // Запускаем сообщения по кд
                String time =  WordDeclensionUtil.MINUTES.getWordInDeclension(minutesTime.addAndGet(-t));
                List<Player> players = getPlayers(ignoredWorlds);

                message.stream()
                        .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                        .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
            }));

            AtomicInteger secondsTime = new AtomicInteger(60); // Получаем секунды кд

            IntStream.of(30, 15, 5, 5, 1, 1, 1, 1).forEach(t -> laterTaskAsync(t, TimeUnit.SECONDS, () -> { // Запускаем сообщения по кд
                String time = WordDeclensionUtil.SECONDS.getWordInDeclension(secondsTime.addAndGet(-t));
                List<Player> players = getPlayers(ignoredWorlds);

                message.stream()
                        .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                        .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
            }));

            laterTask(1, TimeUnit.SECONDS, () -> Bukkit.getScheduler().runTask(Main.inst, () -> { // Выводим в главный поток (это требует API)
                for (int i = 0; i < 2; i++)
                    loc.getWorld().createExplosion(loc, 6F, false, false); // Создаём 2 взрыва

                loc.getBlock().setType(Material.RED_SHULKER_BOX); // Заменяем блок на шалкер
            }));

            cargoManager.createContent(ignoredWorlds);
        });
    }

    private static void setRandomBlocks(List<Location> substrate, Material block, int chance) {
        substrate.parallelStream()
                .map(l -> (Runnable) () -> l.getBlock().setType(block, true))
                .filter(run -> (random.nextInt(100) + 1) <= chance)
                .forEach(run -> Bukkit.getScheduler().runTask(Main.inst, run));
    }

    private static void laterTaskAsync(long time, TimeUnit unit, Runnable task) {
        try {
            Thread.sleep(unit.toMillis(time));
        } catch (InterruptedException ignored) { }

        executor.execute(task);
    }

    private static void laterTask(long time, TimeUnit unit, Runnable task) {
        try {
            Thread.sleep(unit.toMillis(time));
        } catch (InterruptedException ignored) { }

        Bukkit.getScheduler().runTask(Main.inst, task);
    }

    public static List<Location> getArea(Location loc, int radius) {
        Location loc1 = loc.getBlock().getRelative(BlockFace.NORTH_WEST, radius).getLocation(); // Первая точка
        Location loc2 = loc.getBlock().getRelative(BlockFace.SOUTH_EAST, radius).getLocation(); // Вторая точка

        List<Location> blocks = new ArrayList<>();
        World world = loc1.getWorld();

        int maxX = (Math.max(loc1.getBlockX(), loc2.getBlockX()));
        int minX = (Math.min(loc1.getBlockX(), loc2.getBlockX()));

        int maxY = (Math.max(loc1.getBlockY(), loc2.getBlockY()));
        int minY = (Math.min(loc1.getBlockY(), loc2.getBlockY()));

        int maxZ = (Math.max(loc1.getBlockZ(), loc2.getBlockZ()));
        int minZ = (Math.min(loc1.getBlockZ(), loc2.getBlockZ()));

        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                for (int y = minY; y <= maxY; y++)
                    blocks.add(new Location(world, x, y, z));

        return blocks;
    }
}
