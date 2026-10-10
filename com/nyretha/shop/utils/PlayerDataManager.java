package com.nyretha.shop.utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerDataManager {
    private final File folder;
    public PlayerDataManager(JavaPlugin plugin) {
        folder = new File(plugin.getDataFolder(), "players");
        if (!folder.exists()) folder.mkdirs();
    }
    public void logPurchase(Player player, String itemName, int amount, double price) {
        File file = new File(folder, player.getUniqueId() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("uuid", player.getUniqueId().toString());
        cfg.set("name", player.getName());
        cfg.set("totalbuy", cfg.getDouble("totalbuy") + price);
        cfg.set("totalItems", cfg.getInt("totalItems") + amount);
        String key = "logs." + System.currentTimeMillis();
        cfg.set(key + ".timestamp", System.currentTimeMillis());
        cfg.set(key + ".items", amount);
        cfg.set(key + ".price", price);
        cfg.set(key + ".item", itemName);
        try { cfg.save(file); } catch (IOException ex) { ex.printStackTrace(); }
    }
    public double getTotalSpent(UUID uuid) {
        File file = new File(folder, uuid + ".yml");
        return file.exists() ? YamlConfiguration.loadConfiguration(file).getDouble("totalbuy") : 0.0;
    }
    public int getTotalItemsBought(UUID uuid) {
        File file = new File(folder, uuid + ".yml");
        return file.exists() ? YamlConfiguration.loadConfiguration(file).getInt("totalItems") : 0;
    }
}