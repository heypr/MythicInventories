package dev.heypr.mythicinventories.cache;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.core.skills.stats.StatType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MythicCacheManager {

    private final MythicInventories plugin;

    private final ConcurrentHashMap<UUID, Skill> cachedItemMythicSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Skill> cachedTrinketMythicSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<Integer, Map<StatType, Double>>> activeMythicStatChanges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, MythicInventory.TrinketConfig> cachedTrinketConfigs = new ConcurrentHashMap<>();

    public MythicCacheManager(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public Skill getItemSkill(UUID keyName) {
        return cachedItemMythicSkills.get(keyName);
    }

    public void cacheItemSkill(UUID skillConverted, String skillName) {
        if (!plugin.getMythicManager().isMythicMobsEnabled()) return;
        if (getItemSkill(skillConverted) == null) {
            plugin.getMythicManager().loadSkill(skillName).ifPresent(skill -> {
                cachedItemMythicSkills.put(skillConverted, skill);
                plugin.getLogger().info("[Debug] Cached Item Skill: " + skillName);
            });
        }
    }

    public Skill getTrinketSkill(UUID keyName) {
        return cachedTrinketMythicSkills.get(keyName);
    }

    public void cacheTrinketSkill(UUID skillConverted, String skillName) {
        if (!plugin.getMythicManager().isMythicMobsEnabled()) return;
        if (getTrinketSkill(skillConverted) == null) {
            plugin.getMythicManager().loadSkill(skillName).ifPresentOrElse(skill -> {
                cachedTrinketMythicSkills.put(skillConverted, skill);
                plugin.getLogger().info("[Debug] Cached Trinket Skill: " + skillName);
            }, () -> {
                plugin.getLogger().warning("[Debug] Failed to find Mythic Skill for caching: " + skillName);
            });
        }
    }

    public MythicInventory.TrinketConfig getTrinketConfig(UUID uuid) {
        return cachedTrinketConfigs.get(uuid);
    }

    public void cacheTrinketConfig(UUID uuid, MythicInventory.TrinketConfig config) {
        plugin.getLogger().info("[Debug] Caching TrinketConfig for UUID: " + uuid);
        cachedTrinketConfigs.put(uuid, config);
    }

    public void recordStatChange(UUID playerUUID, int slot, StatType stat, double amount) {
        activeMythicStatChanges
                .computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(slot, k -> new ConcurrentHashMap<>())
                .put(stat, amount);
    }

    public Map<StatType, Double> getAndClearStatChanges(UUID playerUUID, int slot) {
        Map<Integer, Map<StatType, Double>> playerMap = activeMythicStatChanges.get(playerUUID);
        if (playerMap == null) return null;
        return playerMap.remove(slot);
    }

    public void clearPlayerCache(UUID playerUUID) {
        activeMythicStatChanges.remove(playerUUID);
    }

    public void clearAllCaches() {
        plugin.getLogger().info("[Debug] Clearing all skill and config caches.");
        cachedTrinketMythicSkills.clear();
        cachedItemMythicSkills.clear();
        cachedTrinketConfigs.clear();
        activeMythicStatChanges.clear();
    }
}
