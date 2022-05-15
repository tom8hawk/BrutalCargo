package ru.baronessdev.personal.brutalcargo.installation;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import ru.baronessdev.personal.brutalprotect.region.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CargoManager {
    @Getter private static final List<CargoManager> cargos = new ArrayList<>();

    @Getter private final Location location;
    @Getter private ContentManager content;

    @Getter private final BlockState creationState;
    @Getter private final List<BlockState> explodedBlocksStates = new ArrayList<>();

    @Getter private String regionName;
    @Getter private RegionManager regionManager;


    public CargoManager(Location location) {
        this.location = location;
        this.creationState = location.getBlock().getState();

        cargos.add(this);
    }

    public void createContent(List<String> ignoredWorlds) {
        content = new ContentManager(this, location);
        content.create(ignoredWorlds);
    }

    public void setRegion(Region region) {
        regionManager = new RegionManager(region);
        regionName = region.getName();
    }

    public void delete() {
        regionManager.getRegion().remove();
        cargos.remove(this);
    }

    public static Optional<CargoManager> getByLocation(Location location) {
        return cargos.parallelStream()
                .filter(c -> c.getLocation().equals(location))
                .findFirst();
    }
}
