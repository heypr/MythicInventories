package dev.heypr.mythicinventories.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class MythicInventorySerializer {

    private final MythicInventories plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public MythicInventorySerializer(MythicInventories plugin) {
        this.plugin = plugin;
        createPlayerDataDirectory();
    }

    /**
     * Create the "playerdata" directory if it doesn't exist.
     */
    private void createPlayerDataDirectory() {
        File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists() && !playerDataDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create playerdata directory!");
        }
    }

    /**
     * Serialize the inventory to JSON.
     */
    public String serializeToJson(MythicInventory inventory) {
        List<Map<String, Object>> inventoryData = new ArrayList<>();
        for (int i = 0; i < inventory.getInventory().getSize(); i++) {
            ItemStack item = inventory.getInventory().getItem(i);
            if (item == null) continue;
            if (!inventory.getSavedItems().contains(i)) continue;
            try {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("slot", i);
                itemData.put("item", item.serialize());
                inventoryData.add(itemData);
            }
            catch (Exception e) {
                plugin.getLogger().severe("Failed to serialize item in slot " + i + ": " + e.getMessage());
            }
        }
        return gson.toJson(inventoryData);
    }


    /**
     * Deserialize an inventory from a JSON file.
     * @param file The file to deserialize the inventory from.
     * @param inventoryInternalName The name of the inventory.
     */
    public MythicInventory deserializeInventoryFromJson(File file, String inventoryInternalName) {
        try (FileReader reader = new FileReader(file)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            MythicInventory inventory = new MythicInventory(plugin, inventoryInternalName);

            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                int slot = obj.get("slot").getAsInt();
                JsonObject itemObj = obj.getAsJsonObject("item");

                Map<String, Object> itemData = gson.fromJson(itemObj, Map.class);
                ItemStack item = ItemStack.deserialize(itemData);

                inventory.setItem(slot, item);
            }

            return inventory;
        }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to load inventory from file: " + file.getName());
            return null;
        }
        catch (Exception e) {
            plugin.getLogger().severe("An error occurred while deserializing inventory: " + e.getMessage());
            return null;
        }
    }



    /**
     * Save inventory to a JSON file in the player's directory.
     * @param inventory The inventory to save.
     * @param player The player to save the inventory for.
     */
    public void saveInventory(MythicInventory inventory, Player player) {
        File playerDir = new File(plugin.getDataFolder(), "playerdata/" + player.getUniqueId());
        if (!playerDir.exists() && !playerDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create directory for player: " + player.getUniqueId());
            return;
        }

        File file = new File(playerDir, inventory.getInternalName() + ".json");
        String jsonString = serializeToJson(inventory);
        saveToJsonFile(jsonString, file);
    }

    /**
     * Save inventory to a JSON file in the inventories directory for when a player is not specified.
     * Remember that the internal name of the inventory is used as the file name.
     * @param inventory The inventory to save.
     */
    public void saveInventory(MythicInventory inventory) {
        File file = new File(plugin.getDataFolder(), "inventorydata/" + inventory.getInternalName() + ".json");
        String jsonString = serializeToJson(inventory);
        saveToJsonFile(jsonString, file);
    }

    /**
     * Load an inventory from a player's directory.
     * @param inventory The inventory to load.
     * @param player The player to load the inventory for.
     */
    public MythicInventory loadInventory(MythicInventory inventory, Player player) {
        File file = new File(plugin.getDataFolder(), "playerdata/" + player.getUniqueId() + "/" + inventory.getInternalName() + ".json");
        if (!file.exists()) {
            plugin.getLogger().warning("Inventory file not found: " + file.getName());
            return null;
        }
        return deserializeInventoryFromJson(file, inventory.getInternalName());
    }

    /**
     * Save serialized inventory to a file.
     * @param jsonString The serialized inventory.
     * @param file The file to save the inventory to.
     */
    private void saveToJsonFile(String jsonString, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonString);
        }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to save inventory to file: " + file.getName());
        }
    }

    /**
     * Get a list of saved inventory names from a player.
     * @param player The player to get the inventory names from.
     */
    public List<String> getPlayerInventoryNames(Player player) {
        File playerDir = new File(plugin.getDataFolder(), "playerdata/" + player.getUniqueId());
        if (!playerDir.exists()) {
            return Collections.emptyList();
        }

        File[] files = playerDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<String> inventoryNames = new ArrayList<>();
        for (File file : files) {
            inventoryNames.add(file.getName().replace(".json", ""));
        }
        return inventoryNames;
    }
}
