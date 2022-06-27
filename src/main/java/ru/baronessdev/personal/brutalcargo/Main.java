package ru.baronessdev.personal.brutalcargo;

import fr.minuskube.inv.InventoryManager;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.baronessdev.personal.brutalcargo.config.Config;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.config.Messages;
import ru.baronessdev.personal.brutalcargo.installation.CargoManager;
import ru.baronessdev.personal.brutalcargo.listener.BukkitListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
                            if (sender instanceof Player) {
                                Player player = (Player) sender;

                                CargoSpawner.spawn(player.getWorld());
                            } else if (args.length > 1) {
                                World world = getServer().getWorld(args[1]);

                                if (world != null)
                                    CargoSpawner.spawn(world);
                            }
                        }
                    } else if (sender instanceof Player) {
                        Player player = (Player) sender;

                        Database.readInventory()
                                .thenApplyAsync(inv -> {
                                    try {
                                        return Bukkit.getScheduler().callSyncMethod(this, () -> player.openInventory(inv)).get();
                                    } catch (InterruptedException | ExecutionException e) {
                                        e.printStackTrace();
                                    }

                                    return null;
                                })
                                .thenAcceptAsync(view -> BukkitListener.getViews().put(view.getPlayer(), view));
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
        return Bukkit.getWorlds().stream()
                .filter(w -> !ignoredWorlds.contains(w.getName()))
                .map(World::getPlayers)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void onDisable() {
        new ArrayList<>(CargoManager.getCargos()).forEach(manager -> {
            List<BlockState> blockStates = new ArrayList<>(manager.getExplodedBlocksStates());
            blockStates.add(manager.getCreationState());

            blockStates.forEach(state -> state.update(true, true));
            manager.delete();
        });
    }
}
