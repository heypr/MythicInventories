package dev.heypr.mythicinventories.commands;

import dev.heypr.mythicinventories.MythicInventories;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MigrateOldDataCommand implements CommandExecutor {

    private final MythicInventories plugin;

    public MigrateOldDataCommand(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        String prefix = "[MythicInventories] ";
        if (args.length == 0) {
            player.sendMessage(prefix + "This command will migrate old save data to the new format. This is irreversible!");
            player.sendMessage(prefix + "An error will appear if you attempt to convert modern player data, and may even (albeit unlikely) corrupt it.");
            player.sendMessage(prefix + "PLEASE MAKE A BACKUP OF THE FILES IN THE 'playerdata' FOLDER BEFORE CONTINUING.");
            player.sendMessage(prefix + "TYPE /migrateolddata confirm TO CONFIRM.");
            plugin.addPlayer(player.getUniqueId());

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.removePlayer(player.getUniqueId());
            }, 20L * 10);
            return true;
        }

        if (args[0].equalsIgnoreCase("confirm")) {
            if (!plugin.getConfirmationList().contains(player.getUniqueId())) {
                player.sendMessage(prefix + "You have not confirmed the migration. Please type /migrateolddata to confirm.");
                return true;
            }
            plugin.removePlayer(player.getUniqueId());
            plugin.getOldDataConverter().convertData();
            player.sendMessage(prefix + "Old data has been successfully converted!");
            return true;
        }

        return true;
    }
}
