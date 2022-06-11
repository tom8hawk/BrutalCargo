package ru.baronessdev.personal.brutalcargo.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.baronessdev.personal.brutalcargo.Main;

import java.io.*;

public class Database {
    private static File file;

    private Database() {
        throw new IllegalStateException("Utility class");
    }

    public static void init() {
        file = new File(Main.inst.getDataFolder().getPath() + "/data.json");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveInventory(Inventory inventory) {
        try (Writer writer = new FileWriter(file, false)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            mapper.writeValue(writer, inventory.getContents());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Inventory readInventory() {
        Inventory inventory = Bukkit.getServer().createInventory(
                null, 54, Messages.inst.getMessage("menu-title"));

        try {
            ItemStack[] items = new ObjectMapper().readValue(file, ItemStack[].class);
            if (items != null) inventory.addItem(items);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return inventory;
    }
}