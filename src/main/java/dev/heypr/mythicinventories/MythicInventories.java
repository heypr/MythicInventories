package dev.heypr.mythicinventories;

import dev.heypr.mythicinventories.bstats.Metrics;
import dev.heypr.mythicinventories.commands.OpenInventoryTabCompleter;
import dev.heypr.mythicinventories.events.InventoryEvents;
import dev.heypr.mythicinventories.inventories.InventoryCreator;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.commands.OpenInventoryCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MythicInventories extends JavaPlugin {

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

        Bukkit.getPluginManager().registerEvents(new InventoryEvents(this), this);

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

    public void reloadInventories() {
        inventories.clear();
        new InventoryCreator(this).loadInventories();
    }

    private void createInventoriesDirectory() {
        File inventoriesDir = new File(getDataFolder(), "inventories");
        if (!inventoriesDir.exists()) {
            inventoriesDir.mkdirs();
        }
    }

    public Map<String, MythicInventory> getInventories() {
        return inventories;
    }

    public List<String> getInventoryNames() {
        return inventories.keySet().stream().toList();
    }

    public void addInventory(MythicInventory inventory, String inventoryId) {
        inventories.put(inventoryId, inventory);
    }
}
