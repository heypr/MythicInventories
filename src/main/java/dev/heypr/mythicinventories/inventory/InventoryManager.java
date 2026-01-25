package dev.heypr.mythicinventories.inventory;

import dev.heypr.mythicinventories.MythicInventories;

import java.io.File;
import java.util.*;

public class InventoryManager {

    private final MythicInventories plugin;
    private final HashMap<String, MythicInventory> inventories = new HashMap<>();
    private final HashMap<UUID, List<MythicInventory>> activePlayerInventories = new HashMap<>();

    public InventoryManager(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public void reloadInventories() {
        plugin.getTrinketScheduler().stopAllTrinketSkillTasks();
        plugin.getCacheManager().clearAllCaches();
        inventories.clear();
        new InventoryCreator(plugin).createInventories();
    }

    public void createInventoriesDirectory() {
        File inventoriesDir = new File(plugin.getDataFolder(), "inventories");
        if (!inventoriesDir.exists()) {
            inventoriesDir.mkdirs();
        }
    }

    public void registerActiveInventory(UUID uuid, MythicInventory inventory) {
        activePlayerInventories.computeIfAbsent(uuid, k -> new ArrayList<>()).add(inventory);
    }

    public List<MythicInventory> getActiveInventories(UUID uuid) {
        return activePlayerInventories.getOrDefault(uuid, Collections.emptyList());
    }

    public void clearActiveInventories(UUID uuid) {
        activePlayerInventories.remove(uuid);
    }

    public HashMap<String, MythicInventory> getInventories() {
        return inventories;
    }

    public List<String> getInventoryNames() {
        return inventories.keySet().stream().toList();
    }

    public void addInventory(MythicInventory inventory, String inventoryId) {
        inventories.put(inventoryId, inventory);
    }
}
