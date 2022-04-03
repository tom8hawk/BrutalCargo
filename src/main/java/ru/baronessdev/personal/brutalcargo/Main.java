package ru.baronessdev.personal.brutalcargo;

import fr.minuskube.inv.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;
import ru.baronessdev.personal.brutalcargo.config.Config;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.config.Messages;
import ru.baronessdev.personal.brutalcargo.installation.CargoManager;
import ru.baronessdev.personal.brutalcargo.installation.RegionManager;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin {
    public static Main inst;

    public static HashMap<HumanEntity, InventoryView> views = new HashMap<>();
    public static ExecutorService executor = Executors.newCachedThreadPool();
    public static InventoryManager inventoryManager;

    public Main() {
        inst = this;
    }

    @Override
    public void onEnable() {
        new Config();
        new Messages();
        new Database();

        inventoryManager = new InventoryManager(this);
        inventoryManager.init();

        getCommand("cargo").setExecutor((CommandSender sender, org.bukkit.command.Command command, String label, String[] args) -> {
            if (args.length >= 1) {
                if (sender.hasPermission("cargo.admin")) {
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
                        .thenAcceptAsync(view -> views.put(view.getPlayer(), view));
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
                    CargoManager.getByLocation(e.getInventory().getLocation()).ifPresent(rg -> {
                        e.getInventory().getLocation().getBlock().setType(Material.AIR);

                        rg.delete();
                    });
                }
            }

            @EventHandler
            public void onBreak(BlockBreakEvent e) {
                CargoManager.getByLocation(e.getBlock().getLocation()).ifPresent(rg -> e.setCancelled(true));
            }

            @EventHandler
            public void onInteract(PlayerInteractEvent e) {
                if (e.hasBlock())
                    CargoManager.getByLocation(e.getClickedBlock().getLocation())
                            .map(CargoManager::getContent)
                            .ifPresent(cargo -> {
                                e.setCancelled(true);
                                cargo.open(e.getPlayer());
                            });
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                Bukkit.getScheduler().runTaskLater(Main.inst, () -> RegionManager.addToAll(e.getPlayer()), 5L);
            }
        }, this);

       CargoSpawner.schedule();
    }

    public static List<Player> getPlayers(List<String> ignoredWorlds) {
        return Bukkit.getOnlinePlayers().parallelStream()
                .filter(p -> !ignoredWorlds.contains(p.getLocation().getWorld().getName()))
                .collect(Collectors.toList());
    }

    @Override
    public void onDisable() {
        CargoManager.getCargos().forEach(rg -> {
            rg.getLocation().getBlock().setType(Material.AIR);

            rg.delete();
        });
    }
}
