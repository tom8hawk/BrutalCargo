package ru.baronessdev.personal.brutalcargo.config;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import ru.baronessdev.personal.brutalcargo.BrutalCargo;

import java.io.*;
import java.util.concurrent.CompletableFuture;

public class Database {
    private static String data;

    public Database() {
        File file = new File(BrutalCargo.inst.getDataFolder() + "/data.dat");

        if (!file.exists())
            BrutalCargo.inst.saveResource("data.dat", false);

        data = file.getAbsolutePath();
    }

    public static void saveInventory(Inventory inventory) {
        CompletableFuture.runAsync(() -> {
            try {
                FileOutputStream fout = new FileOutputStream(data);
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(fout);

                dataOutput.writeObject(inventory.getContents());

                dataOutput.close();
                fout.close();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to save item stacks.", e);
            }
        });
    }

    public static CompletableFuture<Inventory> readInventory() {
        return CompletableFuture.supplyAsync(() -> {
            Inventory inventory = Bukkit.getServer().createInventory(null, 54, Messages.inst.getMessage("menu-title"));

            try {
                FileInputStream inputStream = new FileInputStream(data);
                BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

                try {
                    Object object = dataInput.readObject();

                    if (object != null)
                        inventory.setContents((ItemStack[]) object);
                } catch (EOFException ignored) { }

                dataInput.close();
                inputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            return inventory;
        });
    }
}
