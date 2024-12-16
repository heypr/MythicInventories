package dev.heypr.mythicinventories.mythicmobs;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.entity.Player;

public class OpenInventoryMechanic implements ITargetedEntitySkill {

    protected final String inventoryName;
    private final MythicInventories plugin;

    public OpenInventoryMechanic(MythicLineConfig config, MythicInventories plugin) {
        this.plugin = plugin;
        this.inventoryName = config.getString(new String[] {"inventory", "i", "invname"});
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata metadata, AbstractEntity entity) {
        Player target = (Player) BukkitAdapter.adapt(entity);
        if (inventoryName == null) {
            return SkillResult.INVALID_CONFIG;
        }

        if (!plugin.getInventories().containsKey(inventoryName)) {
            return SkillResult.ERROR;
        }

        if (!target.hasPermission("mythicinventories.open." + inventoryName)) {
            return SkillResult.ERROR;
        }

        MythicInventory mythicInventory = plugin.getInventories().get(inventoryName);

        if (plugin.getInventorySerializer().getPlayerInventoryNames(target).contains(inventoryName)) {
            target.openInventory(plugin.getInventorySerializer().loadInventory(mythicInventory, target).getInventory());
            return SkillResult.SUCCESS;
        }
        
        target.openInventory(mythicInventory.getInventory());
        return SkillResult.SUCCESS;
    }
}
