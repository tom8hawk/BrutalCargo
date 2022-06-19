package ru.baronessdev.personal.brutalcargo.installation;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import ru.baronessdev.personal.brutalcargo.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CargoManager {
    @Getter private static final List<CargoManager> cargos = new ArrayList<>();
    @Getter private final Location location;

    @Getter private String regionName;

    @Getter private RegionManager regionManager;
    @Getter private ContentManager content;


    public CargoManager(Location location) {
        this.location = location;
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

    public void delete(boolean registerTasks) {
        if (registerTasks)
            Bukkit.getScheduler().runTask(Main.inst, () -> creationData.setBlock(location.getBlock()));
        else
            creationData.setBlock(location.getBlock());

        regionManager.getRegion().remove();
        cargos.remove(this);
    }

    public static Optional<CargoManager> getByLocation(Location location) {
        return cargos.parallelStream()
                .filter(c -> c.getLocation().equals(location))
                .findFirst();
    }
}
