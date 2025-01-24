package dev.heypr.mythicinventories.updater;

import com.google.gson.*;
import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OldDataConverter {

    private final MythicInventories plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public OldDataConverter(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public void convertData() {
        List<File> files = plugin.getInventorySerializer().getPlayerDataFiles();

        for (File file : files) {
            plugin.getLogger().info("Converting data in folder " + file.getName());
            File[] inventoryFiles = file.listFiles();
            if (inventoryFiles == null) {
                continue;
            }
            String uuid = file.getName();
            for (File inventoryFile : inventoryFiles) {
                String inventoryInternalName = inventoryFile.getName().replace(".json", "");
                plugin.getLogger().info("Converting data for file " + inventoryInternalName);
                MythicInventory inventory = deserializeInventoryFromJsonLegacy(inventoryFile, inventoryInternalName);
                if (inventory != null) {
                    plugin.getInventorySerializer().saveInventory(inventory, UUID.fromString(uuid));
                }
            }
        }
    }

    private MythicInventory deserializeInventoryFromJsonLegacy(File file, String inventoryInternalName) {
        try (FileReader reader = new FileReader(file)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            MythicInventory inventory = plugin.getInventory(inventoryInternalName);

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
        catch (Exception e) {
            //ignore
        }
        return null;
    }
}
