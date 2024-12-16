package dev.heypr.mythicinventories;

import dev.heypr.mythicinventories.bstats.Metrics;
import dev.heypr.mythicinventories.commands.OpenInventoryTabCompleter;
import dev.heypr.mythicinventories.events.BukkitInventoryEvents;
import dev.heypr.mythicinventories.events.MythicMobEvents;
import dev.heypr.mythicinventories.inventories.InventoryCreator;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.commands.OpenInventoryCommand;
import dev.heypr.mythicinventories.storage.MythicInventorySerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MythicInventories extends JavaPlugin implements Listener {

    // Format: internal inventory name -> MythicInventory object
    private final Map<String, MythicInventory> inventories = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("mythicinventoryopen").setExecutor(new OpenInventoryCommand(this));
        getCommand("mythicinventoryopen").setTabCompleter(new OpenInventoryTabCompleter(this));

        getCommand("mythicinventoryreload").setExecutor((sender, command, label, args) -> {
            reloadInventories();
            sender.sendMessage("Inventories reloaded!");
            return true;
        });

        Bukkit.getPluginManager().registerEvents(new BukkitInventoryEvents(this), this);

        if (!isMythicMobsEnabled()) {
            getLogger().warning("MythicMobs was not found! MythicInventories will have reduced functionality.");
        }
        else {
            Bukkit.getPluginManager().registerEvents(new MythicMobEvents(this), this);
        }

        createInventoriesDirectory();
        reloadInventories();

        new Metrics(this, 23863);

        getLogger().info("MythicInventories enabled!");
    }

    @Override
    public void onDisable() {
        inventories.clear();
        getLogger().info("MythicInventories disabled!");
    }

    /**
     * Get the inventory serializer.
     * @return The inventory serializer.
     */
    public MythicInventorySerializer getInventorySerializer() {
        return new MythicInventorySerializer(this);
    }

    /**
     * Reload all inventories.
     */
    public void reloadInventories() {
        inventories.clear();
        new InventoryCreator(this).loadInventories();
    }

    /**
     * Creates the "inventories" directory if it doesn't exist.
     */
    private void createInventoriesDirectory() {
        File inventoriesDir = new File(getDataFolder(), "inventories");
        if (!inventoriesDir.exists()) {
            inventoriesDir.mkdirs();
        }
    }

    /**
     * Get a map of all inventories.
     * @return A map of all inventories.
     */
    public Map<String, MythicInventory> getInventories() {
        return inventories;
    }

    /**
     * Get a list of all inventory names.
     * @return A list of all inventory names.
     */
    public List<String> getInventoryNames() {
        return inventories.keySet().stream().toList();
    }

    /**
     * Add an inventory to the list.
     * @param inventory The inventory to add.
     * @param inventoryId The internal name of the inventory.
     */
    public void addInventory(MythicInventory inventory, String inventoryId) {
        inventories.put(inventoryId, inventory);
    }

    public boolean isMythicMobsEnabled() {
        return getServer().getPluginManager().isPluginEnabled("MythicMobs");
    }

    public io.lumine.mythic.bukkit.MythicBukkit getMythicInst() {
        return io.lumine.mythic.bukkit.MythicBukkit.inst();
    }
}
