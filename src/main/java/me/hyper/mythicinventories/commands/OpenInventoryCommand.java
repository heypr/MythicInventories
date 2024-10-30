package me.hyper.mythicinventories.commands;

import me.hyper.mythicinventories.MythicInventories;
import me.hyper.mythicinventories.inventories.MythicInventory;
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
            player.sendMessage("Usage: /inventory <name>");
            return true;
        }

        String inventoryName = args[0];

        if (!plugin.getInventories().containsKey(inventoryName)) {
            player.sendMessage("No such inventory: " + inventoryName);
            return true;
        }

        MythicInventory trueInventory = new MythicInventory(plugin.getInventories().get(inventoryName));
        player.openInventory(trueInventory.getInventory());

        return true;
    }
}
