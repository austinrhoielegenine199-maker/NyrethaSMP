package com.nyretha.home.Command;

import com.nyretha.home.Home;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class HomeCommand implements CommandExecutor, TabCompleter {
    private final Home plugin;
    public HomeCommand(Home plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return plugin.onCommand(sender, command, label, args);
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) return Collections.emptyList();
        List<String> choices = new ArrayList<>();
        choices.add("home");
        return choices.stream().filter(s -> s.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))).toList();
    }
}
