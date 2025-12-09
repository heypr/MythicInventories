package dev.heypr.mythicinventories.cache;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import io.lumine.mythic.api.skills.Skill;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MythicCacheManager {

    private final MythicInventories plugin;

    private final ConcurrentHashMap<UUID, Skill> cachedItemMythicSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Skill> cachedTrinketMythicSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, MythicInventory.TrinketConfig> cachedTrinketConfigs = new ConcurrentHashMap<>();

    public MythicCacheManager(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public Skill getItemSkill(UUID keyName) {
        if (!plugin.getMythicManager().isMythicMobsEnabled()) return null;
        return cachedItemMythicSkills.get(keyName);
    }

    public void cacheItemSkill(UUID skillConverted, String skillName) {
        if (getItemSkill(skillConverted) == null) {
            plugin.getMythicManager().loadSkill(skillName).ifPresent(skill ->
                    cachedItemMythicSkills.put(skillConverted, skill)
            );
        }
    }

    public Skill getTrinketSkill(UUID keyName) {
        if (!plugin.getMythicManager().isMythicMobsEnabled()) return null;
        return cachedTrinketMythicSkills.get(keyName);
    }

    public void cacheTrinketSkill(UUID skillConverted, String skillName) {
        if (getTrinketSkill(skillConverted) == null) {
            plugin.getMythicManager().loadSkill(skillName).ifPresent(skill -> cachedTrinketMythicSkills.put(skillConverted, skill));
        }
    }

    public MythicInventory.TrinketConfig getTrinketConfig(UUID uuid) {
        return cachedTrinketConfigs.get(uuid);
    }

    public void cacheTrinketConfig(UUID uuid, MythicInventory.TrinketConfig config) {
        cachedTrinketConfigs.put(uuid, config);
    }

    public void clearAllCaches() {
        cachedTrinketMythicSkills.clear();
        cachedItemMythicSkills.clear();
        cachedTrinketConfigs.clear();
    }
}
