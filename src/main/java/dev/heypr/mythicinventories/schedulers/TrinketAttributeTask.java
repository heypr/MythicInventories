package dev.heypr.mythicinventories.schedulers;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import dev.heypr.mythicinventories.inventory.MythicInventory.TrinketConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class TrinketAttributeTask extends BukkitRunnable {

    private final MythicInventories plugin;
    private final Player player;
    private final int slot;
    private final MythicInventory inventory;
    private final TrinketConfig config;
    private final boolean infiniteUses;
    private final NamespacedKey attrUsesKey;
    private boolean attributesApplied = false;

    public TrinketAttributeTask(MythicInventories plugin, Player player, int slot, MythicInventory inventory, TrinketConfig config) {
        this.plugin = plugin;
        this.player = player;
        this.slot = slot;
        this.inventory = inventory;
        this.config = config;
        this.infiniteUses = config.attributeUses().equals("-1") || config.attributeUses().equalsIgnoreCase("infinite");
        this.attrUsesKey = new NamespacedKey(plugin, "trinket_attribute_remaining_uses");
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

        if (!attributesApplied) {
            plugin.getAttributeManager().applyAttributes(player, slot, item);
            attributesApplied = true;
        }

        if (!infiniteUses) {
            ItemMeta meta = item.getItemMeta();

            if (!meta.getPersistentDataContainer().has(attrUsesKey)) {
                try {
                    int initial = Integer.parseInt(config.attributeUses());
                    meta.getPersistentDataContainer().set(attrUsesKey, PersistentDataType.INTEGER, initial);
                    item.setItemMeta(meta);
                    inventory.getInventory().setItem(slot, item);
                }
                catch (Exception e) {
                    handleExhaustion();
                    return;
                }
            }

            int remaining = meta.getPersistentDataContainer().getOrDefault(attrUsesKey, PersistentDataType.INTEGER, 0);

            if (remaining <= 0) {
                handleExhaustion();
                return;
            }

            remaining--;
            meta.getPersistentDataContainer().set(attrUsesKey, PersistentDataType.INTEGER, remaining);
            item.setItemMeta(meta);
            inventory.getInventory().setItem(slot, item);

            if (remaining <= 0) {
                handleExhaustion();
            }
        }
    }

    private void handleExhaustion() {
        plugin.getTrinketManager().getReplacementItem(config.attributeRunOutItem()).ifPresent(rep -> inventory.getInventory().setItem(slot, rep));
        cleanup();
        plugin.getInventorySerializer().saveInventory(inventory, player);
    }

    private void cleanup() {
        plugin.getAttributeManager().removeAttributes(player, slot);
        plugin.getTrinketScheduler().stopTrinketAttributeTask(player, slot);
    }
}
