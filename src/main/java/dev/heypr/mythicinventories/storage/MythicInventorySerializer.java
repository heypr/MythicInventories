package dev.heypr.mythicinventories.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    public List<File> getPlayerDataFiles() {
        File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            return Collections.emptyList();
        }

        File[] files = playerDataDir.listFiles();
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.asList(files);
    }

    /**
     * Serialize the inventory to JSON.
     */
    public String serializeToJson(MythicInventory inventory) {
        HashMap<Integer, String> inventoryData = new HashMap<>();
        for (int i = 0; i < inventory.getInventory().getSize(); i++) {
            ItemStack item = inventory.getInventory().getItem(i);
            if (item == null) continue;
            if (!inventory.getSavedItems().contains(i)) continue;
            try {
                byte[] serialized = item.serializeAsBytes();
                String encoded = Base64.encodeBase64String(serialized);
                inventoryData.put(i, encoded);
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
            MythicInventory inventory = new MythicInventory(plugin, inventoryInternalName);

            Type hashMapType = new TypeToken<HashMap<Integer, String>>() {}.getType();

            HashMap<Integer, String> map = gson.fromJson(reader, hashMapType);
            for (Integer slot : map.keySet()) {
                byte[] decoded = Base64.decodeBase64(map.get(slot));
                ItemStack deserialized = ItemStack.deserializeBytes(decoded);
                inventory.setItem(slot, deserialized);
            }
            return inventory;
        }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to load inventory from file: " + file.getName());
            return null;
        }
        catch (Exception e) {
            plugin.getLogger().severe("If you have just recently updated, please run the \"/migrateolddata\" command!");
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
        saveInventory(inventory, player.getUniqueId());
    }

    /**
     * Save inventory to a JSON file in the player's directory.
     * @param inventory The inventory to save.
     * @param uuid The player to save the inventory for.
     */
    public void saveInventory(MythicInventory inventory, UUID uuid) {
        File playerDir = new File(plugin.getDataFolder(), "playerdata/" + uuid);
        if (!playerDir.exists() && !playerDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create directory for player: " + uuid);
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
