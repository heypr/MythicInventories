package dev.heypr.mythicinventories;

import dev.heypr.mythicinventories.bstats.Metrics;
import dev.heypr.mythicinventories.commands.OpenInventoryCommand;
import dev.heypr.mythicinventories.commands.OpenInventoryTabCompleter;
import dev.heypr.mythicinventories.events.BukkitInventoryEvents;
import dev.heypr.mythicinventories.events.MythicMobEvents;
import dev.heypr.mythicinventories.inventories.InventoryCreator;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.storage.MythicInventorySerializer;
import dev.heypr.mythicinventories.util.TrinketScheduler;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.SkillTriggers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MythicInventories extends JavaPlugin implements Listener {

    // Format: internal inventory name -> MythicInventory object
    private final HashMap<String, MythicInventory> inventories = new HashMap<>();
    private final ConcurrentHashMap<UUID, Skill> cachedItemMythicSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Skill> cachedTrinketMythicSkills = new ConcurrentHashMap<>();

    private TrinketScheduler trinketScheduler;
    private boolean isPaperServer = false;

    @Override
    public void onEnable() {
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

        Bukkit.getPluginManager().registerEvents(new BukkitInventoryEvents(this), this);
        this.trinketScheduler = new TrinketScheduler(this);

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

    /**
     * Get the trinket scheduler.
     * @return The trinket scheduler.
     */
    public TrinketScheduler getTrinketScheduler() {
        return trinketScheduler;
    }

    /**
     * Get the inventory serializer.
     * @return The inventory serializer.
     */
    public MythicInventorySerializer getInventorySerializer() {
        return new MythicInventorySerializer(this);
    }

    /**
     * Reload all inventories.
     */
    public void reloadInventories() {
        trinketScheduler.stopAllTrinketSkillTasks();
        cachedTrinketMythicSkills.clear();
        cachedItemMythicSkills.clear();
        inventories.clear();
        new InventoryCreator(this).createInventories();
    }

    /**
     * Creates the "inventories" directory if it doesn't exist.
     */
    private void createInventoriesDirectory() {
        File inventoriesDir = new File(getDataFolder(), "inventories");
        if (!inventoriesDir.exists()) {
            inventoriesDir.mkdirs();
        }
    }

    /**
     * Get a map of all inventories.
     * @return A map of all inventories.
     */
    public HashMap<String, MythicInventory> getInventories() {
        return inventories;
    }

    /**
     * Get an inventory by its internal name.
     * @param inventoryId The internal name of the inventory.
     * @return The inventory with the given internal name.
     */
    public MythicInventory getInventory(String inventoryId) {
        return inventories.get(inventoryId);
    }

    /**
     * Get a list of all inventory names.
     * @return A list of all inventory names.
     */
    public List<String> getInventoryNames() {
        return inventories.keySet().stream().toList();
    }

    /**
     * Add an inventory to the list.
     * @param inventory The inventory to add.
     * @param inventoryId The internal name of the inventory.
     */
    public void addInventory(MythicInventory inventory, String inventoryId) {
        inventories.put(inventoryId, inventory);
    }

    /**
     * Check if MythicMobs is enabled.
     * @return True if MythicMobs is enabled, false otherwise.
     */
    public boolean isMythicMobsEnabled() {
        return getServer().getPluginManager().isPluginEnabled("MythicMobs");
    }

    /**
     * Get the MythicBukkit instance.
     * @return The MythicBukkit instance.
     */
    public MythicBukkit getMythicInst() {
        return MythicBukkit.inst();
    }

    /**
     * Check if the server is running Paper.
     * @return True if the server is running Paper, false otherwise.
     */
    public boolean isPaperServer() {
        return isPaperServer;
    }
}
