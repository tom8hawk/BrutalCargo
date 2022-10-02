package ru.baronessdev.personal.brutalcargo;

import fr.minuskube.inv.InventoryManager;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import ru.baronessdev.personal.brutalcargo.config.Config;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.config.Messages;
import ru.baronessdev.personal.brutalcargo.managers.CargoManager;
import ru.baronessdev.personal.brutalcargo.listener.BukkitListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main extends JavaPlugin {
    public static Main inst;

    public static ExecutorService executor = Executors.newWorkStealingPool();
    public static InventoryManager inventoryManager;

    public Main() {
        inst = this;
    }

    @Override
    public void onEnable() {
        PaperLib.suggestPaper(this);

        new Config();
        new Messages();
        new Database();

        inventoryManager = new InventoryManager(this);
        inventoryManager.init();

        getCommand("cargo").setExecutor((CommandSender sender, org.bukkit.command.Command command, String label, String[] args) -> {
            executor.execute(() -> {
                if (sender.hasPermission("cargo.admin")) {
                    if (args.length >= 1) {
                        if (args[0].equalsIgnoreCase("reload")) {
                            new Config();
                            new Messages();
                            new Database();

                            sender.sendMessage(Messages.inst.getMessage("reload"));
                        } else if (args[0].equalsIgnoreCase("spawn")) {
                            World world = null;

                            if (args.length > 1) {
                                world = getServer().getWorld(args[1]);

                                if (world == null) {
                                    sender.sendMessage(Config.inst.getMessage("world-not-exists"));
                                    return;
                                }
                            } else if (sender instanceof Player) {
                                Player player = (Player) sender;
                                world = player.getWorld();
                            }

                            if (Config.inst.contains(world.getName()))
                                CargoSpawner.spawn(world);
                            else
                                sender.sendMessage(Config.inst.getMessage("not-supported-world"));
                        }
                    } else if (sender instanceof Player) {
                        Player player = (Player) sender;
                        Inventory inventory = Database.readInventory();

                        try {
                            BukkitListener.getViews().put(player, Bukkit.getScheduler()
                                    .callSyncMethod(this, () -> player.openInventory(inventory)).get());
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    sender.sendMessage(Config.inst.getMessage("no-permissions"));
                }
            });

            return true;
        });

       Bukkit.getPluginManager().registerEvents(new BukkitListener(), this);
       CargoSpawner.schedule();
    }

    public static List<Player> getPlayers(List<String> ignoredWorlds) {
        List<Player> players = new ArrayList<>();

        Bukkit.getWorlds().stream()
                .filter(w -> !ignoredWorlds.contains(w.getName()))
                .map(World::getPlayers)
                .forEach(players::addAll);

        return players;
    }

    @Override
    public void onDisable() {
        CargoManager.getCargos().forEach(manager -> {
            manager.getExplodedBlocks().add(manager.getLocation());
            manager.delete();
        });
    }
}
