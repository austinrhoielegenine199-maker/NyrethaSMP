package com.nyretha.home;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command adapter; command behavior remains centralized in Home. */
public final class HomeCommand implements CommandExecutor {
    private final Home plugin;
    public HomeCommand(Home plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return plugin.onCommand(sender, command, label, args);
    }
}
