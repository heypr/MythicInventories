package dev.heypr.mythicinventories;

import dev.heypr.mythicinventories.bstats.Metrics;
import dev.heypr.mythicinventories.commands.OpenInventoryCommand;
import dev.heypr.mythicinventories.commands.OpenInventoryTabCompleter;
import dev.heypr.mythicinventories.events.BukkitInventoryEvents;
import dev.heypr.mythicinventories.events.MythicMobEvents;
import dev.heypr.mythicinventories.inventories.InventoryCreator;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.storage.MythicInventorySerializer;
import dev.heypr.mythicinventories.util.AttributeManager;
import dev.heypr.mythicinventories.util.TrinketScheduler;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import io.lumine.mythic.core.skills.SkillTriggers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MythicInventories extends JavaPlugin implements Listener {

    private NamespacedKey mythicIdKey;
    private NamespacedKey trinketCacheKey;

    private final HashMap<String, MythicInventory> inventories = new HashMap<>();
    private final ConcurrentHashMap<UUID, Skill> cachedItemMythicSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Skill> cachedTrinketMythicSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, MythicInventory.TrinketConfig> cachedTrinketConfigs = new ConcurrentHashMap<>();

    private TrinketScheduler trinketScheduler;
    private AttributeManager attributeManager;
    private MythicInventorySerializer inventorySerializer;
    private boolean isPaperServer = false;

    @Override
    public void onEnable() {
        this.mythicIdKey = new NamespacedKey(this, "mythic_item_id");
        this.trinketCacheKey = new NamespacedKey(this, "trinket_config_uuid");

        try {
            Class.forName("com.destroystokyo.paper.event.player.PlayerSetSpawnEvent");
            isPaperServer = true;
        }
        catch (ClassNotFoundException ignored) {}
        getCommand("mythicinventoryopen").setExecutor(new OpenInventoryCommand(this));
        getCommand("mythicinventoryopen").setTabCompleter(new OpenInventoryTabCompleter(this));

        getCommand("mythicinventoryreload").setExecutor((sender, command, label, args) -> {
            reloadInventories();
            sender.sendMessage("Inventories reloaded!");
            return true;
        });

        this.trinketScheduler = new TrinketScheduler(this);
        this.attributeManager = new AttributeManager(this);
        this.inventorySerializer = new MythicInventorySerializer(this);
        Bukkit.getPluginManager().registerEvents(new BukkitInventoryEvents(this), this);

        if (!isMythicMobsEnabled()) {
            getLogger().warning("MythicMobs was not found! MythicInventories will have reduced functionality.");
        }
        else {
            Bukkit.getPluginManager().registerEvents(new MythicMobEvents(this), this);
        }

        createInventoriesDirectory();
        reloadInventories();

        new Metrics(this, 23863);

        getLogger().info("MythicInventories enabled!");
    }

    @Override
    public void onDisable() {
        trinketScheduler.stopAllTrinketSkillTasks();
        cachedTrinketMythicSkills.clear();
        cachedItemMythicSkills.clear();
        cachedTrinketConfigs.clear();
        inventories.clear();
        getLogger().info("MythicInventories disabled!");
    }

    public Skill getFromItemSkillCache(UUID keyName) {
        if (!isMythicMobsEnabled()) return null;
        return cachedItemMythicSkills.get(keyName);
    }

    public void addToItemSkillCache(UUID skillConverted, String skillName) {
        if (getFromItemSkillCache(skillConverted) == null) {
            Skill finalSkill = getMythicInst().getSkillManager().getSkill(null, Collections.singleton(skillName)).get();
            cachedItemMythicSkills.put(skillConverted, finalSkill);
        }
    }

    public void addToTrinketSkillCache(UUID skillConverted, String skillName) {
        if (getFromTrinketSkillCache(skillConverted) == null) {
            Skill finalSkill = getMythicInst().getSkillManager().getSkill(null, Collections.singleton(skillName)).get();
            cachedTrinketMythicSkills.put(skillConverted, finalSkill);
        }
    }

    public Skill getFromTrinketSkillCache(UUID keyName) {
        if (!isMythicMobsEnabled()) return null;
        return cachedTrinketMythicSkills.get(keyName);
    }

    public void addToTrinketConfigCache(UUID uuid, MythicInventory.TrinketConfig config) {
        cachedTrinketConfigs.put(uuid, config);
    }

    public void executeMythicSkill(Player player, String skillName) {
        if (!isMythicMobsEnabled()) {
            getLogger().severe("Attempted to execute skill '" + skillName + "' but MythicMobs is disabled.");
            return;
        }
        UUID skillUUID = UUID.nameUUIDFromBytes(skillName.getBytes(StandardCharsets.UTF_8));
        Skill skill = getFromItemSkillCache(skillUUID);

        if (skill == null) {
            getLogger().severe("Skill '" + skillName + "' not found!");
            return;
        }

        GenericCaster caster = new GenericCaster(BukkitAdapter.adapt(player));
        SkillMetadata meta = getMythicInst().getSkillManager().getEventBus().buildSkillMetadata(SkillTriggers.API, caster, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);

        if (skill.isUsable(meta)) {
            skill.execute(meta);
        }
    }

    public MythicInventory.TrinketConfig getTrinketConfigFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) return null;

        ItemMeta meta = item.getItemMeta();

        String cacheIdString = meta.getPersistentDataContainer().get(this.trinketCacheKey, PersistentDataType.STRING);

        if (cacheIdString != null) {
            try {
                UUID cacheId = UUID.fromString(cacheIdString);
                MythicInventory.TrinketConfig cachedConfig = cachedTrinketConfigs.get(cacheId);

                if (cachedConfig != null) {
                    return cachedConfig;
                }
            }
            catch (IllegalArgumentException e) {
                getLogger().warning("Corrupted Trinket Cache UUID found on item: " + cacheIdString);
            }
        }
        return this.loadTrinketConfigFromMythicItem(meta);
    }

    private MythicInventory.TrinketConfig loadTrinketConfigFromMythicItem(ItemMeta meta) {
        String mythicId = meta.getPersistentDataContainer().get(this.mythicIdKey, PersistentDataType.STRING);
        if (mythicId == null || mythicId.isEmpty()) return null;

        UUID newCacheKey = UUID.nameUUIDFromBytes(mythicId.getBytes(StandardCharsets.UTF_8));

        Optional<MythicItem> itemOptional = getMythicInst().getItemManager().getItem(mythicId);
        if (itemOptional.isEmpty()) {
            return null;
        }

        MythicConfig mmConfig = itemOptional.get().getConfig().getNestedConfig("Trinket");

        if (mmConfig == null) {
            return null;
        }

        String skill = mmConfig.getString("skill", "NO_SKILL");
        String interval = mmConfig.getString("interval", "100");
        String uses = mmConfig.getString("uses", "-1");
        boolean disappears = mmConfig.getBoolean("initial_item_disappears", false);
        boolean hasAttributes = mmConfig.isConfigurationSection("attributes");

        if (skill.equalsIgnoreCase("NO_SKILL") && !hasAttributes) {
            return null;
        }

        if (!skill.equalsIgnoreCase("NO_SKILL")) {
            try {
                if (Integer.parseInt(interval) <= 0) {
                    return null;
                }
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        MythicInventory.TrinketConfig finalConfig = new MythicInventory.TrinketConfig(skill, interval, uses, disappears);
        cachedTrinketConfigs.put(newCacheKey, finalConfig);
        return finalConfig;
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

    public MythicInventory.TrinketConfig getTrinketConfigFromCache(UUID configId) {
        return cachedTrinketConfigs.get(configId);
    }

    public void reloadInventories() {
        trinketScheduler.stopAllTrinketSkillTasks();
        cachedTrinketMythicSkills.clear();
        cachedItemMythicSkills.clear();
        cachedTrinketConfigs.clear();
        inventories.clear();
        new InventoryCreator(this).createInventories();
    }

    private void createInventoriesDirectory() {
        File inventoriesDir = new File(getDataFolder(), "inventories");
        if (!inventoriesDir.exists()) {
            inventoriesDir.mkdirs();
        }
    }

    public HashMap<String, MythicInventory> getInventories() {
        return inventories;
    }

    public List<String> getInventoryNames() {
        return inventories.keySet().stream().toList();
    }

    public void addInventory(MythicInventory inventory, String inventoryId) {
        inventories.put(inventoryId, inventory);
    }

    public boolean isMythicMobsEnabled() {
        return getServer().getPluginManager().isPluginEnabled("MythicMobs");
    }

    public MythicBukkit getMythicInst() {
        return MythicBukkit.inst();
    }

    public boolean isPaperServer() {
        return isPaperServer;
    }

    public NamespacedKey getMythicIdKey() {
        return mythicIdKey;
    }

    public NamespacedKey getTrinketCacheKey() {
        return trinketCacheKey;
    }
}
