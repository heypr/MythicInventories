package me.hyper.mythicinventories.commands;

import me.hyper.mythicinventories.MythicInventories;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OpenInventoryTabCompleter implements TabCompleter {

    private final MythicInventories plugin;

    public OpenInventoryTabCompleter(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return plugin.getInventoryNames();
    }
}
