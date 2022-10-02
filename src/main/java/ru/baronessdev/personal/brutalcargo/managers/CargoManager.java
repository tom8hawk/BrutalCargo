package ru.baronessdev.personal.brutalcargo.managers;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import ru.baronessdev.personal.brutalcargo.Main;
import ru.baronessdev.personal.brutalprotect.region.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CargoManager {
    @Getter private static final List<CargoManager> cargos = Collections.synchronizedList(new ArrayList<>());

    @Getter private final Location location;
    @Getter private ContentManager content;

    @Getter private final List<Location> explodedBlocks = Collections.synchronizedList(new ArrayList<>());

    @Getter private String regionName;
    @Getter private RegionManager regionManager;

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

    public void delete() {
        regionManager.getRegion().remove();
        cargos.remove(this);

        // При отключении сервера нельзя планировать задачи
        if (Bukkit.isPrimaryThread()) returnToAir();
        else Bukkit.getScheduler().runTask(Main.inst, this::returnToAir);
    }

    private void returnToAir() {
        getExplodedBlocks().forEach(l -> l.getBlock().setType(Material.AIR));
    }

    public static Optional<CargoManager> getByLocation(Location location) {
        return cargos.parallelStream()
                .filter(c -> c.getLocation().equals(location))
                .findFirst();
    }
}
