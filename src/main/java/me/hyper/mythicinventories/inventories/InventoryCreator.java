package me.hyper.mythicinventories.inventories;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.hyper.mythicinventories.MythicInventories;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                    LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
                    String displayName = inventorySection.getString("name", inventoryId);
                    int size = inventorySection.getInt("size", 9);
                    MythicInventory inventory = new MythicInventory(plugin, serializer.deserialize(displayName), size);
                    List<Map<?, ?>> items = inventorySection.getMapList("items");

                    boolean fillItemExists = false;

                    for (Map<?, ?> itemData : items) {
                        int slot = 0;
                        boolean fillItem = (boolean) itemData.get("fill_item");
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
                        if (slot == 0 && !itemData.containsKey("fill_item")) {
                            plugin.getLogger().severe("No slot number found for an item in inventory \"" + inventoryId + "\"!");
                            continue;
                        }
                        if (slot > size) {
                            plugin.getLogger().severe("Slot number for an item in inventory \"" + inventoryId + "\" is greater than the inventory size!");
                            continue;
                        }
                        if (itemData.containsKey("fill_item") && slot != 0) {
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
                            if (itemData.get("amount") instanceof Integer && (Integer) itemData.get("amount") > 0) {
                                item.setAmount((Integer) itemData.get("amount"));
                            }
                        }

                        if (itemData.containsKey("name")) {
                            meta.displayName(serializer.deserialize((String) itemData.get("name")));
                        }

                        if (itemData.containsKey("lore")) {
                            List<?> loreList = itemData.get("lore") instanceof List ? (List<?>) itemData.get("lore") : null;
                            if (loreList == null) {
                                plugin.getLogger().severe("Invalid lore format in inventory \"" + inventoryId + "\"!");
                                continue;
                            }
                            List<String> lore = loreList.stream()
                                    .filter(line -> line instanceof String)
                                    .map(line -> serializer.deserialize(line.toString()).content())
                                    .toList();
                            meta.lore(lore.stream().map(Component::text).collect(Collectors.toList()));
                        }

                        if (itemData.get("mm_skill") != null) {
                            if (MythicBukkit.inst().getSkillManager().getSkill((String) itemData.get("mm_skill")).isEmpty()) {
                                plugin.getLogger().severe("Invalid skill name \"" + itemData.get("mm_skill") + "\" in inventory \"" + inventoryId + "\"!");
                                continue;
                            }
                            NamespacedKey key = new NamespacedKey(plugin, "skill");
                            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, (String) itemData.get("mm_skill"));
                        }

                        item.setItemMeta(meta);

                        if (itemData.containsKey("fill_item")) {
                            if (fillItemExists) {
                                plugin.getLogger().severe("More that one item in inventory \"" + inventoryId + "\" has been defined as being a fill item! Please only define one.");
                                continue;
                            }
                            fillItemExists = true;
                            for (int i = 0; i < size; i++) {
                                if (inventory.getInventory().getItem(i) != null) {
                                    continue;
                                }
                                inventory.setItem(i, item);
                            }
                        }
                        else {
                            inventory.setItem(slot, item);
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
