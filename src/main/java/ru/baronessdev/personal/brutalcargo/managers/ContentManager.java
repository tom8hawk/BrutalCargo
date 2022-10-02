package ru.baronessdev.personal.brutalcargo.managers;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryListener;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.baronessdev.personal.brutalcargo.Main;
import ru.baronessdev.personal.brutalcargo.config.Database;
import ru.baronessdev.personal.brutalcargo.config.Messages;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static ru.baronessdev.personal.brutalcargo.Main.executor;
import static ru.baronessdev.personal.brutalcargo.Main.getPlayers;

public class ContentManager implements InventoryProvider {
    private final List<Player> openers = new CopyOnWriteArrayList<>();
    
    @Getter private final CargoManager cargo;
    @Getter private final Location location;

    private Inventory content;
    private SmartInventory inventory;

    private int secs = 30;
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
                    .size(3, 9)
                    .title(cargoTitle)
                    .listener(new InventoryListener(InventoryCloseEvent.class, o -> openers.remove((Player) ((InventoryCloseEvent) o).getPlayer())))
                    .build();

            content = Bukkit.createInventory(null, 27, cargoTitle);
            content.setContents(getRandomItems(Database.readInventory().getContents()));

            while (secs > 0) {
                if (!openers.isEmpty())
                    secs--;

                if (location.getBlock().getType() == Material.AIR) {
                    delete();
                    return;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
            
            opened = true;

            Player opener = openers.get(0);
            Bukkit.getScheduler().runTask(Main.inst, () -> openers.forEach(this::open));

            String openMessage = Messages.inst.getMessage("open-message")
                    .replace("%player", opener.getDisplayName());

            getPlayers(ignoredWorlds).parallelStream()
                    .forEach(p -> p.sendMessage(openMessage));

            while (location.getBlock().getType() != Material.AIR && !content.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }

            delete();
        });
    }

    private void delete() {
        Bukkit.getScheduler().runTask(Main.inst, () -> {
            if (location.getBlock().getType() != Material.AIR)
                location.getBlock().setType(Material.AIR);

            openers.stream().filter(Player::isOnline).forEach(Player::closeInventory);
            content.getViewers().forEach(HumanEntity::closeInventory);
        });

        cargo.delete();
    }

    private static ItemStack[] getRandomItems(ItemStack[] elements) {
        List<ItemStack> returnList = Arrays.stream(elements).collect(Collectors.toList());
        Collections.shuffle(returnList);

        if (returnList.size() > 6)
            return randomise(returnList.subList(returnList.size() - 6, returnList.size()));

        return randomise(returnList);
    }

    private static ItemStack[] randomise(List<ItemStack> elements) {
        List<ItemStack> contents = new ArrayList<>(Collections.nCopies(27, new ItemStack(Material.AIR)));

        List<Integer> results = new ArrayList<>();
        Random random = new Random();

        elements.forEach(item -> {
            while (true) {
                int loc = random.nextInt(27);

                if (!results.contains(loc)) {
                    contents.set(loc, item);
                    results.add(loc);

                    return;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        return contents.toArray(new ItemStack[0]);
    }

    public void open(Player player) {
        if (opened)
            player.openInventory(content);
        else
            inventory.open(player);

        openers.add(player);
    }

    @Override
    public void init(Player player, InventoryContents inventoryContents) {
        inventoryContents.fill(ClickableItem.empty(getItem()));
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {
        inventoryContents.fill(ClickableItem.empty(getItem()));
    }

    private ItemStack getItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(Messages.inst.getMessage("item-name")
                .replace("%time", String.valueOf(secs)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);

        return item;
    }
}
