package ru.baronessdev.personal.brutalcargo;

import lombok.Getter;
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
        List<World> queue = new ArrayList<>();
        Config.inst.getKeys().parallelStream().map(Bukkit::getWorld).forEach(queue::add);

        executor.execute(() -> {
            if (!queue.isEmpty()) {
                while (true) {
                    Collections.shuffle(queue);
                    CargoSpawner.spawn(queue.get(0)).join();
                }
            }
        });
    }

    private static CompletableFuture<Void> spawn(World world) {
        return CompletableFuture.runAsync(() -> {
            String worldName = world.getName();
            String cargoName = Config.inst.getMessage(worldName + ".name");

            try { // Ожидаем кд
                Thread.sleep(TimeUnit.MINUTES.toMillis(Config.inst.getInt(worldName + ".cooldown")));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int highest;
            Location temp;
            int border = (int) world.getWorldBorder().getSize() / 2; // Получаем координаты границ в каждую сторону

            List<Integer> mins = Arrays.stream(Config.inst.getMessage(worldName + ".min-cords").split(" "))
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());

            while (true) {
                temp = new Location(world,
                        random.ints(mins.get(0), border).findFirst().orElse(random.nextInt(border)), 1,
                        random.ints(mins.get(1), border).findFirst().orElse(random.nextInt(border))); // Получаем рандомные координаты
                highest = temp.getWorld().getHighestBlockAt(temp).getY(); // Получаем высоту первого нормального блока на этих координатах

                if (highest > 10 && Cuboid.getArea(temp, 60).parallelStream().noneMatch(l -> ru.baronessdev.personal.brutalprotect.region.Region.getByLocation(l).isPresent()))
                    break;
            }

            Location loc = temp.clone();
            loc.setY(highest);

            CargoManager cargoManager = new CargoManager(loc);
            cargoManager.createRegion(); // Создаём приват
            List<Location> substrate = Cuboid.getArea(loc, 4); // Создаём подложку

            substrate.parallelStream() // Ставим рандомные блоки
                    .map(l -> (Runnable) () -> l.getBlock().setType(Material.NETHERRACK))
                    .forEach(run -> Bukkit.getScheduler().runTask(Main.inst, run));

            substrate.parallelStream() // Убираем рандомные блоки
                    .map(l -> (Runnable) () -> l.getBlock().setType(Material.AIR, true))
                    .filter(run -> (random.nextInt(100) + 1) <= 15)
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

    public static class Cuboid {
        @Getter private final List<Location> blocks = new ArrayList<>();

        public Cuboid(Location location1, Location location2) {
            World world = location1.getWorld();

            int maxX = (Math.max(location1.getBlockX(), location2.getBlockX()));
            int minX = (Math.min(location1.getBlockX(), location2.getBlockX()));

            int maxY = (Math.max(location1.getBlockY(), location2.getBlockY()));
            int minY = (Math.min(location1.getBlockY(), location2.getBlockY()));

            int maxZ = (Math.max(location1.getBlockZ(), location2.getBlockZ()));
            int minZ = (Math.min(location1.getBlockZ(), location2.getBlockZ()));

            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++)
                    for (int y = minY; y <= maxY; y++)
                        blocks.add(new Location(world, x, y, z));
        }

        public static List<Location> getArea(Location loc, int radius) {
            Location loc1 = loc.getBlock().getRelative(BlockFace.NORTH_WEST, radius).getLocation(); // Первая точка
            Location loc2 = loc.getBlock().getRelative(BlockFace.SOUTH_EAST, radius).getLocation(); // Вторая точка

            return new Cuboid(loc1, loc2).getBlocks();
        }
    }
}
