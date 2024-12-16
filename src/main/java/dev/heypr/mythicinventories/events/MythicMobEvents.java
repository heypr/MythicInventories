package dev.heypr.mythicinventories.events;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.mythicmobs.OpenInventoryMechanic;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobEvents implements Listener {

    private final MythicInventories plugin;

    public MythicMobEvents(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event)	{
        if (event.getMechanicName().equalsIgnoreCase("openinventory")) {
            event.register(new OpenInventoryMechanic(event.getConfig(), plugin));
        }
    }
}
