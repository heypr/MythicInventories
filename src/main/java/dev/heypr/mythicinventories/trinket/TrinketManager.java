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

    public MythicInventory.TrinketConfig getTrinketConfigFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) return null;

        ItemMeta meta = item.getItemMeta();

        String cacheIdString = meta.getPersistentDataContainer().get(plugin.getTrinketCacheKey(), PersistentDataType.STRING);

        if (cacheIdString != null) {
            try {
                UUID cacheId = UUID.fromString(cacheIdString);
                MythicInventory.TrinketConfig cachedConfig = plugin.getCacheManager().getTrinketConfig(cacheId);

                if (cachedConfig != null) {
                    return cachedConfig;
                }
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
        if (itemOptional.isEmpty()) {
            return null;
        }

        MythicConfig mmConfig = itemOptional.get().getConfig().getNestedConfig("Trinket");

        if (mmConfig == null) {
            return null;
        }

        String skill = mmConfig.getString("skill", "NO_SKILL");
        String interval = mmConfig.getString("interval", "100");
        String uses = mmConfig.getString("uses", "-1");
        boolean disappears = mmConfig.getBoolean("initial_item_disappears", false);
        boolean hasAttributes = mmConfig.isConfigurationSection("attributes");

        if (skill.equalsIgnoreCase("NO_SKILL") && !hasAttributes) {
            return null;
        }

        if (!skill.equalsIgnoreCase("NO_SKILL")) {
            try {
                if (Integer.parseInt(interval) <= 0) {
                    return null;
                }
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        MythicInventory.TrinketConfig finalConfig = new MythicInventory.TrinketConfig(skill, interval, uses, disappears);
        plugin.getCacheManager().cacheTrinketConfig(newCacheKey, finalConfig);
        return finalConfig;
    }
}
