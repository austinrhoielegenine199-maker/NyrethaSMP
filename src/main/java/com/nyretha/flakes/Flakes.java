package com.nyretha.flakes;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Flakes currency module. Currency balances are stored by player UUID. */
public final class Flakes extends JavaPlugin {
    private File balancesFile;
    private YamlConfiguration balances;

    @Override public void onEnable() {
        balancesFile = new File(getDataFolder(), "balances.yml");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        balances = YamlConfiguration.loadConfiguration(balancesFile);
        getCommand("flakes");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        String key = player.getUniqueId().toString();
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            sender.sendMessage(ChatColor.of("#009bff") + "Flakes: " + balances.getInt(key, 0));
            return true;
        }
        if (args[0].equalsIgnoreCase("take") && args.length >= 3 && sender.hasPermission("flakes.admin")) {
            Player target = getServer().getPlayerExact(args[1]);
            int amount;
            try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { sender.sendMessage("Amount must be a whole number."); return true; }
            if (target == null || amount < 1) { sender.sendMessage("Player or amount is invalid."); return true; }
            String targetKey = target.getUniqueId().toString();
            int current = balances.getInt(targetKey, 0);
            if (amount > current) { sender.sendMessage("That player does not have enough Flakes."); return true; }
            balances.set(targetKey, current - amount);
            saveBalances();
            sender.sendMessage("Removed " + amount + " Flakes from " + target.getName() + ".");
            return true;
        }
        sender.sendMessage("Usage: /flakes [balance] or /flakes take <player> <amount>");
        return true;
    }

    private void saveBalances() {
        try { balances.save(balancesFile); }
        catch (IOException ex) { getLogger().severe("Could not save Flakes balances: " + ex.getMessage()); }
    }
}
