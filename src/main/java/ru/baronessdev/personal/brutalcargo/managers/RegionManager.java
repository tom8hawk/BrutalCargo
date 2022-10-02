package ru.baronessdev.personal.brutalcargo.managers;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.baronessdev.personal.brutalprotect.region.Region;

public class RegionManager {
    @Getter private final Region region;

    public RegionManager(Region region) {
        this.region = region;

        region.getMembers().addAll(Bukkit.getOnlinePlayers());
        region.save();
    }

    public static void addToAll(Player player) {
        CargoManager.getCargos().forEach(cargoManager -> {
            RegionManager manager = cargoManager.getRegionManager();

            if (manager != null)
                manager.getRegion().addMember(player);
        });
    }
}
