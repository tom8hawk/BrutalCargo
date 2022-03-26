package ru.baronessdev.personal.brutalcargo;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.baronessdev.personal.brutalcargo.BrutalCargo.executor;

public class RegionManager {
    @Getter private static final List<RegionManager> regions = new ArrayList<>();

    @Getter @Setter private Location location;
    @Getter @Setter private Region region;

    public static void create(Location loc) {
        executor.execute(() -> {
            RegionManager manager = new RegionManager();
            manager.setLocation(loc);

            Selection selection = new Selection();
            selection.setBlock(loc, 60);

            while (!selection.isShutdowned());

            manager.setRegion(Region.getByLocation(loc));
            Holographic.delete(manager.getRegion());


//            while (true) {
//                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
//
//                if (players.size() > 0) {
//                    Selection selection = new Selection(players.get(0));
//                    selection.setBlock(loc, 60);
//
//                    while (!selection.isShutdowned());
//
//                    manager.setRegion(Region.getByLocation(loc));
//                    Holographic.delete(manager.getRegion());
//
//                    break;
//                }
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            regions.add(manager);
            Bukkit.getOnlinePlayers().forEach(manager.getRegion()::addMember);
        });
    }

    public static void addToAll(Player player) {
        executor.execute(() ->
                new ArrayList<>(regions).parallelStream()
                    .map(RegionManager::getRegion)
                    .filter(rg -> !rg.getMembers().stream()
                            .map(OfflinePlayer::getUniqueId)
                            .collect(Collectors.toList())
                            .contains(player.getUniqueId()))
                    .forEach(rg -> rg.addMember(player))
        );
    }

    public static Optional<RegionManager> getByLocation(Location location) {
        return regions.parallelStream()
                .filter(rg -> rg.getLocation().equals(location))
                .findFirst();
    }

    public void delete() {
        region.remove();
    }
}
