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

import java.util.UUID;

public class TrinketSkillTask extends BukkitRunnable {

    private final MythicInventories plugin;
    private final Player player;
    private final Skill cachedSkill;
    private final int slot;
    private final MythicInventory inventory;
    private final boolean infiniteUses;
    private final UUID initialConfigUUID;

    public TrinketSkillTask(MythicInventories plugin, Player player, int slot, TrinketConfig config, Skill cachedSkill, MythicInventory inventory) {
        this.plugin = plugin;
        this.player = player;
        this.slot = slot;
        this.cachedSkill = cachedSkill;
        this.inventory = inventory;
        this.infiniteUses = config.uses().equals("-1") || config.uses().equalsIgnoreCase("infinite");

        ItemStack currentItem = inventory.getInventory().getItem(slot);
        if (currentItem != null && currentItem.hasItemMeta()) {
            String idString = currentItem.getItemMeta().getPersistentDataContainer().get(plugin.getTrinketCacheKey(), PersistentDataType.STRING);
            this.initialConfigUUID = idString != null ? UUID.fromString(idString) : null;
        }
        else {
            this.initialConfigUUID = null;
        }
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
            return;
        }

        if (!inventory.isTrinketSlot(slot)) {
            plugin.getLogger().warning("Trinket slot " + slot + " in inventory " + inventory.getInternalName() + " was removed while skill was running. Cleaning up effects for player " + player.getName());

            plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
            plugin.getAttributeManager().removeAttributes(player, slot);
            plugin.getInventorySerializer().saveInventory(inventory, player);
            return;
        }

        ItemStack trinketItem = inventory.getInventory().getItem(slot);
        if (trinketItem == null || trinketItem.getType().isAir() || !trinketItem.hasItemMeta()) {
            plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
            return;
        }

        ItemMeta meta = trinketItem.getItemMeta();

        int remainingUses = infiniteUses ? -1 : getUsesFromPDC(meta);

        if (remainingUses == 0) {
            plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
            plugin.getInventorySerializer().saveInventory(inventory, player);
            return;
        }

        if (remainingUses != -1) {
            remainingUses--;
        }

        GenericCaster caster = new GenericCaster(BukkitAdapter.adapt(player));
        SkillMetadata skillMeta = plugin.getMythicManager().getMythicInst().getSkillManager().getEventBus().buildSkillMetadata(SkillTriggers.API, caster, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);

        if (cachedSkill.isUsable(skillMeta)) {
            cachedSkill.execute(skillMeta);
            if (!infiniteUses) {
                NamespacedKey usesKey = new NamespacedKey(plugin, "trinket_remaining_uses");
                meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, remainingUses);
                trinketItem.setItemMeta(meta);

                inventory.getInventory().setItem(slot, trinketItem);
                plugin.getInventorySerializer().saveInventory(inventory, player);
            }
        }

        if (remainingUses == 0) {
            plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
            plugin.getInventorySerializer().saveInventory(inventory, player);
        }
    }

    private int getUsesFromPDC(ItemMeta meta) {
        NamespacedKey usesKey = new NamespacedKey(plugin, "trinket_remaining_uses");

        if (meta.getPersistentDataContainer().has(usesKey)) {
            return (meta.getPersistentDataContainer().getOrDefault(usesKey, PersistentDataType.INTEGER, 0));
        }

        if (initialConfigUUID != null) {
            TrinketConfig config = plugin.getCacheManager().getTrinketConfig(initialConfigUUID);
            if (config != null) {
                try {
                    int initialUses = Integer.parseInt(config.uses());
                    meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, initialUses);
                    return initialUses;
                }
                catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
