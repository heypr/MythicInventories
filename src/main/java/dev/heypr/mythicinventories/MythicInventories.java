package dev.heypr.mythicinventories;

import dev.heypr.mythicinventories.bstats.Metrics;
import dev.heypr.mythicinventories.cache.MythicCacheManager;
import dev.heypr.mythicinventories.commands.OpenInventoryCommand;
import dev.heypr.mythicinventories.commands.OpenInventoryTabCompleter;
import dev.heypr.mythicinventories.inventory.InventoryManager;
import dev.heypr.mythicinventories.listeners.BukkitInventoryEvents;
import dev.heypr.mythicinventories.listeners.MythicMobEvents;
import dev.heypr.mythicinventories.mythicmobs.MythicManager;
import dev.heypr.mythicinventories.schedulers.TrinketScheduler;
import dev.heypr.mythicinventories.storage.MythicInventorySerializer;
import dev.heypr.mythicinventories.trinket.AttributeManager;
import dev.heypr.mythicinventories.trinket.TrinketManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class MythicInventories extends JavaPlugin {

    private NamespacedKey mythicIdKey;
    private NamespacedKey trinketCacheKey;

    private TrinketScheduler trinketScheduler;
    private AttributeManager attributeManager;
    private MythicInventorySerializer inventorySerializer;

    private MythicCacheManager cacheManager;
    private MythicManager mythicManager;
    private InventoryManager inventoryManager;
    private TrinketManager trinketManager;

    private MythicInventories plugin;

    @Override
    public void onEnable() {
        this.mythicIdKey = new NamespacedKey(this, "mythic_item_id");
        this.trinketCacheKey = new NamespacedKey(this, "trinket_config_uuid");

        this.trinketScheduler = new TrinketScheduler(this);
        this.attributeManager = new AttributeManager(this);
        this.inventorySerializer = new MythicInventorySerializer(this);

        this.mythicManager = new MythicManager(this);
        this.cacheManager = new MythicCacheManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.trinketManager = new TrinketManager(this);

        this.plugin = this;

        getCommand("mythicinventoryopen").setExecutor(new OpenInventoryCommand(this.plugin));
        getCommand("mythicinventoryopen").setTabCompleter(new OpenInventoryTabCompleter(this.plugin));

        getCommand("mythicinventoryreload").setExecutor((sender, command, label, args) -> {
            this.inventoryManager.reloadInventories();
            sender.sendMessage("Inventories reloaded!");
            return true;
        });

        getServer().getPluginManager().registerEvents(new BukkitInventoryEvents(this), this);

        if (this.mythicManager.isMythicMobsEnabled()) {
            getServer().getPluginManager().registerEvents(new MythicMobEvents(this), this);
        }
        else {
            getLogger().warning("MythicMobs was not found! MythicInventories will have reduced functionality.");
        }

        this.inventoryManager.createInventoriesDirectory();
        this.inventoryManager.reloadInventories();

        new Metrics(this, 23863);

        getLogger().info("MythicInventories enabled!");
    }

    @Override
    public void onDisable() {
        this.trinketScheduler.stopAllTrinketSkillTasks();
        this.cacheManager.clearAllCaches();
        getLogger().info("MythicInventories disabled!");
    }

    public TrinketScheduler getTrinketScheduler() {
        return trinketScheduler;
    }

    public AttributeManager getAttributeManager() {
        return attributeManager;
    }

    public MythicInventorySerializer getInventorySerializer() {
        return inventorySerializer;
    }

    public MythicCacheManager getCacheManager() {
        return cacheManager;
    }

    public NamespacedKey getMythicIdKey() {
        return mythicIdKey;
    }

    public NamespacedKey getTrinketCacheKey() {
        return trinketCacheKey;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public TrinketManager getTrinketManager() {
        return trinketManager;
    }

    public MythicManager getMythicManager() {
        return mythicManager;
    }
}
