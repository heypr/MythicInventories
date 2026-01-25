package dev.heypr.mythicinventories.schedulers;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import dev.heypr.mythicinventories.inventory.MythicInventory.TrinketConfig;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.SkillTriggers;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class TrinketSkillTask extends BukkitRunnable {

    private final MythicInventories plugin;
    private final Player player;
    private final Skill cachedSkill;
    private final int slot;
    private final MythicInventory inventory;
    private final TrinketConfig config;
    private final boolean infiniteUses;
    private final NamespacedKey usesKey;

    public TrinketSkillTask(MythicInventories plugin, Player player, int slot, TrinketConfig config, Skill cachedSkill, MythicInventory inventory) {
        this.plugin = plugin;
        this.player = player;
        this.slot = slot;
        this.config = config;
        this.cachedSkill = cachedSkill;
        this.inventory = inventory;
        this.infiniteUses = config.skillUses().equals("-1") || config.skillUses().equalsIgnoreCase("infinite");
        this.usesKey = new NamespacedKey(plugin, "trinket_remaining_uses");
    }

    @Override
    public void run() {
        if (!player.isOnline() || !inventory.isTrinketSlot(slot)) {
            cleanup();
            return;
        }

        ItemStack item = inventory.getInventory().getItem(slot);
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            cleanup();
            return;
        }

        if (!infiniteUses) {
            ItemMeta meta = item.getItemMeta();

            if (!meta.getPersistentDataContainer().has(usesKey)) {
                try {
                    int initial = Integer.parseInt(config.skillUses());
                    meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, initial);
                    item.setItemMeta(meta);
                    inventory.getInventory().setItem(slot, item);
                }
                catch (Exception e) {
                    handleExhaustion();
                    return;
                }
            }

            int remaining = meta.getPersistentDataContainer().getOrDefault(usesKey, PersistentDataType.INTEGER, 0);

            if (remaining <= 0) {
                handleExhaustion();
                return;
            }

            GenericCaster caster = new GenericCaster(BukkitAdapter.adapt(player));
            SkillMetadata skillMeta = plugin.getMythicManager().getMythicInst().getSkillManager().getEventBus()
                    .buildSkillMetadata(SkillTriggers.API, caster, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);

            if (cachedSkill.isUsable(skillMeta)) {
                cachedSkill.execute(skillMeta);
                remaining--;
                meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, remaining);
                item.setItemMeta(meta);
                inventory.getInventory().setItem(slot, item);
                if (remaining <= 0) handleExhaustion();
            }
        }
        else {
            GenericCaster caster = new GenericCaster(BukkitAdapter.adapt(player));
            SkillMetadata skillMeta = plugin.getMythicManager().getMythicInst().getSkillManager().getEventBus()
                    .buildSkillMetadata(SkillTriggers.API, caster, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);

            if (cachedSkill.isUsable(skillMeta)) {
                cachedSkill.execute(skillMeta);
            }
        }
    }

    private void handleExhaustion() {
        plugin.getTrinketManager().getReplacementItem(config.skillRunOutItem()).ifPresent(rep -> inventory.getInventory().setItem(slot, rep));
        cleanup();
        plugin.getInventorySerializer().saveInventory(inventory, player);
    }

    private void cleanup() {
        plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
    }
}
