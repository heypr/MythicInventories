package me.hyper.mythicinventories.inventories;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.hyper.mythicinventories.MythicInventories;
import me.hyper.mythicinventories.misc.ClickTypes;
import me.hyper.mythicinventories.misc.Utility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InventoryCreator {

    private final MythicInventories plugin;

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

                    MythicInventory inventory = new MythicInventory(plugin, size, Utility.deserializeText(displayName));
                    List<Map<?, ?>> items = inventorySection.getMapList("items");

                    boolean fillItemExists = false;

                    for (Map<?, ?> itemData : items) {

                        try {
                            int slot = 0;
                            boolean fillItem = false;
                            if (itemData.containsKey("slot")) {
                                Object slotObj = itemData.get("slot");
                                if (slotObj instanceof Integer) {
                                    slot = (Integer) slotObj;
                                }
                                else {
                                    plugin.getLogger().severe("Invalid slot value \"" + slotObj + "\" in inventory \"" + inventoryId + "\"!");
                                    continue;
                                }
                            }

                            if (itemData.containsKey("fill_item")) {
                                Object fillItemObj = itemData.get("fill_item");
                                if (fillItemObj instanceof Boolean) {
                                    fillItem = (Boolean) fillItemObj;
                                }
                                else {
                                    plugin.getLogger().severe("Invalid fill_item value \"" + fillItemObj + "\" in inventory \"" + inventoryId + "\"!");
                                    continue;
                                }
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
                            if (!itemData.containsKey("type")) {
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

                            if (itemData.containsKey("amount")) {
                                Object amountObj = itemData.get("amount");
                                if (amountObj instanceof Integer && (Integer) amountObj > 0) {
                                    item.setAmount((Integer) amountObj);
                                }
                                else {
                                    plugin.getLogger().severe("Invalid amount value \"" + amountObj + "\" in inventory \"" + inventoryId + "\"!");
                                    continue;
                                }
                            }

                            if (itemData.containsKey("name")) {
                                meta.displayName(Utility.deserializeText(itemData.get("name").toString()).asComponent().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                            }

                            if (itemData.containsKey("lore")) {
                                List<?> loreList = itemData.get("lore") instanceof List ? (List<?>) itemData.get("lore") : null;
                                if (loreList == null) {
                                    plugin.getLogger().severe("Invalid lore format in inventory \"" + inventoryId + "\"!");
                                    continue;
                                }
                                List<Component> lore = loreList.stream()
                                        .filter(line -> line instanceof String)
                                        .map(line -> Utility.deserializeText(line.toString()).asComponent().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                                        .toList();
                                meta.lore(new ArrayList<>(lore));
                            }

                            if (itemData.containsKey("mm_skill")) {
                                String skillName = itemData.get("mm_skill").toString();
                                if (MythicBukkit.inst().getSkillManager().getSkill(skillName).isEmpty()) {
                                    plugin.getLogger().severe("Invalid skill name \"" + skillName + "\" in inventory \"" + inventoryId + "\"!");
                                    continue;
                                }
                                NamespacedKey key = new NamespacedKey(plugin, "skill");
                                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skillName);
                            }

                            if (itemData.containsKey("click_type")) {
                                String clickType = itemData.get("click_type").toString().toUpperCase();
                                if (Arrays.stream(ClickTypes.values()).noneMatch(type -> type.name().equals(clickType))) {
                                    plugin.getLogger().severe("Invalid click type \"" + clickType + "\" in inventory \"" + inventoryId + "\"!");
                                    continue;
                                }
                                NamespacedKey key = new NamespacedKey(plugin, "click_type");
                                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, clickType);
                            }

                            if (itemData.containsKey("item_flags")) {
                                List<?> itemFlagList = itemData.get("item_flags") instanceof List ? (List<?>) itemData.get("item_flags") : null;
                                if (itemFlagList == null) {
                                    plugin.getLogger().severe("Invalid item_flag format/options in inventory \"" + inventoryId + "\" with item type " + material + "!");
                                    continue;
                                }
                                for (Object flag : itemFlagList) {
                                    try {
                                        meta.addItemFlags(ItemFlag.valueOf(flag.toString().toUpperCase()));
                                    }
                                    catch (IllegalArgumentException e) {
                                        plugin.getLogger().severe("Invalid item flag \"" + flag + "\" in inventory \"" + inventoryId + "\"" + "!");
                                    }
                                }
                            }

                            if (itemData.containsKey("commands")) {
                                List<?> commandsList = itemData.get("commands") instanceof List ? (List<?>) itemData.get("commands") : null;
                                if (commandsList == null) {
                                    plugin.getLogger().severe("Invalid commands format/options in inventory \"" + inventoryId + "\" with item type " + material + "!");
                                    continue;
                                }
                                for (Object command : commandsList) {
                                    if (command instanceof String) {
                                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "commands"), PersistentDataType.STRING, command.toString());
                                    }
                                    else {
                                        plugin.getLogger().severe("Invalid command \"" + command + "\" in inventory \"" + inventoryId + "\"" + "!");
                                    }
                                }
                            }

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
}
