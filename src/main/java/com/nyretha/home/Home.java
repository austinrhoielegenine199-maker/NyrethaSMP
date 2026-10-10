package com.nyretha.home;

import com.nyretha.home.Command.HomeCommand;
import com.nyretha.home.Manager.DataManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Legacy Home module. The unified Core remains the sole plugin.yml entry point. */
public final class Home extends JavaPlugin {
    private static Home instance;
    private DataManager dataManager;

    @Override public void onEnable() {
        instance = this;
        dataManager = new DataManager(this);
        HomeCommand executor = new HomeCommand(this);
        for (String name : new String[]{"home", "sethome", "delhome"}) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(executor);
                command.setTabCompleter(executor);
            }
        }
    }

    @Override public void onDisable() { if (instance == this) instance = null; }

    public static Home getInstance() { return instance; }
    public DataManager getDataManager() { return dataManager; }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        String name = command.getName().toLowerCase(java.util.Locale.ROOT);
        if (name.equals("sethome")) {
            String homeName = args.length == 0 ? "home" : args[0];
            dataManager.setHome(player, homeName, player.getLocation());
            player.sendMessage("§aHome '" + homeName + "' saved.");
            return true;
        }
        if (name.equals("delhome")) {
            String homeName = args.length == 0 ? "home" : args[0];
            if (!dataManager.hasHome(player, homeName)) player.sendMessage("§cThat home does not exist.");
            else {
                dataManager.deleteHome(player, homeName);
                player.sendMessage("§aHome '" + homeName + "' deleted.");
            }
            return true;
        }
        String homeName = args.length == 0 ? "home" : args[0];
        Location location = dataManager.getHome(player, homeName);
        if (location == null || location.getWorld() == null) player.sendMessage("§cThat home does not exist.");
        else {
            player.teleport(location);
            player.sendMessage("§aTeleported to '" + homeName + "'.");
        }
        return true;
    }
}
