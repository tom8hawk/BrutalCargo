package ru.baronessdev.personal.brutalcargo.config;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class Database extends YAML {
    public static Database inst;

    public Database() {
        initialize("data.yml");
        inst = this;
    }

    public static void saveInventory(Inventory inventory) {
        inst.configuration.set("contents", null);
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack item = inventory.getItem(i);

            if (item != null) {
                Map<String, Object> serialized = item.serialize();
                serialized.put("slot", i);

                data.add(serialized);
            }
        }

        try {
            inst.configuration.set("contents", data);
            inst.configuration.save(inst.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CompletableFuture<Inventory> readInventory() {
        return CompletableFuture.supplyAsync(() -> {
            Inventory inventory = Bukkit.getServer().createInventory(null, 54, Messages.inst.getMessage("menu-title"));

            if (inst.configuration.contains("contents"))
                inst.configuration.getMapList("contents").stream()
                        .map(map -> map.entrySet().stream().collect(Collectors.toMap(k -> (String) k.getKey(), v -> (Object) v.getValue())))
                        .forEach(map -> inventory.setItem((int) map.get("slot"), ItemStack.deserialize(map)));

            return inventory;
        });
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