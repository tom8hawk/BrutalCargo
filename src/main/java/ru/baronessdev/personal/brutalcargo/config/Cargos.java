package ru.baronessdev.personal.brutalcargo.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import ru.baronessdev.personal.brutalcargo.Main;
import ru.baronessdev.personal.brutalcargo.installation.CargoManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.bukkit.Bukkit.getServer;
import static ru.baronessdev.personal.brutalcargo.Main.executor;

public class Cargos extends YAML {
    public Cargos() {
        file = new File(plugin.getDataFolder() + "/cargos.yml");

        if (!file.exists())
            Initialize("cargos.yml");
    }

    public static void save() {
        Cargos cargo = new Cargos();
        List<CargoManager> cargos = new ArrayList<>(CargoManager.getCargos());

        cargos.forEach(rg -> {
            Location loc = rg.getLocation();
            cargo.configuration.set(rg.getRegionName(), List.of(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

            rg.delete();
        });
    }

    public static void deleteAll() {
        executor.execute(() -> {
            Cargos cargos = new Cargos();

            cargos.getKeys().parallelStream()
                    .map(cargos::getLocation)
                    .map(loc -> (Runnable) () -> loc.getBlock().setType(Material.AIR))
                    .forEach(task -> Bukkit.getScheduler().runTask(Main.inst, task));

            cargos.file.delete();
        });
    }

    private Location getLocation(String name) {
        List<String> locs = configuration.getStringList(name);
        return new Location(getServer().getWorld(locs.get(0)), Integer.parseInt(locs.get(1)), Integer.parseInt(locs.get(2)), Integer.parseInt(locs.get(3)));
    }

    private List<String> getKeys() {
        return new ArrayList<>(configuration.getKeys(false));
    }

    @Override
    public void load() {
        try {
            configuration.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}
