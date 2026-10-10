package com.nyretha.flakes;

import com.nyretha.flakes.Utlis.HexUtils;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Flakes currency module. Player balances are persisted in flakes-data.yml.
 * Register this class as the module's JavaPlugin main class and declare the
 * /flakes command in plugin.yml to enable its command handler.
 */
public final class FlakesPlugin extends JavaPlugin {
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data directory.");
        }
        dataFile = new File(getDataFolder(), "flakes-data.yml");
        loadBalances();
        getLogger().info("Flakes currency module enabled.");
    }

    @Override
    public void onDisable() {
        saveBalances();
    }

    private void loadBalances() {
        if (dataFile == null) dataFile = new File(getDataFolder(), "flakes-data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveBalances() {
        if (data == null || dataFile == null) return;
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save Flakes balances: " + exception.getMessage());
        }
    }

    public long getBalance(UUID playerId) {
        return Math.max(0L, data.getLong("balances." + playerId, 0L));
    }

    public boolean deposit(UUID playerId, long amount) {
        if (amount <= 0) return false;
        long current = getBalance(playerId);
        if (Long.MAX_VALUE - current < amount) return false;
        data.set("balances." + playerId, current + amount);
        saveBalances();
        return true;
    }

    public boolean withdraw(UUID playerId, long amount) {
        if (amount <= 0) return false;
        long current = getBalance(playerId);
        if (current < amount) return false;
        data.set("balances." + playerId, current - amount);
        saveBalances();
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("flakes")) return false;
        if (args.length == 0 || args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Usage: /flakes balance <player>");
                return true;
            }
            Player player = (Player) sender;
            sender.sendMessage(HexUtils.colorize("&dYour balance: &f" + getBalance(player.getUniqueId()) + " Flakes"));
            return true;
        }

        if (args[0].equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player) || args.length != 3) {
                sender.sendMessage("Usage: /flakes pay <player> <amount>");
                return true;
            }
            Player payer = (Player) sender;
            Player target = Bukkit.getPlayerExact(args[1]);
            long amount = parsePositiveAmount(args[2]);
            if (target == null) {
                sender.sendMessage(HexUtils.colorize("&cThat player must be online."));
                return true;
            }
            if (target.getUniqueId().equals(payer.getUniqueId())) {
                sender.sendMessage(HexUtils.colorize("&cYou cannot pay yourself."));
                return true;
            }
            if (amount <= 0 || !withdraw(payer.getUniqueId(), amount)) {
                sender.sendMessage(HexUtils.colorize("&cInvalid amount or insufficient Flakes."));
                return true;
            }
            if (!deposit(target.getUniqueId(), amount)) {
                deposit(payer.getUniqueId(), amount);
                sender.sendMessage(HexUtils.colorize("&cCould not complete that payment."));
                return true;
            }
            payer.sendMessage(HexUtils.colorize("&aPaid &f" + amount + " Flakes &ato " + target.getName() + "."));
            target.sendMessage(HexUtils.colorize("&aReceived &f" + amount + " Flakes &afrom " + payer.getName() + "."));
            return true;
        }

        if ((args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")) && sender.hasPermission("flakes.admin")) {
            if (args.length != 3) {
                sender.sendMessage("Usage: /flakes " + args[0] + " <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            long amount = parsePositiveAmount(args[2]);
            if (target == null || amount <= 0) {
                sender.sendMessage(HexUtils.colorize("&cUse an online player and a positive whole number."));
                return true;
            }
            boolean success = args[0].equalsIgnoreCase("give")
                ? deposit(target.getUniqueId(), amount) : withdraw(target.getUniqueId(), amount);
            sender.sendMessage(HexUtils.colorize(success ? "&aFlakes balance updated." : "&cCould not update balance."));
            return true;
        }

        sender.sendMessage(HexUtils.colorize("&d/flakes balance &7- View your balance"));
        sender.sendMessage(HexUtils.colorize("&d/flakes pay <player> <amount> &7- Send Flakes"));
        if (sender.hasPermission("flakes.admin")) {
            sender.sendMessage(HexUtils.colorize("&d/flakes give|take <player> <amount>"));
        }
        return true;
    }

    private long parsePositiveAmount(String raw) {
        try {
            long amount = Long.parseLong(raw);
            return amount > 0 ? amount : -1L;
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }
}
