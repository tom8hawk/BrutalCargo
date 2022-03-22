package ru.baronessdev.personal.brutalcargo.config;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.baronessdev.personal.brutalcargo.BrutalCargo;

import java.io.File;

public abstract class YAML {
    protected static final BrutalCargo plugin = BrutalCargo.inst;
    protected final YamlConfiguration configuration = new YamlConfiguration();
    protected File file;

    public void Initialize(String fileName) {
        String path = plugin.getDataFolder() + "/" + fileName;
        file = new File(path);

        if (!file.exists()) {
            plugin.saveResource(fileName, true);
        }
        load();
    }

    public abstract void load();
}
