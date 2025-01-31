package dev.heypr.mythicinventories.inventories;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.misc.MIClickType;
import io.lumine.mythic.bukkit.BukkitAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InventoryCreator {

    private final MythicInventories plugin;
    private String inventoryId;
    private Map<?, ?> itemData;
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
                        plugin.getLogger().info("Loaded " + inventoryCount + " inventories!");
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

        if (size % 9 != 0 && size > 54) {
            plugin.getLogger().severe("Invalid inventory size \"" + size + "\" in inventory \"" + inventoryId + "\"! Must be a multiple of 9 or less than 54.");
            return false;
        }

        MythicInventory inventory = new MythicInventory(plugin, size, deserializeText(displayName));
        inventory.setInternalName(inventoryId);
        List<Map<?, ?>> items = inventorySection.getMapList("items");
        for (Map<?, ?> itemData : items) {
            if (!loadItem(itemData, inventory, size, inventoryId)) {
                plugin.getLogger().severe("Failed to load item in inventory \"" + inventoryId + "\"!");
            }
        }
        plugin.addInventory(inventory, inventoryId);
        inventoryCount++;
        return true;
    }

    private boolean loadItem(Map<?, ?> itemData, MythicInventory inventory, int size, String inventoryId) {

        this.inventoryId = inventoryId;
        this.itemData = itemData;
        boolean isFillItem = false;

        try {
            int slot;

            if (checkValue("slot")) {
                int fl = getSlot(itemData);
                if (fl == 0) {
                    return false;
                }
                slot = getSlot(itemData);
            }
            else {
                slot = 0;
            }

            if (checkValue("fill_item")) {
                isFillItem = isFillItem(itemData);
            }

            if (slot == 0 && !isFillItem) {
                plugin.getLogger().severe("No slot number found for an item in inventory \"" + inventoryId + "\"!");
                return false;
            }
            if (slot >= size && !isFillItem) {
                plugin.getLogger().severe("Slot number for an item in inventory \"" + inventoryId + "\" is greater than the inventory size!");
                return false;
            }
            if (isFillItem && slot != 0) {
                plugin.getLogger().severe("Both \"slot\" and \"fill_item\" options found for item in inventory \"" + inventoryId + "\"! Please only define one.");
                return false;
            }
            if (!checkValue("type")) {
                plugin.getLogger().severe("No item type found for item in inventory \"" + inventoryId + "\"!");
                return false;
            }
            if (!checkValue("type")) {
                plugin.getLogger().severe("No item type found for item in inventory \"" + inventoryId + "\"!");
                return false;
            }

            ItemStack item;
            String type = itemData.get("type").toString();

            if (type.startsWith("mythic:")) {
                if (plugin.isMythicMobsEnabled()) {
                    type = type.replace("mythic:", "");
                    if (plugin.getMythicInst().getItemManager().getItem(type).isEmpty()) {
                        plugin.getLogger().severe("Invalid MythicMobs item \"" + type + "\" in inventory \"" + inventoryId + "\"!");
                        return false;
                    }
                    item = BukkitAdapter.adapt(plugin.getMythicInst().getItemManager().getItem(type).get().generateItemStack(1));
                }
                else {
                    plugin.getLogger().severe("MythicMobs is not enabled! Cannot set item type to: " + type);
                    return false;
                }
            }
            else {
                try {
                    item = new ItemStack(Material.valueOf(itemData.get("type").toString().toUpperCase()));
                }
                catch (IllegalArgumentException e) {
                    plugin.getLogger().severe("Invalid item type \"" + itemData.get("type") + "\" in inventory \"" + inventoryId + "\"!");
                    return false;
                }
            }

            ItemMeta meta = item.getItemMeta();

            if (checkValue("amount")) {
                int amount = getAmount(itemData);
                if (amount == 0) {
                    plugin.getLogger().severe("Setting amount to 1.");
                    amount = 1;
                }
                item.setAmount(amount);
            }
            else {
                item.setAmount(1);
            }

            if (checkValue("name")) {
                meta.displayName(deserializeText(itemData.get("name").toString()).asComponent().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }

            if (checkValue("lore")) {
                List<Component> lore = getLore(itemData);
                if (lore != null) {
                    meta.lore(lore);
                }
            }

            for (MIClickType clickType : MIClickType.values()) {
                if (checkValue(clickType.name().toLowerCase())) {
                    handleClickType(itemData, inventory, clickType, slot);
                }
            }

            if (checkValue("item_flags")) {
                hasItemFlags(itemData, meta, item);
            }

            if (checkValue("interactable")) {
                isInteractable(itemData, slot, item, inventory);
            }

            if (checkValue("save")) {
                if (plugin.isPaperServer()) {
                    shouldSave(itemData, slot, inventory);
                }
                else {
                    plugin.getLogger().severe("Due to the way that items are internally saved, the 'save' option is only available on Paper servers!");
                }
            }

            item.setItemMeta(meta);

            if (isFillItem) {
                if (fillItemExists) {
                    plugin.getLogger().severe("More than one item in inventory \"" + inventoryId + "\" has been defined as being a fill item! Please only define one.");
                    return false;
                }
                this.fillItemExists = true;
                for (int i = 0; i < size; i++) {
                    if (inventory.getInventory().getItem(i) != null) {
                        return false;
                    }
                    inventory.getInventory().setItem(i, item);
                }
            }
            else {
                inventory.setItem(slot, item);
            }
        }
        catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error parsing item in inventory \"" + inventoryId + "\": " + e.getMessage(), e);
        }
        return true;
    }

    /**
     * Deserialize a text component from a string.
     *
     * @param text The text to deserialize.
     * @return The deserialized text component.
     */
    private TextComponent deserializeText(String text) {
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage mm = MiniMessage.miniMessage();
        return legacy.deserialize(legacy.serialize(mm.deserialize(text).asComponent()));
    }

    /**
     * Check if the item should be a fill item.
     *
     * @param itemData The item data.
     * @return True if the item should be a fill item, false otherwise.
     */
    private boolean isFillItem(Map<?, ?> itemData) {
        Object fillItemObj = itemData.get("fill_item");
        if (fillItemObj instanceof Boolean) {
            return (boolean) fillItemObj;
        }
        else {
            plugin.getLogger().severe("Invalid fill_item value \"" + fillItemObj + "\" in inventory \"" + inventoryId + "\"!");
            return false;
        }
    }

    /**
     * Get the amount of the item.
     *
     * @param itemData The item data.
     * @return The amount of the item.
     */
    private int getAmount(Map<?, ?> itemData) {
        Object amountObj = itemData.get("amount");
        if (amountObj instanceof Integer) {
            return (int) amountObj;
        }
        else {
            plugin.getLogger().severe("Invalid amount value \"" + amountObj + "\" in inventory \"" + inventoryId + "\"!");
            return 0;
        }
    }

    /**
     * Get the lore for the item.
     *
     * @param itemData The item data.
     * @return The lore for the item.
     */
    private List<Component> getLore(Map<?, ?> itemData) {
        List<?> loreList = itemData.get("lore") instanceof List ? (List<?>) itemData.get("lore") : null;
        if (loreList == null) {
            plugin.getLogger().severe("Invalid lore format in inventory \"" + inventoryId + "\"!");
            return null;
        }
        return loreList.stream()
                .filter(line -> line instanceof String)
                .map(line -> deserializeText(line.toString()).asComponent().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList();
    }

    /**
     * Checks if the item has a click type.
     *
     * @param itemData  The item data.
     * @param inventory The inventory to add the click type to.
     * @param clickType The click type to check.
     * @param slot      The slot of the item.
     *
     */
    private void handleClickType(Map<?, ?> itemData, MythicInventory inventory, MIClickType clickType, int slot) {
        String clickTypeKey = clickType.name().toLowerCase();
        Object clickTypeValue = itemData.get(clickTypeKey);

        if (!plugin.isMythicMobsEnabled()) {
            plugin.getLogger().severe("MythicMobs is not enabled! Cannot set click type to: " + clickTypeKey);
            return;
        }

        if (!(clickTypeValue instanceof List<?> clickTypeValueList)) {
            plugin.getLogger().severe("Invalid or missing click_type list for key \"" + clickTypeKey + "\" in inventory \"" + inventoryId + "\"!");
            return;
        }

        clickTypeValueList.stream()
                .filter(value -> value instanceof String)
                .map(Object::toString)
                .forEach(skillName -> inventory.addClickSkill(slot, clickType, skillName));
    }


    /**
     * Checks if the item has item flags.
     *
     * @param itemData The item data.
     * @param meta The item meta.
     * @param item The item to check.
     */
    private void hasItemFlags(Map<?, ?> itemData, ItemMeta meta, ItemStack item) {
        List<?> itemFlagList = itemData.get("item_flags") instanceof List ? (List<?>) itemData.get("item_flags") : null;
        if (itemFlagList == null) {
            plugin.getLogger().severe("Invalid item_flag format/options in inventory \"" + inventoryId + "\" with item type " + item.getType() + "!");
            return;
        }
        for (Object flag : itemFlagList) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.toString().toUpperCase()));
            }
            catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Invalid item flag \"" + flag + "\" in inventory \"" + inventoryId + "\"" + "!");
                return;
            }
        }
    }

    /**
     * Checks if the item is interactable.
     *
     * @param itemData The item data.
     * @param slot     The slot of the item.
     * @param item     The item to check.
     * @param inventory The inventory to add the interactable item to.
     */
    private void isInteractable(Map<?, ?> itemData, int slot, ItemStack item, MythicInventory inventory) {
        Boolean interactable = (Boolean) itemData.get("interactable");
        if (interactable == null) {
            plugin.getLogger().severe("Invalid interactable value found in inventory \"" + inventoryId + "\" with item type " + item.getType() + "!");
            return;
        }
        if (!interactable) return;
        inventory.addInteractableItem(slot, item);
    }

    /**
     * Checks if the item should be saved.
     *
     * @param itemData The item data.
     * @param inventory The inventory to save the item to.
     */
    private void shouldSave(Map<?, ?> itemData, int slot, MythicInventory inventory) {
        Boolean save = (Boolean) itemData.get("save");
        if (save == null) {
            plugin.getLogger().severe("Invalid save value found in inventory \"" + inventoryId + "!");
            return;
        }
        if (!save) return;
        inventory.addSavedItem(slot);
    }

    /**
     * Get the slot number for the item.
     *
     * @param itemData The item data.
     * @return The slot number for the item, 0 if invalid or not found.
     */
    private int getSlot(Map<?, ?> itemData) {
        Object slotObj = itemData.get("slot");
        if (slotObj instanceof Integer) {
            return (Integer) slotObj;
        }
        else {
            plugin.getLogger().severe("Invalid slot value \"" + slotObj + "\" in inventory \"" + inventoryId + "\"!");
            return 0;
        }
    }

    /**
     * Check if the item has the given value, basic thing to reduce the weird "containsKey" stuff.
     *
     * @param key The key to check (duh).
     * @return True if the item has the value, false otherwise.
     */
    private boolean checkValue(String key) {
        return itemData.containsKey(key);
    }
}
