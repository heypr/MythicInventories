package dev.heypr.mythicinventories.util;

import dev.heypr.mythicinventories.MythicInventories;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.utils.profiles.Profile;
import io.lumine.mythic.core.items.MythicItem;
import io.lumine.mythic.core.players.PlayerData;
import io.lumine.mythic.core.skills.stats.StatRegistry;
import io.lumine.mythic.core.skills.stats.StatSource;
import io.lumine.mythic.core.skills.stats.StatType;
import io.lumine.mythic.core.utils.annotations.MythicStat;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AttributeManager {

    private final MythicInventories plugin;
    private static final UUID NAMESPACE = UUID.nameUUIDFromBytes("mythicinventories".getBytes(StandardCharsets.UTF_8));
    private static final String MODIFIER_NAME_PREFIX = "MI_Trinket_Slot_";

    public AttributeManager(MythicInventories plugin) {
        this.plugin = plugin;
    }

    private UUID getModifierUUID(Player player, int slot) {
        String sourceString = player.getUniqueId() + ":" + slot;
        return UUID.nameUUIDFromBytes((NAMESPACE + sourceString).getBytes(StandardCharsets.UTF_8));
    }

    private Attribute getAttribute(String name) {
        try {
            return Attribute.valueOf(name.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid Attribute name: " + name);
            return null;
        }
    }

    private AttributeModifier.Operation getOperation(String type) {
        return switch (type.toLowerCase()) {
            case "add", "subtract" -> AttributeModifier.Operation.ADD_NUMBER;
            case "multiply" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> {
                plugin.getLogger().warning("Invalid Attribute Operation type: " + type);
                yield null;
            }
        };
    }

    public void applyAttributes(Player player, int slot, ItemStack item) {
        if (item == null || item.getType().isAir() || !plugin.isMythicMobsEnabled()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String mythicId = meta.getPersistentDataContainer().get(plugin.getMythicIdKey(), PersistentDataType.STRING);
        if (mythicId == null || mythicId.isEmpty()) return;

        Optional<MythicItem> itemOptional = plugin.getMythicInst().getItemManager().getItem(mythicId);
        if (itemOptional.isEmpty()) return;

        MythicConfig mmConfig = itemOptional.get().getConfig().getNestedConfig("Trinket");
        if (mmConfig == null || !mmConfig.isConfigurationSection("attributes")) return;

        Map<String, MythicConfig> attributeConfigs = mmConfig.getNestedConfigs("attributes");
        if (attributeConfigs.isEmpty()) return;

        for (Map.Entry<String, MythicConfig> entry : attributeConfigs.entrySet()) {
            String attributeName = entry.getKey();
            MythicConfig modifierConfig = entry.getValue();
            Optional<StatType> stat = plugin.getMythicInst().getStatManager().getStat("stick");
            plugin.getMythicInst().getPlayerManager().getProfile(player).getStatRegistry().putBaseValue(stat.get(), 10);

            Attribute attribute = getAttribute(attributeName);
            if (attribute == null) continue;

            String type = modifierConfig.getString("type");
            double amount;

            amount = modifierConfig.getDouble("amount", 0.0);

            if (type == null || type.isEmpty()) {
                plugin.getLogger().warning("Missing or invalid 'type' for attribute " + attributeName + " in item " + mythicId);
                continue;
            }

            AttributeModifier.Operation operation = getOperation(type);
            if (operation == null) continue;

            if (type.equalsIgnoreCase("subtract")) {
                amount *= -1;
            }

            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance == null) continue;

            UUID modifierId = getModifierUUID(player, slot);
            String modifierName = MODIFIER_NAME_PREFIX + attributeName + "_" + slot;
            attributeInstance.removeModifier(modifierId);
            AttributeModifier modifier = new AttributeModifier(modifierId, modifierName, amount, operation);

            attributeInstance.addModifier(modifier);
        }
    }

    public void removeAttributes(Player player, int slot) {
        UUID modifierId = getModifierUUID(player, slot);

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(modifierId);
            }
        }
    }
}
