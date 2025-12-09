package dev.heypr.mythicinventories.mythicmobs;

import dev.heypr.mythicinventories.MythicInventories;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.SkillTriggers;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class MythicManager {

    private final MythicInventories plugin;

    public MythicManager(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public boolean isMythicMobsEnabled() {
        return plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs");
    }

    public MythicBukkit getMythicInst() {
        return MythicBukkit.inst();
    }

    public Optional<Skill> loadSkill(String skillName) {
        if (!isMythicMobsEnabled()) return Optional.empty();
        return getMythicInst().getSkillManager().getSkill(null, Collections.singleton(skillName));
    }

    public void executeMythicSkill(Player player, String skillName) {
        if (!isMythicMobsEnabled()) {
            plugin.getLogger().severe("Attempted to execute skill '" + skillName + "' but MythicMobs is disabled.");
            return;
        }

        UUID skillUUID = UUID.nameUUIDFromBytes(skillName.getBytes(StandardCharsets.UTF_8));
        Skill skill = plugin.getCacheManager().getItemSkill(skillUUID);

        if (skill == null) {
            plugin.getLogger().severe("Skill '" + skillName + "' not found!");
            return;
        }

        GenericCaster caster = new GenericCaster(BukkitAdapter.adapt(player));
        SkillMetadata meta = getMythicInst().getSkillManager().getEventBus().buildSkillMetadata(
                SkillTriggers.API,
                caster,
                BukkitAdapter.adapt(player),
                BukkitAdapter.adapt(player.getLocation()),
                true
        );

        if (skill.isUsable(meta)) {
            skill.execute(meta);
        }
    }
}