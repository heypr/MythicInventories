package dev.heypr.mythicinventories.util;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.inventories.MythicInventory.TrinketConfig;
import io.lumine.mythic.api.skills.Skill;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrinketScheduler {

    private final MythicInventories plugin;
    private final ConcurrentHashMap<UUID, HashMap<Integer, BukkitTask>> runningTrinketTasks = new ConcurrentHashMap<>();

    public TrinketScheduler(MythicInventories plugin) {
        this.plugin = plugin;
    }

    public void startTrinketSkillTask(Player player, int slot, TrinketConfig config, ItemStack trinket, MythicInventory inventory) {
        stopTrinketSkillTask(player, slot);

        if (!plugin.isMythicMobsEnabled()) return;

        int interval;
        try {
            interval = Integer.parseInt(config.interval());
        }
        catch (NumberFormatException e) {
            plugin.getLogger().severe("Invalid interval '" + config.interval() + "' for trinket skill '" + config.skill() + "'. Task cancelled.");
            return;
        }

        String keyStr = config.skill();
        UUID skillUUID = UUID.nameUUIDFromBytes(keyStr.getBytes(StandardCharsets.UTF_8));
        Skill skill = plugin.getFromTrinketSkillCache(skillUUID);
        if (skill == null) {
            plugin.getLogger().severe("Could not start trinket task for " + config.skill() + ": skill is invalid or not found.");
            return;
        }
        TrinketSkillTask runnable = new TrinketSkillTask(plugin, player, slot, config, skill, trinket, inventory);

        BukkitTask bukkitTask = runnable.runTaskTimer(plugin, 1L, interval);

        runningTrinketTasks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(slot, bukkitTask);
    }

    public void stopTrinketSkillTask(Player player, int slot) {
        UUID playerID = player.getUniqueId();

        runningTrinketTasks.computeIfPresent(playerID, (uuid, slotMap) -> {
            BukkitTask task = slotMap.remove(slot);
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
            return slotMap.isEmpty() ? null : slotMap;
        });
    }

    public void stopAllTrinketSkillTasks(Player player) {
        UUID playerID = player.getUniqueId();

        HashMap<Integer, BukkitTask> slotMap = runningTrinketTasks.remove(playerID);

        if (slotMap != null) {
            slotMap.values().forEach(task -> {
                if (!task.isCancelled()) {
                    task.cancel();
                }
            });
            plugin.getLogger().info("Stopped all running trinket skill tasks for " + player.getName());
        }
    }

    public void stopAllTrinketSkillTasks() {
        this.runningTrinketTasks.clear();
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }
}
