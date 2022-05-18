package ru.baronessdev.personal.brutalcargo;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.installation.CargoManager;
import ru.baronessdev.personal.brutalcargo.installation.RegionManager;

import java.util.HashMap;
import java.util.Map;

public class BukkitListener implements Listener {
    @Getter private static final Map<HumanEntity, InventoryView> views = new HashMap<>();

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (views.containsKey(e.getPlayer())) {
            Database.saveInventory(e.getInventory());
            views.remove(e.getPlayer());
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
                    .ifPresent(cargo -> {
                        e.setCancelled(true);

                        if (cargo.getContent() != null)
                            cargo.getContent().open(e.getPlayer());
                    });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(Main.inst, () -> RegionManager.addToAll(e.getPlayer()), 5L);
    }
}
