package dev.heypr.mythicinventories.inventories;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.misc.ClickTypes;
import io.lumine.mythic.bukkit.MythicBukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InventoryCreator {

    private final MythicInventories plugin;
    private String inventoryId;
    private Map<?, ?> itemData;

    public InventoryCreator(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public void loadInventories() {
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

                    if (!(config.get(inventoryId) instanceof ConfigurationSection)) {
                        plugin.getLogger().severe("Skipping invalid inventory entry: " + inventoryId + " in " + file.getName());
                        continue;
                    }

                    ConfigurationSection inventorySection = config.getConfigurationSection(inventoryId);

                    if (inventorySection == null) {
                        plugin.getLogger().severe("No inventory section found for ID " + inventoryId + " in " + file.getName() + "!");
                        continue;
                    }

                    String displayName = inventorySection.getString("name", inventoryId);
                    int size = inventorySection.getInt("size", 9);

                    if (size % 9 != 0) {
                        plugin.getLogger().severe("Invalid inventory size \"" + size + "\" in inventory \"" + inventoryId + "\"! Must be a multiple of 9.");
                        continue;
                    }

                    MythicInventory inventory = new MythicInventory(plugin, size, deserializeText(displayName));
                    inventory.setInternalName(inventoryId);
                    List<Map<?, ?>> items = inventorySection.getMapList("items");

                    boolean fillItemExists = false;

                    for (Map<?, ?> itemData : items) {

                        this.inventoryId = inventoryId;
                        this.itemData = itemData;

                        try {
                            int slot = 0;
                            boolean fillItem = false;

                            if (checkValue("slot")) {
                                int fl = getSlot(itemData);
                                if (fl == 0) {
                                    continue;
                                }
                                slot = getSlot(itemData);
                            }

                            if (checkValue("fill_item")) {
                                if (!isFillItem(itemData)) {
                                    continue;
                                }
                                fillItem = isFillItem(itemData);
                            }

                            if (slot == 0 && !fillItem) {
                                plugin.getLogger().severe("No slot number found for an item in inventory \"" + inventoryId + "\"!");
                                continue;
                            }
                            if (slot >= size && !fillItem) {
                                plugin.getLogger().severe("Slot number for an item in inventory \"" + inventoryId + "\" is greater than the inventory size!");
                                continue;
                            }
                            if (fillItem && slot != 0) {
                                plugin.getLogger().severe("Both \"slot\" and \"fill_item\" options found for item in inventory \"" + inventoryId + "\"! Please only define one.");
                                continue;
                            }
                            if (!checkValue("type")) {
                                plugin.getLogger().severe("No item type found for item in inventory \"" + inventoryId + "\"!");
                                continue;
                            }

                            Material material;

                            try {
                                material = Material.valueOf(itemData.get("type").toString().toUpperCase());
                            }
                            catch (IllegalArgumentException e) {
                                plugin.getLogger().severe("Invalid material type \"" + itemData.get("type") + "\" in inventory \"" + inventoryId + "\"!");
                                continue;
                            }

                            ItemStack item = new ItemStack(material);
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

                            if (checkValue("mm_skill")) {
                                hasMMSkill(itemData, meta);
                            }

                            if (checkValue("click_type")) {
                                hasClickType(itemData, meta);
                            }

                            if (checkValue("item_flags")) {
                                hasItemFlags(itemData, meta, item);
                            }

                            if (checkValue("interactable")) {
                                isInteractable(itemData, slot, item, inventory);
                            }

                            if (checkValue("save")) {
                                shouldSave(itemData, slot, inventory);
                            }

                            // TODO: Implement commands
                            //if (checkValue("commands")) {
                            //    List<?> commandsList = itemData.get("commands") instanceof List ? (List<?>) itemData.get("commands") : null;
                            //    if (commandsList == null) {
                            //        plugin.getLogger().severe("Invalid commands format/options in inventory \"" + inventoryId + "\" with item type " + material + "!");
                            //        continue;
                            //    }
                            //    // WHAT THE FUCK IS This
                            //    for (Object command : commandsList) {
                            //        if (command instanceof String) {
                            //            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "commands"), PersistentDataType.STRING, command.toString());
                            //        }
                            //        else {
                            //            plugin.getLogger().severe("Invalid command \"" + command + "\" in inventory \"" + inventoryId + "\"" + "!");
                            //        }
                            //    }
                            //}

                            item.setItemMeta(meta);

                            if (fillItem) {
                                if (fillItemExists) {
                                    plugin.getLogger().severe("More than one item in inventory \"" + inventoryId + "\" has been defined as being a fill item! Please only define one.");
                                    continue;
                                }
                                fillItemExists = true;
                                for (int i = 0; i < size; i++) {
                                    if (inventory.getInventory().getItem(i) != null) {
                                        continue;
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
                    }
                    plugin.addInventory(inventory, inventoryId);
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

    private TextComponent deserializeText(String text) {
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage mm = MiniMessage.miniMessage();
        return legacy.deserialize(legacy.serialize(mm.deserialize(text).asComponent()));
    }

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
     * Checks if the item has a MythicMobs skill.
     *
     * @param itemData The item data.
     * @param meta The item meta.
     */
    private void hasMMSkill(Map<?, ?> itemData, ItemMeta meta) {
        String skillName = itemData.get("mm_skill").toString();
        if (MythicBukkit.inst().getSkillManager().getSkill(skillName).isEmpty()) {
            plugin.getLogger().severe("Invalid skill name \"" + skillName + "\" in inventory \"" + inventoryId + "\"!");
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "skill");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skillName);
    }

    /**
     * Checks if the item has a click type.
     *
     * @param itemData The item data.
     * @param meta The item meta.
     * @return True if the item has a click type, false otherwise.
     */
    private boolean hasClickType(Map<?, ?> itemData, ItemMeta meta) {
        String clickType = itemData.get("click_type").toString().toUpperCase();
        if (Arrays.stream(ClickTypes.values()).noneMatch(type -> type.name().equals(clickType))) {
            plugin.getLogger().severe("Invalid click type \"" + clickType + "\" in inventory \"" + inventoryId + "\"!");
            return false;
        }
        NamespacedKey key = new NamespacedKey(plugin, "click_type");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, clickType);
        return true;
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
