package com.nyretha.homes;

import com.nyretha.homes.integration.TeamHomeButtonListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Nyretha Homes entry point. The existing Homes module can add its home-slot
 * manager here; this entry point registers the Team Home integration.
 */
public final class Home extends JavaPlugin {
    private TeamHomeButtonListener teamHomeButtonListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        teamHomeButtonListener = new TeamHomeButtonListener(this);
        teamHomeButtonListener.register();
        getLogger().info("NyrethaHomes enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("home") || command.getName().equalsIgnoreCase("homes")) {
            Inventory gui = getServer().createInventory(null, 36, ChatColor.translateAlternateColorCodes('&', "&8ʜᴏᴍᴇꜱ"));
            player.openInventory(gui);
            return true;
        }
        return false;
    }
}
