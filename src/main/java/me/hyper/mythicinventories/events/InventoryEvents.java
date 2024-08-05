package me.hyper.mythicinventories.events;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.hyper.mythicinventories.MythicInventories;
import me.hyper.mythicinventories.inventories.MythicInventory;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InventoryEvents implements Listener {

    private final MythicInventories plugin;

    public InventoryEvents(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getCurrentItem() instanceof ItemStack item && event.getClickedInventory() instanceof MythicInventory) {
            event.setCancelled(true);
            NamespacedKey key = new NamespacedKey(plugin, "skill");
            String skillName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (skillName != null) {
                MythicBukkit.inst().getAPIHelper().castSkill(event.getWhoClicked(), skillName);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getCursor() instanceof ItemStack item && event.getInventory() instanceof MythicInventory) {
            event.setCancelled(true);
            NamespacedKey key = new NamespacedKey(plugin, "skill");
            String skillName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (skillName != null) {
                MythicBukkit.inst().getAPIHelper().castSkill(event.getWhoClicked(), skillName);
            }

        }
        if (event.getOldCursor() instanceof ItemStack item && event.getInventory() instanceof MythicInventory) {
            event.setCancelled(true);
            NamespacedKey key = new NamespacedKey(plugin, "skill");
            String skillName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (skillName != null) {
                MythicBukkit.inst().getAPIHelper().castSkill(event.getWhoClicked(), skillName);
            }
        }
    }
}
