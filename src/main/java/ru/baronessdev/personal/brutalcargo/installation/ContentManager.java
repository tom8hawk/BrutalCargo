package ru.baronessdev.personal.brutalcargo.installation;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.baronessdev.personal.brutalcargo.Main;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.config.Messages;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ru.baronessdev.personal.brutalcargo.Main.executor;
import static ru.baronessdev.personal.brutalcargo.Main.getPlayers;

public class ContentManager implements InventoryProvider {
    @Getter private final CargoManager cargo;
    @Getter private final Location location;

    private Inventory content;
    private SmartInventory inventory;
    private int secs = 30;

    private Player opener;
    private boolean opened;

    public ContentManager(CargoManager cargo, Location location) {
        this.cargo = cargo;
        this.location = location;
    }

    public void create(List<String> ignoredWorlds) {
        executor.execute(() -> {
            String cargoTitle = Messages.inst.getMessage("cargo-title");

            inventory = SmartInventory.builder()
                    .provider(this)
                    .manager(Main.inventoryManager)
                    .size(4, 9)
                    .title(cargoTitle)
                    .build();

            HashSet<Player> players = new HashSet<>();

            executor.execute(() -> {
                content = Bukkit.createInventory(null, 27, cargoTitle);

                Database.readInventory()
                        .thenApplyAsync(res -> getRandomItems(res.getContents()))
                        .thenAcceptAsync(content::setContents);
            });

            while (secs > 0) {
                players.removeIf(p -> !p.isOnline() || Main.inventoryManager.getInventory(p).isEmpty());

                players.addAll(Bukkit.getOnlinePlayers().parallelStream()
                        .filter(p -> Main.inventoryManager.getInventory(p).isPresent())
                        .collect(Collectors.toList()));

                if (!players.isEmpty()) {
                    opener = (Player) players.toArray()[0];

                    secs--;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            opened = true;
            Bukkit.getScheduler().runTask(Main.inst, () -> players.forEach(this::open));

            executor.execute(() -> {
                while (true) {
                    if (content.isEmpty()) {
                        Bukkit.getScheduler().runTask(Main.inst, () -> {
                            location.getBlock().setType(Material.AIR);
                            content.getViewers().forEach(HumanEntity::closeInventory);
                        });

                        cargo.delete();
                        return;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            String openMessage = Messages.inst.getMessage("open-message")
                    .replace("%player", opener.getDisplayName());

            getPlayers(ignoredWorlds).parallelStream()
                    .forEach(p -> p.sendMessage(openMessage));
        });
    }

    private static ItemStack[] getRandomItems(ItemStack[] elements) {
        List<ItemStack> returnList = Arrays.stream(elements).collect(Collectors.toList());
        Collections.shuffle(returnList);

        if (returnList.size() > 27)
            return returnList.subList(returnList.size() - 27, returnList.size()).toArray(ItemStack[]::new);

        return returnList.toArray(ItemStack[]::new);
    }

    public void open(Player player) {
        if (opened) {
            player.openInventory(content);
        } else {
            inventory.open(player);
        }
    }

    @Override
    public void init(Player player, InventoryContents inventoryContents) {
        inventoryContents.fillBorders(ClickableItem.empty(getItem()));
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {
        inventoryContents.fillBorders(ClickableItem.empty(getItem()));
    }

    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(Messages.inst.getMessage("item-name")
                .replace("%time", String.valueOf(secs)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);

        return item;
    }
}
