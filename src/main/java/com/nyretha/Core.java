package com.nyretha;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Single Bukkit entry point for NyrethaSMP. */
public final class Core extends JavaPlugin {
    private static Core instance;
    private YamlConfiguration homes;
    private YamlConfiguration flakes;
    private File homesFile;
    private File flakesFile;

    @Override public void onEnable() {
        instance = this;
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create the plugin data folder.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        homesFile = new File(getDataFolder(), "homes.yml");
        flakesFile = new File(getDataFolder(), "flakes.yml");
        homes = YamlConfiguration.loadConfiguration(homesFile);
        flakes = YamlConfiguration.loadConfiguration(flakesFile);
        registerCommand("home");
        registerCommand("sethome");
        registerCommand("delhome");
        registerCommand("flakes");
        getLogger().info("NyrethaSMP Core enabled.");
    }

    private void registerCommand(String name) {
        var command = getCommand(name);
        if (command != null) command.setExecutor(this::handleCommand);
        else getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
    }

    private boolean handleCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("flakes")) return handleFlakes(sender, args);
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        String homeName = args.length == 0 ? "home" : args[0].toLowerCase(Locale.ROOT);
        String key = player.getUniqueId() + "." + homeName;
        if (name.equals("sethome")) {
            homes.set(key, player.getLocation());
            save(homes, homesFile, "homes");
            player.sendMessage(ChatColor.GREEN + "Home '" + homeName + "' saved.");
        } else if (name.equals("delhome")) {
            if (!homes.contains(key)) player.sendMessage(ChatColor.RED + "That home does not exist.");
            else {
                homes.set(key, null);
                save(homes, homesFile, "homes");
                player.sendMessage(ChatColor.GREEN + "Home '" + homeName + "' deleted.");
            }
        } else {
            Location destination = homes.getLocation(key);
            if (destination == null || destination.getWorld() == null) player.sendMessage(ChatColor.RED + "That home does not exist.");
            else {
                player.teleport(destination);
                player.sendMessage(ChatColor.GREEN + "Teleported to '" + homeName + "'.");
            }
        }
        return true;
    }

    private boolean handleFlakes(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        String key = player.getUniqueId().toString();
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            sender.sendMessage(ChatColor.of("#009bff") + "Flakes: " + flakes.getInt(key, 0));
            return true;
        }
        if (args[0].equalsIgnoreCase("take") && args.length == 3 && sender.hasPermission("flakes.admin")) {
            Player target = getServer().getPlayerExact(args[1]);
            int amount;
            try { amount = Integer.parseInt(args[2]); }
            catch (NumberFormatException ex) { sender.sendMessage(ChatColor.RED + "Amount must be a whole number."); return true; }
            if (target == null || amount < 1) {
                sender.sendMessage(ChatColor.RED + "Player or amount is invalid.");
                return true;
            }
            String targetKey = target.getUniqueId().toString();
            int balance = flakes.getInt(targetKey, 0);
            if (amount > balance) {
                sender.sendMessage(ChatColor.RED + "That player does not have enough Flakes.");
                return true;
            }
            flakes.set(targetKey, balance - amount);
            save(flakes, flakesFile, "Flakes");
            sender.sendMessage(ChatColor.of("#009bff") + "Removed " + amount + " Flakes from " + target.getName() + ".");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Usage: /flakes [balance] or /flakes take <player> <amount>");
        return true;
    }

    private void save(YamlConfiguration configuration, File file, String label) {
        try { configuration.save(file); }
        catch (IOException ex) { getLogger().severe("Could not save " + label + ": " + ex.getMessage()); }
    }

    @Override public void onDisable() {
        if (instance == this) instance = null;
    }
    public static Core getInstance() { return instance; }
}
