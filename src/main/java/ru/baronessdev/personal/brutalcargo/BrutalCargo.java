package ru.baronessdev.personal.brutalcargo;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;
import ru.baronessdev.personal.brutalcargo.config.Config;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.config.Messages;
import ru.baronessdev.personal.brutalprotect.utils.Holographic;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BrutalCargo extends JavaPlugin {
    public static HashMap<HumanEntity, InventoryView> views = new HashMap<>();
    public static ExecutorService executor = Executors.newCachedThreadPool();
    public static BrutalCargo inst;

    public BrutalCargo() {
        inst = this;
    }

    @Override
    public void onEnable() {
        new Config();
        new Messages();
        new Database();

        getCommand("cargo").setExecutor((CommandSender sender, org.bukkit.command.Command command, String label, String[] args) -> {
            if (sender instanceof Player) {
                if (sender.hasPermission("cargo.admin")) {
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
                            .thenAcceptAsync(view -> views.put(view.getPlayer(), view));

                } else {
                    sender.sendMessage(Messages.inst.getMessage("no-permissions"));
                }
            } else {
                sender.sendMessage(Messages.inst.getMessage("not-player"));
            }

            return true;
        });

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onClose(InventoryCloseEvent e) {
                if (views.containsKey(e.getPlayer())) {
                    Database.saveInventory(e.getInventory());

                    views.remove(e.getPlayer());
                } else if (e.getInventory().getLocation() != null && e.getInventory().isEmpty()) {
                    RegionManager.getByLocation(e.getInventory().getLocation()).ifPresent(rg -> {
                        e.getInventory().getLocation().getBlock().setType(Material.AIR);

                        rg.delete();
                    });
                }
            }

            @EventHandler
            public void onBreak(BlockBreakEvent e) {
                RegionManager.getByLocation(e.getBlock().getLocation()).ifPresent(rg -> e.setCancelled(true));
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                Bukkit.getScheduler().runTaskLater(BrutalCargo.inst, () -> RegionManager.addToAll(e.getPlayer()), 5L);
            }
        }, this);

       Cargo.schedule();
    }

    @Override
    public void onDisable() {
        RegionManager.getRegions().forEach(rg -> {
            rg.getLocation().getBlock().setType(Material.AIR);

            rg.delete();
        });
    }
}
