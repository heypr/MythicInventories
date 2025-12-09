package dev.heypr.mythicinventories.inventory;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.util.ComponentSerializer;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class ItemLoader {

    private final MythicInventories plugin;
    private final String inventoryId;
    private final Map<?, ?> itemData;
    private final int inventorySize;

    public ItemLoader(MythicInventories plugin, String inventoryId, Map<?, ?> itemData, int inventorySize) {
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

    private Integer getInteger(String key, int defaultValue) {
        return getObject(key, Integer.class, defaultValue);
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
        Integer slot = getInteger("slot", -1);
        return slot != null && slot >= 0 ? slot : -1;
    }

    public ItemStack createItemStack() {
        if (!checkValue("type")) {
            plugin.getLogger().severe("No item type found for item in inventory \"" + inventoryId + "\"!");
            return null;
        }

        String type = itemData.get("type").toString();

        if (type.startsWith("mythic:")) {
            if (!plugin.getMythicManager().isMythicMobsEnabled()) {
                plugin.getLogger().severe("MythicMobs is not enabled! Cannot set item type to: " + type);
                return null;
            }
            String mythicId = type.replace("mythic:", "");
            Optional<MythicItem> item = plugin.getMythicManager().getMythicInst().getItemManager().getItem(mythicId);

            if (item.isPresent()) {
                MythicConfig mmConfig = item.get().getConfig().getNestedConfig("Trinket");

                if (mmConfig == null) {
                    return BukkitAdapter.adapt(item.get().generateItemStack(1));
                }

                String skill = mmConfig.getString("skill", "NO_SKILL");
                String interval = mmConfig.getString("interval", "100");
                boolean hasAttributes = mmConfig.isConfigurationSection("attributes");

                if (skill.equalsIgnoreCase("NO_SKILL") && !hasAttributes) {
                    plugin.getLogger().warning("Mythic Item '" + mythicId + "' has a Trinket section but no skill or attributes defined.");
                    return BukkitAdapter.adapt(item.get().generateItemStack(1));
                }

                if (!skill.equalsIgnoreCase("NO_SKILL")) {
                    try {
                        if (Integer.parseInt(interval) <= 0) {
                            plugin.getLogger().severe("Invalid trinket interval (" + interval + ") for item '" + mythicId + "'.");
                            return null;
                        }
                    }
                    catch (NumberFormatException e) {
                        plugin.getLogger().severe("Non-numeric trinket interval (" + interval + ") for item '" + mythicId + "'.");
                        return null;
                    }
                }

                ItemStack finalItem = BukkitAdapter.adapt(item.get().generateItemStack(1));

                ItemMeta meta = finalItem.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(plugin.getMythicIdKey(), PersistentDataType.STRING, mythicId);
                    finalItem.setItemMeta(meta);
                }

                return finalItem;
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
        int amount = getInteger("amount", 1);
        if (amount <= 0) amount = 1;
        item.setAmount(amount);
    }

    public void setDisplayName(ItemMeta meta) {
        if (checkValue("name")) {
            String name = itemData.get("name").toString();
            meta.displayName(ComponentSerializer.applyDefaultFormatting(name));
        }
    }

    public void setUnbreakable(ItemMeta meta) {
        if (checkValue("unbreakable")) {
            boolean unbreakable = getBoolean("unbreakable");
            meta.setUnbreakable(unbreakable);
        }
    }

    // TODO: Data component options
    public void setMaxStackSize(ItemStack item) {
        // DataComponentTypes.MAX_STACK_SIZE
    }

    public void setMaxDamage(ItemStack item) {
        // DataComponentTypes.MAX_DAMAGE
    }

    public void setDamage(ItemStack item) {
        // DataComponentTypes.DAMAGE
    }

    public void setUnbreakable(ItemStack item) {
        // DataComponentTypes.UNBREAKABLE
    }

    public void setCustomName(ItemStack item) {
        // DataComponentTypes.CUSTOM_NAME
    }

    public void setItemName(ItemStack item) {
        // DataComponentTypes.ITEM_NAME
    }

    public void setItemModel(ItemStack item) {
        // DataComponentTypes.ITEM_MODEL
    }

    public void setRarity(ItemStack item) {
        // DataComponentTypes.RARITY
    }

    public void setRepairCost(ItemStack item) {
        // DataComponentTypes.REPAIR_COST
    }

    public void setEnchantmentGlintOverride(ItemStack item) {
        // DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE
    }

    public void setTooltipStyle(ItemStack item) {
        // DataComponentTypes.TOOLTIP_STYLE
    }

    public void setStoredEnchantments(ItemStack item) {
        // DataComponentTypes.STORED_ENCHANTMENTS
    }

    public void setDyedColor(ItemStack item) {
        // DataComponentTypes.DYED_COLOR
    }

    public void setMapColor(ItemStack item) {
        // DataComponentTypes.MAP_COLOR
    }

    public void setMapId(ItemStack item) {
        // DataComponentTypes.MAP_ID
    }

    public void setMapPostProcessing(ItemStack item) {
        // DataComponentTypes.MAP_POST_PROCESSING
    }

    public void setOminousBottleAmplifier(ItemStack item) {
        // DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER
    }

    public void setNoteBlockSound(ItemStack item) {
        // DataComponentTypes.NOTE_BLOCK_SOUND
    }

    public void setBaseColor(ItemStack item) {
        // DataComponentTypes.BASE_COLOR
    }

    public void setInstrument(ItemStack item) {
        // DataComponentTypes.INSTRUMENT
    }

    public void setRecipes(ItemStack item) {
        // DataComponentTypes.RECIPES
    }

    public void setHideAdditionalTooltip(ItemStack item) {
        // DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP
    }

    public void setHideTooltip(ItemStack item) {
        // DataComponentTypes.HIDE_TOOLTIP
    }

    public void setIntangibleProjectile(ItemStack item) {
        // DataComponentTypes.INTANGIBLE_PROJECTILE
    }

    public void setGlider(ItemStack item) {
        // DataComponentTypes.GLIDER
    }

    public void setLore(ItemStack item) {
        // DataComponentTypes.LORE
    }

    public void setEnchantments(ItemStack item) {
        // DataComponentTypes.ENCHANTMENTS
    }

    public void setCanPlaceOn(ItemStack item) {
        // DataComponentTypes.CAN_PLACE_ON
    }

    public void setCanBreak(ItemStack item) {
        // DataComponentTypes.CAN_BREAK
    }

    public void setAttributeModifiers(ItemStack item) {
        // DataComponentTypes.ATTRIBUTE_MODIFIERS
    }

    public void setCustomModelData(ItemStack item) {
        // DataComponentTypes.CUSTOM_MODEL_DATA
    }

    public void setFood(ItemStack item) {
        // DataComponentTypes.FOOD
    }

    public void setConsumable(ItemStack item) {
        // DataComponentTypes.CONSUMABLE
    }

    public void setUseRemainder(ItemStack item) {
        // DataComponentTypes.USE_REMAINDER
    }

    public void setUseCooldown(ItemStack item) {
        // DataComponentTypes.USE_COOLDOWN
    }

    public void setDamageResistant(ItemStack item) {
        // DataComponentTypes.DAMAGE_RESISTANT
    }

    public void setTool(ItemStack item) {
        // DataComponentTypes.TOOL
    }

    public void setEnchantable(ItemStack item) {
        // DataComponentTypes.ENCHANTABLE
    }

    public void setEquippable(ItemStack item) {
        // DataComponentTypes.EQUIPPABLE
    }

    public void setRepairable(ItemStack item) {
        // DataComponentTypes.REPAIRABLE
    }

    public void setDeathProtection(ItemStack item) {
        // DataComponentTypes.DEATH_PROTECTION
    }

    public void setMapDecorations(ItemStack item) {
        // DataComponentTypes.MAP_DECORATIONS
    }

    public void setChargedProjectiles(ItemStack item) {
        // DataComponentTypes.CHARGED_PROJECTILES
    }

    public void setBundleContents(ItemStack item) {
        // DataComponentTypes.BUNDLE_CONTENTS
    }

    public void setPotionContents(ItemStack item) {
        // DataComponentTypes.POTION_CONTENTS
    }

    public void setSuspiciousStewEffects(ItemStack item) {
        // DataComponentTypes.SUSPICIOUS_STEW_EFFECTS
    }

    public void setWritableBookContent(ItemStack item) {
        // DataComponentTypes.WRITABLE_BOOK_CONTENT
    }

    public void setWrittenBookContent(ItemStack item) {
        // DataComponentTypes.WRITTEN_BOOK_CONTENT
    }

    public void setTrim(ItemStack item) {
        // DataComponentTypes.TRIM
    }

    public void setJukeboxPlayable(ItemStack item) {
        // DataComponentTypes.JUKEBOX_PLAYABLE
    }

    public void setLodestoneTracker(ItemStack item) {
        // DataComponentTypes.LODESTONE_TRACKER
    }

    public void setFireworkExplosion(ItemStack item) {
        // DataComponentTypes.FIREWORK_EXPLOSION
    }

    public void setFireworks(ItemStack item) {
        // DataComponentTypes.FIREWORKS
    }

    public void setProfile(ItemStack item) {
        // DataComponentTypes.PROFILE
    }

    public void setBannerPatterns(ItemStack item) {
        // DataComponentTypes.BANNER_PATTERNS
    }

    public void setPotDecorations(ItemStack item) {
        // DataComponentTypes.POT_DECORATIONS
    }

    public void setContainer(ItemStack item) {
        // DataComponentTypes.CONTAINER
    }

    public void setBlockData(ItemStack item) {
        // DataComponentTypes.BLOCK_DATA
    }

    public void setContainerLoot(ItemStack item) {
        // DataComponentTypes.CONTAINER_LOOT
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
        if (!plugin.getMythicManager().isMythicMobsEnabled()) {
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
                                plugin.getCacheManager().cacheItemSkill(key, skillName);
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
            if (getBoolean("save")) {
                inventory.addSavedItem(slot);
            }
        }
    }

    public boolean handleTrinketSlot(MythicInventory inventory, int slot) {
        if (!checkValue("trinket") || !getBoolean("trinket")) {
            return true;
        }
        inventory.addTrinketSlot(slot);
        return true;
    }
}
