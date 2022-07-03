package ru.baronessdev.personal.brutalcargo.config;

import org.bukkit.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Config extends YAML {
    public static Config inst;

    public Config() {
        initialize("config.yml");
        inst = this;
    }

    public String getMessage(String path) {
        String result = configuration.getString(path);
        return ChatColor.translateAlternateColorCodes('&', result != null ? result : "");
    }

    public List<String> getList(String path) {
        return configuration.getStringList(path).stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
    }

    public int getInt(String path) {
        return configuration.getInt(path);
    }

    public boolean contains(String path) {
        return configuration.contains(path);
    }

    public List<String> getKeys() {
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
