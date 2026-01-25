package dev.heypr.mythicinventories.trinket;

import dev.heypr.mythicinventories.MythicInventories;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.core.items.MythicItem;
import io.lumine.mythic.core.skills.stats.StatType;
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
            return null;
        }
    }

    private StatType getMythicStat(Player player, String stat) {
        try {
            for (StatType statType : plugin.getMythicManager().getMythicInst().getPlayerManager().getProfile(player).getStatRegistry().getApplicableStats()) {
                if (statType.getKey().equalsIgnoreCase(stat)) {
                    return statType;
                }
            }
        }
        catch (Exception e) {
            plugin.getLogger().warning("[Debug] Error searching for Mythic Stat: " + stat);
        }
        return null;
    }

    private AttributeModifier.Operation getVanillaOperation(String type) {
        return switch (type.toLowerCase()) {
            case "add", "subtract" -> AttributeModifier.Operation.ADD_NUMBER;
            case "multiply" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> {
                plugin.getLogger().warning("[Debug] Invalid Attribute Operation type: " + type);
                yield null;
            }
        };
    }

    public void applyAttributes(Player player, int slot, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            plugin.getLogger().info("[Debug] applyAttributes: Item is null/air.");
            return;
        }
        if (!plugin.getMythicManager().isMythicMobsEnabled()) {
            plugin.getLogger().warning("[Debug] applyAttributes: MythicMobs not enabled.");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String mythicId = meta.getPersistentDataContainer().get(plugin.getMythicIdKey(), PersistentDataType.STRING);
        if (mythicId == null || mythicId.isEmpty()) {
            plugin.getLogger().info("[Debug] Item in slot " + slot + " has no Mythic ID metadata.");
            return;
        }

        Optional<MythicItem> itemOptional = plugin.getMythicManager().getMythicInst().getItemManager().getItem(mythicId);
        if (itemOptional.isEmpty()) {
            plugin.getLogger().warning("[Debug] Mythic Item ID '" + mythicId + "' not found in MythicMobs.");
            return;
        }

        MythicConfig mmConfig = itemOptional.get().getConfig().getNestedConfig("Trinket");
        if (mmConfig == null || !mmConfig.isConfigurationSection("attributes")) {
            plugin.getLogger().info("[Debug] Mythic Item '" + mythicId + "' has no Trinket Attributes defined.");
            return;
        }

        Map<String, MythicConfig> attributeConfigs = mmConfig.getNestedConfigs("attributes");
        plugin.getLogger().info("[Debug] Processing " + attributeConfigs.size() + " attributes for " + player.getName());

        for (Map.Entry<String, MythicConfig> entry : attributeConfigs.entrySet()) {
            String key = entry.getKey();
            MythicConfig modifierConfig = entry.getValue();
            String type = modifierConfig.getString("type", "add");
            double amount = modifierConfig.getDouble("amount", 0.0);

            if (type.equalsIgnoreCase("subtract")) amount *= -1;

            Attribute vanillaAttr = getAttribute(key);
            if (vanillaAttr != null) {
                plugin.getLogger().info("[Debug] Applying Vanilla Attribute: " + key + " (Value: " + amount + ")");
                applyVanilla(player, slot, vanillaAttr, type, amount);
                continue;
            }

            StatType mythicStat = getMythicStat(player, key);
            if (mythicStat != null) {
                plugin.getLogger().info("[Debug] Applying Mythic Stat: " + key + " (Value: " + amount + ")");
                applyMythic(player, slot, mythicStat, type, amount);
            }
            else {
                plugin.getLogger().warning("[Debug] Attribute/Stat '" + key + "' not recognized as Vanilla or Mythic.");
            }
        }
    }

    private void applyVanilla(Player player, int slot, Attribute attribute, String type, double amount) {
        AttributeInstance ai = player.getAttribute(attribute);
        if (ai == null) return;

        AttributeModifier.Operation op = getVanillaOperation(type);
        if (op == null) return;

        UUID uuid = getModifierUUID(player, slot);
        ai.removeModifier(uuid);
        ai.addModifier(new AttributeModifier(uuid, MODIFIER_NAME_PREFIX + slot, amount, op));
    }

    private void applyMythic(Player player, int slot, StatType stat, String type, double amount) {
        var statRegistry = plugin.getMythicManager().getMythicInst().getPlayerManager().getProfile(player).getStatRegistry();
        double currentBase = statRegistry.get(stat);
        double newBase;

        switch (type.toLowerCase()) {
            case "add", "subtract" -> newBase = currentBase + amount;
            case "multiply" -> newBase = currentBase * amount;
            case "set" -> newBase = amount;
            default -> {
                return;
            }
        }

        double delta = newBase - currentBase;
        statRegistry.putBaseValue(stat, newBase);

        plugin.getCacheManager().recordStatChange(player.getUniqueId(), slot, stat, delta);
        plugin.getLogger().info("[Debug] Mythic Stat " + stat.getKey() + " changed by " + delta + " (New Base: " + newBase + ")");
    }

    public void removeAttributes(Player player, int slot) {
        plugin.getLogger().info("[Debug] Removing attributes for " + player.getName() + " from slot " + slot);

        UUID modifierId = getModifierUUID(player, slot);
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attr = player.getAttribute(attribute);
            if (attr != null) attr.removeModifier(modifierId);
        }

        Map<StatType, Double> appliedChanges = plugin.getCacheManager().getAndClearStatChanges(player.getUniqueId(), slot);
        if (appliedChanges != null) {
            appliedChanges.forEach((stat, delta) -> {
                double currentBase = plugin.getMythicManager().getMythicInst().getPlayerManager().getProfile(player).getStatRegistry().get(stat);
                plugin.getMythicManager().getMythicInst().getPlayerManager().getProfile(player).getStatRegistry().putBaseValue(stat, currentBase - delta);
                plugin.getLogger().info("[Debug] Reversed Mythic Stat: " + stat.getKey() + " by " + (-delta));
            });
        }

        plugin.getLogger().info("[Debug] Attributes removed.");
    }
}
