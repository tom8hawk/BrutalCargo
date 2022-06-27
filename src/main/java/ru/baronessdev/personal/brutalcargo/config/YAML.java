package ru.baronessdev.personal.brutalcargo.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

import static ru.baronessdev.personal.brutalcargo.Main.inst;

public abstract class YAML {
    protected final YamlConfiguration configuration = new YamlConfiguration();
    protected File file;

    public void initialize(String fileName) {
        String path = inst.getDataFolder() + "/" + fileName;
        file = new File(path);

        if (!file.exists())
            inst.saveResource(fileName, true);

        load();
    }

    public abstract void load();
}
