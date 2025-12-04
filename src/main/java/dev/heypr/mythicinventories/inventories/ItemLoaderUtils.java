package dev.heypr.mythicinventories.inventories;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory.TrinketConfig;
import dev.heypr.mythicinventories.util.ComponentSerializer;
import dev.heypr.mythicinventories.util.MIClickType;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ItemLoaderUtils {

    private final MythicInventories plugin;
    private final String inventoryId;
    private final Map<?, ?> itemData;
    private final int inventorySize;

    public ItemLoaderUtils(MythicInventories plugin, String inventoryId, Map<?, ?> itemData, int inventorySize) {
        this.plugin = plugin;
        this.inventoryId = inventoryId;
        this.itemData = itemData;
        this.inventorySize = inventorySize;
    }

    public boolean checkValue(String key) {
        return itemData.containsKey(key);
    }

    private <T> T getObject(String key, Class<T> expectedType, T defaultValue) {
        Object value = itemData.get(key);
        if (expectedType.isInstance(value)) {
            return expectedType.cast(value);
        }
        else if (value != null) {
            plugin.getLogger().severe("Invalid value for key '" + key + "' in inventory '" + inventoryId + "'. Expected " + expectedType.getSimpleName() + ", got " + value.getClass().getSimpleName() + ".");
        }
        return defaultValue;
    }

    private Boolean getBoolean(String key) {
        return getObject(key, Boolean.class, false);
    }

    public boolean validateSlotAndFillItem() {
        boolean hasSlot = checkValue("slot");
        boolean isFillItem = checkValue("fill_item") && getBoolean("fill_item");

        if (!hasSlot && !isFillItem) {
            plugin.getLogger().severe("Item in \"" + inventoryId + "\" must define a 'slot' or 'fill_item'.");
            return false;
        }
        if (hasSlot && isFillItem) {
            plugin.getLogger().severe("Both 'slot' and 'fill_item' found in inventory \"" + inventoryId + "\". Define only one.");
            return false;
        }

        if (hasSlot && getSlot() >= inventorySize) {
            plugin.getLogger().severe("Slot number (" + getSlot() + ") for an item in inventory \"" + inventoryId + "\" is greater than the inventory size (" + inventorySize + ")!");
            return false;
        }

        return true;
    }

    public boolean isFillItem() {
        return getBoolean("fill_item");
    }

    public int getSlot() {
        Integer slot = getObject("slot", Integer.class, -1);
        return slot != null && slot >= 0 ? slot : -1;
    }

    public ItemStack createItemStack() {
        if (!checkValue("type")) {
            plugin.getLogger().severe("No item type found for item in inventory \"" + inventoryId + "\"!");
            return null;
        }

        String type = itemData.get("type").toString();

        if (type.startsWith("mythic:")) {
            if (!plugin.isMythicMobsEnabled()) {
                plugin.getLogger().severe("MythicMobs is not enabled! Cannot set item type to: " + type);
                return null;
            }
            String mythicId = type.replace("mythic:", "");
            Optional<MythicItem> item;
            item = plugin.getMythicInst().getItemManager().getItem(mythicId);
            if (item.isPresent()) {
                return BukkitAdapter.adapt(item.get().generateItemStack(1));
            }
            else {
                plugin.getLogger().severe("Invalid MythicMobs item \"" + mythicId + "\" in inventory \"" + inventoryId + "\"!");
                return null;
            }
        }

        try {
            return new ItemStack(Material.valueOf(type.toUpperCase()));
        }
        catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Invalid item type \"" + type + "\" in inventory \"" + inventoryId + "\"!");
            return null;
        }
    }

    public void setAmount(ItemStack item) {
        int amount = getObject("amount", Integer.class, 1);
        if (amount <= 0) amount = 1;
        item.setAmount(amount);
    }

    public void setDisplayName(ItemMeta meta) {
        if (checkValue("name")) {
            String name = itemData.get("name").toString();
            meta.displayName(ComponentSerializer.applyDefaultFormatting(name));
        }
    }

    public void setLore(ItemMeta meta) {
        if (checkValue("lore")) {
            List<?> loreList = itemData.get("lore") instanceof List ? (List<?>) itemData.get("lore") : null;

            if (loreList == null) {
                plugin.getLogger().severe("Invalid lore format in inventory \"" + inventoryId + "\"!");
                return;
            }

            List<net.kyori.adventure.text.Component> lore = loreList.stream()
                    .filter(line -> line instanceof String)
                    .map(line -> ComponentSerializer.applyDefaultFormatting(line.toString()))
                    .toList();

            meta.lore(lore);
        }
    }

    public void addItemFlags(ItemMeta meta, ItemStack item) {
        if (checkValue("item_flags")) {
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
    }

    public void handleClickTypes(MythicInventory inventory, int slot) {
        if (!plugin.isMythicMobsEnabled()) {
            return;
        }

        for (MIClickType clickType : MIClickType.values()) {
            String clickTypeKey = clickType.name().toLowerCase();
            if (checkValue(clickTypeKey)) {
                Object clickTypeValue = itemData.get(clickTypeKey);

                if (clickTypeValue instanceof List<?> clickTypeValueList) {
                    clickTypeValueList.stream()
                            .filter(value -> value instanceof String)
                            .map(Object::toString)
                            .forEach(skillName -> {
                                inventory.addClickSkill(slot, clickType, skillName);
                                UUID key = UUID.nameUUIDFromBytes(skillName.getBytes(StandardCharsets.UTF_8));
                                plugin.addToItemSkillCache(key, skillName);
                            });
                }
                else {
                    plugin.getLogger().severe("Invalid click_type list for key \"" + clickTypeKey + "\" in inventory \"" + inventoryId + "\"!");
                }
            }
        }
    }

    public void handleInteractable(int slot, ItemStack item, MythicInventory inventory) {
        if (checkValue("interactable")) {
            if (getBoolean("interactable")) {
                inventory.addInteractableItem(slot, item);
            }
        }
    }

    public void handleShouldSave(int slot, MythicInventory inventory) {
        if (checkValue("save")) {
            if (plugin.isPaperServer()) {
                if (getBoolean("save")) {
                    inventory.addSavedItem(slot);
                }
            }
            else {
                plugin.getLogger().severe("Due to the way that items are internally saved, the 'save' option is only available on Paper servers!");
            }
        }
    }

    public boolean handleTrinketSlot(MythicInventory inventory, int slot) {
        if (!checkValue("trinket_slot") || !getBoolean("trinket_slot")) {
            return true;
        }
        Boolean disappears = getBoolean("trinket_initial_item_disappears");

        TrinketConfig config = new TrinketConfig("DUMMY_SKILL", "100", "-1", disappears);
        inventory.addTrinketSlot(slot, config);
        return true;
    }
}
