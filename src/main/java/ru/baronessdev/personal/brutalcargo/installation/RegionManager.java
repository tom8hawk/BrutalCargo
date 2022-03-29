package ru.baronessdev.personal.brutalcargo.installation;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.baronessdev.personal.brutalprotect.region.Region;
import ru.baronessdev.personal.brutalprotect.selection.Selection;
import ru.baronessdev.personal.brutalprotect.utils.Holographic;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ru.baronessdev.personal.brutalcargo.Main.executor;

public class RegionManager {
    private final CargoManager cargoManager;
    @Getter @Setter private Region region;

    public RegionManager(CargoManager cargoManager) {
        this.cargoManager = cargoManager;
    }

    public static void addToAll(Player player) {
        executor.execute(() ->
                new ArrayList<>(CargoManager.getCargos()).parallelStream()
                        .map(CargoManager::getRegionManager)
                        .map(RegionManager::getRegion)
                        .filter(rg -> !rg.getMembers().stream()
                                .map(OfflinePlayer::getUniqueId)
                                .collect(Collectors.toList())
                                .contains(player.getUniqueId()))
                        .forEach(rg -> rg.addMember(player))
        );
    }

    public CompletableFuture<Void> create() {
        return CompletableFuture.runAsync(() -> {
            Location loc = cargoManager.getLocation();

            Selection selection = new Selection();
            selection.setBlock(loc, 60);

            while (!selection.isShutdowned());

            setRegion(Region.getByLocation(loc).get());
            Holographic.delete(region);

            Bukkit.getOnlinePlayers().parallelStream().forEach(RegionManager::addToAll);
        });
    }

    public void delete() {
        region.remove();
    }
}
