package dev.heypr.mythicinventories.inventories;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.util.ComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InventoryCreator {

    private final MythicInventories plugin;
    private int inventoryCount;
    private boolean fillItemExists = false;

    public InventoryCreator(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public void createInventories() {
        inventoryCount = 0;
        File inventoriesDir = new File(plugin.getDataFolder(), "inventories");
        File[] files = inventoriesDir.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) {
            plugin.getLogger().severe("No inventory files found in the inventories directory!");
            return;
        }

        for (File file : files) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String inventoryId : config.getKeys(false)) {
                    if (!loadInventory(file, config, inventoryId)) {
                        plugin.getLogger().severe("Failed to load inventory " + inventoryId + " from file " + file.getName() + "!");
                    }
                    else {
                        plugin.getLogger().info("Loaded " + inventoryId + " from file " + file.getName() + ".");
                        inventoryCount++;
                    }
                }
            }
            catch (MarkedYAMLException e) {
                plugin.getLogger().severe("Error in file " + file.getName() + " at line " + e.getProblemMark().getLine() + ": " + e.getMessage());
            }
            catch (Exception e) {
                plugin.getLogger().severe("Error processing file " + file.getName() + ": " + e.getMessage());
            }
        }
        if (inventoryCount > 0) {
            plugin.getLogger().info("Successfully loaded a total of " + inventoryCount + " inventories!");
        }
    }

    private boolean loadInventory(File file, FileConfiguration config, String inventoryId) {
        if (!(config.get(inventoryId) instanceof ConfigurationSection)) {
            plugin.getLogger().severe("Skipping invalid inventory entry: " + inventoryId + " in " + file.getName());
            return false;
        }

        ConfigurationSection inventorySection = config.getConfigurationSection(inventoryId);

        if (inventorySection == null) {
            plugin.getLogger().severe("No inventory section found for ID " + inventoryId + " in " + file.getName() + "!");
            return false;
        }

        String displayName = inventorySection.getString("name", "Container");
        int size = inventorySection.getInt("size", 9);

        if (size % 9 != 0 || size > 54 || size <= 0) {
            plugin.getLogger().severe("Invalid inventory size \"" + size + "\" in inventory \"" + inventoryId + "\"! Must be a multiple of 9, between 9 and 54.");
            return false;
        }

        MythicInventory inventory = new MythicInventory(plugin, size, ComponentSerializer.applyDefaultFormatting(displayName));
        inventory.setInternalName(inventoryId);
        List<Map<?, ?>> items = inventorySection.getMapList("items");

        this.fillItemExists = false;

        for (Map<?, ?> itemData : items) {
            if (!loadItem(itemData, inventory, size, inventoryId)) {
                plugin.getLogger().severe("Failed to load item in inventory \"" + inventoryId + "\"!");
            }
        }
        plugin.addInventory(inventory, inventoryId);
        return true;
    }

    private boolean loadItem(Map<?, ?> itemData, MythicInventory inventory, int size, String inventoryId) {
        try {
            ItemLoaderUtils utils = new ItemLoaderUtils(plugin, inventoryId, itemData, size);

            if (!utils.validateSlotAndFillItem()) {
                return false;
            }

            boolean isFillItem = utils.isFillItem();
            int slot = utils.getSlot();

            ItemStack item = utils.createItemStack();
            if (item == null) {
                return false;
            }

            ItemMeta meta = item.getItemMeta();

            utils.setAmount(item);
            utils.setDisplayName(meta);
            utils.setUnbreakable(meta);
            utils.setLore(meta);
            utils.addItemFlags(meta, item);

            utils.handleInteractable(slot, item, inventory);
            utils.handleShouldSave(slot, inventory);

            utils.handleClickTypes(inventory, slot);

            if (!utils.handleTrinketSlot(inventory, slot)) {
                return false;
            }

            item.setItemMeta(meta);

            if (isFillItem) {
                if (this.fillItemExists) {
                    plugin.getLogger().severe("More than one item in inventory \"" + inventoryId + "\" has been defined as being a fill item! Please only define one.");
                    return false;
                }
                this.fillItemExists = true;
                for (int i = 0; i < size; i++) {
                    if (inventory.getInventory().getItem(i) == null) {
                        inventory.getInventory().setItem(i, item);
                    }
                }
            }
            else {
                inventory.setItem(slot, item);
            }

        }
        catch (Exception e) {
            plugin.getLogger().severe("Error parsing item in inventory \"" + inventoryId + "\": " + e.getMessage());
            return false;
        }
        return true;
    }
}
