package dev.heypr.mythicinventories.listeners;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import dev.heypr.mythicinventories.inventory.MythicInventory.TrinketConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BukkitEvents implements Listener {

    private final MythicInventories plugin;

    public BukkitEvents(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            HashMap<String, HashMap<Integer, ItemStack>> allSavedData = plugin.getInventorySerializer().loadInventory(player);

            for (Map.Entry<String, HashMap<Integer, ItemStack>> entry : allSavedData.entrySet()) {
                String invName = entry.getKey();
                HashMap<Integer, ItemStack> items = entry.getValue();

                MythicInventory inv = new MythicInventory(plugin, invName);

                items.forEach(inv::setItem);
                plugin.getInventoryManager().registerActiveInventory(player.getUniqueId(), inv);

                for (Map.Entry<Integer, ItemStack> itemEntry : items.entrySet()) {
                    int slot = itemEntry.getKey();
                    ItemStack item = itemEntry.getValue();

                    if (!inv.isTrinketSlot(slot)) continue;

                    plugin.getAttributeManager().applyAttributes(player, slot, item);

                    TrinketConfig config = plugin.getTrinketManager().getTrinketConfigFromItem(item);
                    if (config != null) {
                        if (config.skill() != null && !config.skill().isEmpty()) {
                            plugin.getTrinketScheduler().startTrinketSkillTask(player, slot, config, inv);
                        }
                        if (config.attributeInterval() != null) {
                            plugin.getTrinketScheduler().startTrinketAttributeTask(player, slot, config, inv);
                        }
                    }
                }
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getTrinketScheduler().stopAllTrinketSkillTasks(player);

        List<MythicInventory> inventories = plugin.getInventoryManager().getActiveInventories(uuid);
        for (MythicInventory inv : inventories) {
            for (int i = 0; i < inv.getInventory().getSize(); i++) {
                if (inv.isTrinketSlot(i)) {
                    plugin.getAttributeManager().removeAttributes(player, i);
                }
            }
            plugin.getInventorySerializer().saveInventory(inv, player);
        }

        plugin.getInventoryManager().clearActiveInventories(uuid);
        plugin.getCacheManager().clearPlayerCache(uuid);
    }
}
