package dev.heypr.mythicinventories.commands;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OpenInventoryCommand implements CommandExecutor {

    private final MythicInventories plugin;

    public OpenInventoryCommand(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /miopen <name> [player]");
            return true;
        }

        String inventoryName = args[0];
        if (!plugin.getInventories().containsKey(inventoryName)) {
            player.sendMessage("No such inventory: " + inventoryName);
            return true;
        }

        if (!player.hasPermission("mythicinventories.open." + inventoryName)) {
            player.sendMessage("No permission.");
            return true;
        }

        MythicInventory mythicInventory = plugin.getInventories().get(inventoryName);
        Player target = args.length > 1 ? plugin.getServer().getPlayer(args[1]) : player;

        if (target == null) {
            player.sendMessage("User either does not exist or is not online.");
            return true;
        }

        target.openInventory(mythicInventory.getInventory());
        return true;
    }
}
