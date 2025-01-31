package dev.heypr.mythicinventories;

import dev.heypr.mythicinventories.bstats.Metrics;
import dev.heypr.mythicinventories.commands.MigrateOldDataCommand;
import dev.heypr.mythicinventories.commands.OpenInventoryCommand;
import dev.heypr.mythicinventories.commands.OpenInventoryTabCompleter;
import dev.heypr.mythicinventories.events.BukkitInventoryEvents;
import dev.heypr.mythicinventories.events.MythicMobEvents;
import dev.heypr.mythicinventories.inventories.InventoryCreator;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.storage.MythicInventorySerializer;
import dev.heypr.mythicinventories.updater.OldDataConverter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class MythicInventories extends JavaPlugin implements Listener {

    // Format: internal inventory name -> MythicInventory object
    private final HashMap<String, MythicInventory> inventories = new HashMap<>();
    private List<UUID> confirmationList = new ArrayList<>();
    private boolean isPaperServer = false;

    @Override
    public void onEnable() {
        try {
            Class.forName("com.destroystokyo.paper.event.player.PlayerSetSpawnEvent");
            isPaperServer = true;
        }
        catch (ClassNotFoundException ignored) {}

        getCommand("migrateolddata").setExecutor(new MigrateOldDataCommand(this));
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
     * Get the old data converter.
     * @return The old data converter.
     */
    public OldDataConverter getOldDataConverter() {
        return new OldDataConverter(this);
    }

    /**
     * Reload all inventories.
     */
    public void reloadInventories() {
        inventories.clear();
        new InventoryCreator(this).createInventories();
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
    public HashMap<String, MythicInventory> getInventories() {
        return inventories;
    }

    /**
     * Get an inventory by its internal name.
     * @param inventoryId The internal name of the inventory.
     * @return The inventory with the given internal name.
     */
    public MythicInventory getInventory(String inventoryId) {
        return inventories.get(inventoryId);
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

    /**
     * Get the confirmation list.
     * @return The confirmation list.
     */
    public List<UUID> getConfirmationList() {
        return confirmationList;
    }

    /**
     * Add a player to the confirmation list.
     * @param player The player to add.
     */
    public void addPlayer(UUID player) {
        if (!confirmationList.contains(player)) {
            confirmationList.add(player);
        }
    }

    /**
     * Remove a player from the confirmation list.
     * @param player The player to remove.
     */
    public void removePlayer(UUID player) {
        confirmationList.remove(player);
    }

    /**
     * Check if MythicMobs is enabled.
     * @return True if MythicMobs is enabled, false otherwise.
     */
    public boolean isMythicMobsEnabled() {
        return getServer().getPluginManager().isPluginEnabled("MythicMobs");
    }

    /**
     * Get the MythicBukkit instance.
     * @return The MythicBukkit instance.
     */
    public MythicBukkit getMythicInst() {
        return MythicBukkit.inst();
    }

    /**
     * Check if the server is running Paper.
     * @return True if the server is running Paper, false otherwise.
     */
    public boolean isPaperServer() {
        return isPaperServer;
    }
}
