package ru.baronessdev.personal.brutalcargo.config;

import me.lucko.helper.serialize.GsonStorageHandler;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.baronessdev.personal.brutalcargo.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class Database extends GsonStorageHandler<ItemStack[]> {
    private static Database inst;
    private static final Path path = Path.of(Main.inst.getDataFolder().getPath() + "/data.json");

    public Database() {
        super("data", ".json", Main.inst.getDataFolder(), ItemStack[].class);

        inst = this;
        File file = path.toFile();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveInventory(Inventory inventory) {
        inst.saveToFile(path, inventory.getContents());
    }

    public static Inventory readInventory() {
        Inventory inventory = Bukkit.getServer().createInventory(null, 54, Messages.inst.getMessage("menu-title"));
        ItemStack[] items = inst.readFromFile(path);

        if (items != null)
            inventory.setContents(items);

        return inventory;
    }
}
