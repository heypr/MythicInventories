package dev.heypr.mythicinventories.trinket;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class TrinketManager {

    private final MythicInventories plugin;

    public TrinketManager(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public Optional<ItemStack> getReplacementItem(String id) {
        if (id == null || id.equalsIgnoreCase("NONE") || id.isEmpty()) {
            return Optional.empty();
        }
        if (id.equalsIgnoreCase("AIR")) {
            return Optional.of(new ItemStack(Material.AIR));
        }
        if (id.toLowerCase().startsWith("mythic:")) {
            String mythicId = id.substring(7);
            return Optional.ofNullable(plugin.getMythicManager().getMythicInst().getItemManager().getItemStack(mythicId));
        }
        Material material = Material.matchMaterial(id.toUpperCase());
        if (material != null) {
            return Optional.of(new ItemStack(material));
        }
        return Optional.empty();
    }

    public MythicInventory.TrinketConfig getTrinketConfigFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) return null;
        ItemMeta meta = item.getItemMeta();
        String cacheIdString = meta.getPersistentDataContainer().get(plugin.getTrinketCacheKey(), PersistentDataType.STRING);

        if (cacheIdString != null) {
            try {
                UUID cacheId = UUID.fromString(cacheIdString);
                MythicInventory.TrinketConfig cachedConfig = plugin.getCacheManager().getTrinketConfig(cacheId);
                if (cachedConfig != null) return cachedConfig;
            }
            catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Corrupted Trinket Cache UUID found on item: " + cacheIdString);
            }
        }
        return this.loadTrinketConfigFromItemMeta(meta);
    }

    public MythicInventory.TrinketConfig loadTrinketConfigFromItemMeta(ItemMeta meta) {
        String mythicId = meta.getPersistentDataContainer().get(plugin.getMythicIdKey(), PersistentDataType.STRING);
        if (mythicId == null || mythicId.isEmpty()) return null;

        UUID newCacheKey = UUID.nameUUIDFromBytes(mythicId.getBytes(StandardCharsets.UTF_8));
        Optional<MythicItem> itemOptional = plugin.getMythicManager().getMythicInst().getItemManager().getItem(mythicId);
        if (itemOptional.isEmpty()) return null;

        MythicConfig mmConfig = itemOptional.get().getConfig().getNestedConfig("Trinket");
        if (mmConfig == null) return null;

        String skill = mmConfig.getString("skill", "NO_SKILL");
        String skillInterval = mmConfig.getString("skill_interval", "100");
        String skillUses = mmConfig.getString("skill_uses", "-1");
        String skillRunOut = mmConfig.getString("skill_run_out_item", null);
        String attributeInterval = mmConfig.getString("attribute_interval", "20");
        String attributeUses = mmConfig.getString("attribute_uses", "-1");
        String attrRunOut = mmConfig.getString("attribute_run_out_item", null);
        boolean disappears = mmConfig.getBoolean("initial_item_disappears", false);
        boolean hasAttributes = mmConfig.isConfigurationSection("attributes");

        if (skill.equalsIgnoreCase("NO_SKILL") && !hasAttributes) return null;

        MythicInventory.TrinketConfig finalConfig = new MythicInventory.TrinketConfig(
                skill, skillInterval, skillUses, skillRunOut,
                attributeInterval, attributeUses, attrRunOut, disappears);
        plugin.getCacheManager().cacheTrinketConfig(newCacheKey, finalConfig);
        return finalConfig;
    }
}
