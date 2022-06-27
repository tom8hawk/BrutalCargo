package ru.baronessdev.personal.brutalcargo.config;

import org.bukkit.ChatColor;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class Messages extends YAML {
    public static Messages inst;

    public Messages() {
        initialize("messages.yml");
        inst = this;
    }

    public String getMessage(String path) {
        String result = configuration.getString(path);
        return ChatColor.translateAlternateColorCodes('&', result != null ? result : "");
    }

    public List<String> getList(String path) {
        return configuration.getStringList(path).stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
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
