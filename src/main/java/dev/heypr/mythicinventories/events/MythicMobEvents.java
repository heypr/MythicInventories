package dev.heypr.mythicinventories.events;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.mythicmobs.OpenInventoryMechanic;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMobItemGenerateEvent;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MythicMobEvents implements Listener {

    private final MythicInventories plugin;

    public MythicMobEvents(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event) {
        if (event.getMechanicName().equalsIgnoreCase("openinventory")) {
            event.register(new OpenInventoryMechanic(event.getConfig(), plugin));
        }
    }

    @EventHandler
    public void onItemGenEvent(MythicMobItemGenerateEvent event) {
        MythicItem mythicItem = event.getItem();
        ItemStack itemStack = event.getItemStack();

        if (itemStack == null || mythicItem == null) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        if (!event.getItem().getConfig().contains("Trinket")) {
            String mythicId = mythicItem.getInternalName();
            meta.getPersistentDataContainer().set(plugin.getMythicIdKey(), PersistentDataType.STRING, mythicId);
            itemStack.setItemMeta(meta);
            event.setItemStack(itemStack);
            return;
        }

        String mythicId = mythicItem.getInternalName();

        meta.getPersistentDataContainer().set(plugin.getMythicIdKey(), PersistentDataType.STRING, mythicId);

        MythicConfig mmConfig = event.getItem().getConfig().getNestedConfig("Trinket");

        String skill = mmConfig.getString("skill", "NO_SKILL");
        String interval = mmConfig.getString("interval", "100");
        String uses = mmConfig.getString("uses", "-1");
        boolean disappears = mmConfig.getBoolean("initial_item_disappears", false);

        MythicInventory.TrinketConfig finalConfig = new MythicInventory.TrinketConfig(skill, interval, uses, disappears);
        UUID cacheId = UUID.randomUUID();
        plugin.addToTrinketConfigCache(cacheId, finalConfig);

        meta.getPersistentDataContainer().set(plugin.getTrinketCacheKey(), PersistentDataType.STRING, cacheId.toString());

        itemStack.setItemMeta(meta);
        event.setItemStack(itemStack);
    }
}
