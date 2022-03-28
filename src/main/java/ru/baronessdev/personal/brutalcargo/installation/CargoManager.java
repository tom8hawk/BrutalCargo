package ru.baronessdev.personal.brutalcargo.installation;

import lombok.Getter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class CargoManager {
    @Getter private static final List<CargoManager> cargos = new ArrayList<>();
    @Getter private final Location location;

    @Getter private RegionManager regionManager;
    @Getter private ContentManager content;

    public CargoManager(Location location) {
        this.location = location;
    }

    public void createContent() {

    }

    public void createRegion() {
        regionManager = new RegionManager(this);

        try {
            regionManager.create().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        regionManager.delete();

        cargos.remove(this);
    }

    public static Optional<CargoManager> getByLocation(Location location) {
        return cargos.parallelStream()
                .filter(c -> c.getLocation().equals(location))
                .findFirst();
    }
}
