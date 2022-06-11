package ru.baronessdev.personal.brutalcargo.installation;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.baronessdev.personal.brutalprotect.region.Region;

import java.util.ArrayList;

import static ru.baronessdev.personal.brutalcargo.Main.executor;

public class RegionManager {
    @Getter private final Region region;

    public RegionManager(Region region) {
        this.region = region;

        Bukkit.getOnlinePlayers().parallelStream().forEach(RegionManager::addToAll);
    }

    public static void addToAll(Player player) {
        executor.execute(() ->
                new ArrayList<>(CargoManager.getCargos()).forEach(cargo -> {
                    RegionManager manager = cargo.getRegionManager();

                    if (manager != null) {
                        Region region = manager.getRegion();

                        if (!region.isPlayerInRegion(player))
                            region.addMember(player);
                    }
                })
        );
    }
}
