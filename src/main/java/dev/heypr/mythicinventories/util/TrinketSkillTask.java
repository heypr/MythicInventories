package dev.heypr.mythicinventories.util;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.inventories.MythicInventory.TrinketConfig;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.core.skills.SkillTriggers;
import io.lumine.mythic.core.utils.jnbt.CompoundTag;
import io.lumine.mythic.core.utils.jnbt.CompoundTagBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class TrinketSkillTask extends BukkitRunnable {

    private final MythicInventories plugin;
    private final Player player;
    private final Skill cachedSkill;
    private final int slot;
    private int remainingUses;
    private ItemStack trinketItem;
    private MythicInventory inventory;

    public TrinketSkillTask(MythicInventories plugin, Player player, int slot, TrinketConfig config, Skill cachedSkill, ItemStack trinketItem, MythicInventory inventory) {
        this.plugin = plugin;
        this.player = player;
        this.slot = slot;
        this.cachedSkill = cachedSkill;
        this.trinketItem = trinketItem;
        this.inventory = inventory;

        String usesString = config.uses();
        if (usesString.equals("-1") || usesString.equalsIgnoreCase("infinite")) {
            this.remainingUses = -1;
        }
        else {
            this.remainingUses = Integer.parseInt(usesString);
        }
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
            return;
        }

        if (remainingUses != -1) {
            remainingUses--;
        }

        GenericCaster caster = new GenericCaster(BukkitAdapter.adapt(player));
        SkillMetadata meta = plugin.getMythicInst().getSkillManager().getEventBus().buildSkillMetadata(SkillTriggers.API, caster, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);

        if (cachedSkill.isUsable(meta)) {
            cachedSkill.execute(meta);
            BukkitItemStack item = BukkitAdapter.adapt(trinketItem);
            inventory.setItem(slot, BukkitAdapter.adapt(item.customData(item.getCustomData().setValue(CompoundTagBuilder.create().putInt("trinket_uses", remainingUses).build().getValue()))));
        }

        if (remainingUses == 0) {
            plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
        }
    }
}
