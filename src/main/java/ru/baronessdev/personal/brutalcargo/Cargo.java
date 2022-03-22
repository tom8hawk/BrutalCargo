package ru.baronessdev.personal.brutalcargo;

import io.papermc.lib.PaperLib;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.Vector;
import ru.baronessdev.personal.brutalcargo.config.Config;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.config.Messages;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.baronessdev.personal.brutalcargo.BrutalCargo.executor;

public class Cargo {
    private static final Random random = new Random();
    @Getter @Setter private static List<World> queue = new ArrayList<>();

    public static void schedule() {
        Config.inst.getKeys().parallelStream().map(Bukkit::getWorld).forEach(queue::add);

        executor.execute(() -> {
            while (true) {
                if (!queue.isEmpty()) {
                    Cargo.spawn(queue.get(0)).join();
                    queue.remove(0);
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

            while (true) {
                temp = new Location(world, random.nextInt(border), 1, random.nextInt(border)); // Получаем рандомные координаты
                highest = temp.getWorld().getHighestBlockAt(temp).getY(); // Получаем высоту первого нормального блока на этих координатах

                if (highest > 10 && Cuboid.getArea(temp, 60).parallelStream().noneMatch(l -> ru.baronessdev.personal.brutalprotect.region.Region.getByLocation(l) != null))
                    break;
            }

            Location loc = temp.clone();
            loc.setY(highest);

            RegionManager.create(loc); // Создаём приват
            List<Location> substrate = Cuboid.getArea(loc, 4); // Создаём подложку

            substrate.parallelStream() // Убираем рандомные блоки
                    .map(l -> (Runnable) () -> l.getBlock().setType(Material.AIR, true))
                    .filter(run -> (random.nextInt(100) + 1) <= 5)
                    .forEach(run -> Bukkit.getScheduler().runTask(BrutalCargo.inst, run));

            substrate.parallelStream() // Ставим рандомные блоки
                    .map(l -> (Runnable) () -> l.add(0, 1, 0).getBlock().setType(Material.NETHERRACK))
                    .filter(run -> (random.nextInt(100) + 1) > 10)
                    .forEach(run -> Bukkit.getScheduler().runTask(BrutalCargo.inst, run));

            Bukkit.getScheduler().runTask(BrutalCargo.inst, () -> loc.getBlock().setType(Material.RESPAWN_ANCHOR)); // Ставим на локацию якорь возрождения

            List<String> message = Messages.inst.getList("time-message"); // Получаем сообщение
            String coordinates = loc.getX() + " " + loc.getY() + " " + loc.getZ(); // Преобразуем X Y Z координаты в строку

            List<String> ignoredWorlds = Config.inst.getList(worldName + ".ignored-worlds"); // Получаем игнорируемые для сообщений миры
            AtomicInteger minutesTime = new AtomicInteger(10); // Получаем минуты кд

            IntStream.of(0, 5, 4).forEach(t -> laterTask(t, TimeUnit.MINUTES, () -> { // Запускаем сообщения по кд
                String time =  WordDeclensionUtil.MINUTES.getWordInDeclension(minutesTime.addAndGet(-t));
                List<Player> players = getPlayers(ignoredWorlds);

                message.stream()
                        .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                        .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
            }));

            AtomicInteger secondsTime = new AtomicInteger(60); // Получаем секунды кд

            IntStream.of(30, 15, 5, 5, 1, 1, 1, 1).forEach(t -> laterTask(t, TimeUnit.SECONDS, () -> { // Запускаем сообщения по кд
                String time = WordDeclensionUtil.SECONDS.getWordInDeclension(secondsTime.addAndGet(-t));
                List<Player> players = getPlayers(ignoredWorlds);

                message.stream()
                        .map(line -> line.replace("%world", cargoName).replace("%time", time).replace("%coordinates", coordinates))
                        .forEach(line -> players.parallelStream().forEach(player -> player.sendMessage(line)));
            }));

            laterTask(1, TimeUnit.SECONDS, () -> Bukkit.getScheduler().runTask(BrutalCargo.inst, () -> { // Выводим в главный поток (это требует API)
                for (int i = 0; i < 2; i++)
                    loc.getWorld().createExplosion(loc, 6F, false, false); // Создаём взрыв

                loc.getBlock().setType(Material.RED_SHULKER_BOX); // Заменяем блок на шалкер

                BlockState state = PaperLib.getBlockState(loc.getBlock(), false).getState();
                InventoryHolder shulker = (InventoryHolder) state;

                Inventory inven = shulker.getInventory();
                Database.readInventory().thenAcceptAsync(inv -> inven.setContents(getRandomItems(inv.getContents()))); // Кладём в шалкер рандомные предметы
            }));

            queue.add(world); // Добавляем мир в конец очереди
        });
    }

    private static ItemStack[] getRandomItems(ItemStack[] elements) {
        List<ItemStack> returnList = Arrays.stream(elements).collect(Collectors.toList());
        Collections.shuffle(returnList);

        if (returnList.size() > 27)
            return returnList.subList(returnList.size() - 27, returnList.size()).toArray(ItemStack[]::new);

        return returnList.toArray(ItemStack[]::new);
    }

    private static List<Player> getPlayers(List<String> ignoredWorlds) {
        return Bukkit.getOnlinePlayers().parallelStream()
                .filter(p -> !ignoredWorlds.contains(p.getLocation().getWorld().getName()))
                .collect(Collectors.toList());
    }

    private static void laterTask(long time, TimeUnit unit, Runnable task) {
        try {
            Thread.sleep(unit.toMillis(time));
        } catch (InterruptedException ignored) { }

        task.run();
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
            Location loc1 = loc.clone();
            loc1.setY(loc.getY() - 1);

            Location loc2 = loc1.getBlock().getRelative(BlockFace.SOUTH_EAST, radius).getLocation(); // Вторая точка
            loc1 = loc1.getBlock().getRelative(BlockFace.NORTH_WEST, radius).getLocation(); // Первая точка

            return new Cuboid(loc1, loc2).getBlocks();
        }
    }
}
