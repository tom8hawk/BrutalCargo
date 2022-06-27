package ru.baronessdev.personal.brutalcargo.installation;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.baronessdev.personal.brutalprotect.region.Region;

import java.util.ArrayList;
import java.util.Objects;

public class RegionManager {
    @Getter private final Region region;

    public RegionManager(Region region) {
        this.region = region;
        Bukkit.getOnlinePlayers().parallelStream().forEach(RegionManager::addToAll);
    }

    public static void addToAll(Player player) {
        new ArrayList<>(CargoManager.getCargos()).stream()
                .map(CargoManager::getRegionManager)
                .filter(Objects::nonNull)
                .map(RegionManager::getRegion)
                .forEach(rg -> rg.addMember(player));
    }
}
