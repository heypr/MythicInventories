package dev.heypr.mythicinventories.schedulers;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import dev.heypr.mythicinventories.inventory.MythicInventory.TrinketConfig;
import io.lumine.mythic.api.skills.Skill;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrinketScheduler {

    private final MythicInventories plugin;
    private final ConcurrentHashMap<UUID, HashMap<Integer, BukkitTask>> runningTrinketTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, HashMap<Integer, BukkitTask>> runningAttributeTasks = new ConcurrentHashMap<>();

    public TrinketScheduler(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public void startTrinketSkillTask(Player player, int slot, TrinketConfig config, MythicInventory inventory) {
        plugin.getLogger().info("[Debug] Attempting to start Skill Task for " + player.getName() + " in slot " + slot);
        stopTrinketSkillTask(player, slot);

        if (!plugin.getMythicManager().isMythicMobsEnabled()) {
            plugin.getLogger().warning("[Debug] Skill Task failed: MythicMobs not enabled.");
            return;
        }

        int interval;
        try {
            interval = Integer.parseInt(config.skillInterval());
            plugin.getLogger().info("[Debug] Parsed skill interval: " + interval);
        }
        catch (NumberFormatException e) {
            plugin.getLogger().severe("Invalid interval '" + config.skillInterval() + "' for trinket skill '" + config.skill() + "'. Task cancelled.");
            return;
        }

        String keyStr = config.skill();
        UUID skillUUID = UUID.nameUUIDFromBytes(keyStr.getBytes(StandardCharsets.UTF_8));
        Skill skill = plugin.getCacheManager().getTrinketSkill(skillUUID);
        if (skill == null) {
            plugin.getLogger().warning("[Debug] Skill Task failed: Skill '" + keyStr + "' not found in cache.");
            return;
        }

        TrinketSkillTask runnable = new TrinketSkillTask(plugin, player, slot, config, skill, inventory);
        BukkitTask bukkitTask = runnable.runTaskTimer(plugin, 1L, interval);

        runningTrinketTasks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(slot, bukkitTask);
        plugin.getLogger().info("[Debug] Skill Task started successfully.");
    }

    public void stopTrinketSkillTask(Player player, int slot) {
        plugin.getLogger().info("[Debug] Stopping Skill Task for " + player.getName() + " in slot " + slot);
        UUID playerID = player.getUniqueId();
        runningTrinketTasks.computeIfPresent(playerID, (uuid, slotMap) -> {
            BukkitTask task = slotMap.remove(slot);
            if (task != null && !task.isCancelled()) {
                task.cancel();
                plugin.getLogger().info("[Debug] Skill Task cancelled.");
            }
            return slotMap.isEmpty() ? null : slotMap;
        });
    }

    public void startTrinketAttributeTask(Player player, int slot, TrinketConfig config, MythicInventory inventory) {
        plugin.getLogger().info("[Debug] Attempting to start Attribute Task for " + player.getName() + " in slot " + slot);
        stopTrinketAttributeTask(player, slot);

        int interval;
        try {
            interval = Integer.parseInt(config.attributeInterval());
        }
        catch (NumberFormatException e) {
            plugin.getLogger().info("[Debug] Invalid attribute interval, defaulting to 20.");
            interval = 20;
        }

        TrinketAttributeTask task = new TrinketAttributeTask(plugin, player, slot, inventory, config);
        BukkitTask bukkitTask = task.runTaskTimer(plugin, 1L, interval);

        runningAttributeTasks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(slot, bukkitTask);
        plugin.getLogger().info("[Debug] Attribute Task started with interval " + interval);
    }

    public void stopTrinketAttributeTask(Player player, int slot) {
        plugin.getLogger().info("[Debug] Stopping Attribute Task for " + player.getName() + " in slot " + slot);
        runningAttributeTasks.computeIfPresent(player.getUniqueId(), (uuid, slotMap) -> {
            BukkitTask task = slotMap.remove(slot);
            if (task != null) {
                task.cancel();
                plugin.getLogger().info("[Debug] Attribute Task cancelled.");
            }
            return slotMap.isEmpty() ? null : slotMap;
        });
    }

    public void stopAllTrinketSkillTasks(Player player) {
        plugin.getLogger().info("[Debug] Stopping ALL tasks for player " + player.getName());
        UUID playerID = player.getUniqueId();

        HashMap<Integer, BukkitTask> slotMap = runningTrinketTasks.remove(playerID);
        if (slotMap != null) {
            slotMap.values().forEach(task -> {
                if (!task.isCancelled()) task.cancel();
            });
        }

        HashMap<Integer, BukkitTask> attrMap = runningAttributeTasks.remove(playerID);
        if (attrMap != null) {
            attrMap.values().forEach(task -> {
                if (!task.isCancelled()) task.cancel();
            });
        }
    }

    public void stopAllTrinketSkillTasks() {
        plugin.getLogger().info("[Debug] Stopping ALL trinket tasks globally.");
        this.runningTrinketTasks.clear();
        this.runningAttributeTasks.clear();
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }
}
