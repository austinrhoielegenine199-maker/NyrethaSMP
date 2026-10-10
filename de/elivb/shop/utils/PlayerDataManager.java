package de.elivb.shop.utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerDataManager {
   private final JavaPlugin plugin;
   private final File playerDataFolder;
   private final SimpleDateFormat dateFormat;

   public PlayerDataManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.playerDataFolder = new File(plugin.getDataFolder(), "players");
      this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
      if (!this.playerDataFolder.exists()) this.playerDataFolder.mkdirs();
   }

   public void logPurchase(Player player, String itemName, int amount, double price) {
      try {
         UUID uuid = player.getUniqueId();
         String playerName = player.getName();
         File playerFile = this.findPlayerFile(uuid, playerName);
         FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
         if (!config.contains("uuid")) config.set("uuid", uuid.toString());
         config.set("name", playerName);
         double currentTotalBuy = config.getDouble("totalbuy", 0.0);
         int currentTotalItems = config.getInt("totalItems", 0);
         config.set("totalbuy", currentTotalBuy + price);
         config.set("totalItems", currentTotalItems + amount);
         Map<String, Object> logs = config.getConfigurationSection("logs") != null ? config.getConfigurationSection("logs").getValues(false) : new HashMap();
         String timestampKey = String.valueOf(System.currentTimeMillis());
         Map<String, Object> logEntry = new HashMap();
         logEntry.put("timestamp", this.dateFormat.format(new Date()));
         logEntry.put("items", amount);
         logEntry.put("price", this.formatPrice(price));
         logEntry.put("itemsbuy", itemName + ":" + amount);
         logs.put(timestampKey, logEntry);
         config.set("logs", logs);
         config.save(playerFile);
      } catch (IOException ignored) { }
   }

   private File findPlayerFile(UUID uuid, String playerName) {
      File nameFile = new File(this.playerDataFolder, playerName + ".yml");
      if (nameFile.exists()) {
         FileConfiguration config = YamlConfiguration.loadConfiguration(nameFile);
         String storedUUID = config.getString("uuid");
         return storedUUID != null && storedUUID.equals(uuid.toString()) ? nameFile : this.createNewPlayerFile(uuid, playerName);
      }
      for(File file : this.playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"))) {
         FileConfiguration config = YamlConfiguration.loadConfiguration(file);
         String storedUUID = config.getString("uuid");
         if (storedUUID != null && storedUUID.equals(uuid.toString())) {
            File newFile = new File(this.playerDataFolder, playerName + ".yml");
            if (newFile.exists()) newFile = new File(this.playerDataFolder, playerName + "_" + uuid.toString().substring(0, 8) + ".yml");
            file.renameTo(newFile);
            return newFile;
         }
      }
      return this.createNewPlayerFile(uuid, playerName);
   }

   private File createNewPlayerFile(UUID uuid, String playerName) {
      File playerFile = new File(this.playerDataFolder, playerName + ".yml");
      if (playerFile.exists()) playerFile = new File(this.playerDataFolder, playerName + "_" + uuid.toString().substring(0, 8) + ".yml");
      return playerFile;
   }

   private String formatPrice(double price) {
      if (price >= 1.0E9) return String.format("%.1fB", price / 1.0E9);
      if (price >= 1000000.0) return String.format("%.1fM", price / 1000000.0);
      return price >= 1000.0 ? String.format("%.1fk", price / 1000.0) : String.format("%.0f", price);
   }

   public double getTotalSpent(UUID uuid) {
      File file = this.findPlayerFileByUUID(uuid);
      return file != null && file.exists() ? YamlConfiguration.loadConfiguration(file).getDouble("totalbuy", 0.0) : 0.0;
   }
   public int getTotalItemsBought(UUID uuid) {
      File file = this.findPlayerFileByUUID(uuid);
      return file != null && file.exists() ? YamlConfiguration.loadConfiguration(file).getInt("totalItems", 0) : 0;
   }
   public Map<String, Object> getPurchaseLogs(UUID uuid) {
      File file = this.findPlayerFileByUUID(uuid);
      if (file != null && file.exists()) {
         FileConfiguration config = YamlConfiguration.loadConfiguration(file);
         if (config.getConfigurationSection("logs") != null) return config.getConfigurationSection("logs").getValues(false);
      }
      return new HashMap();
   }
   private File findPlayerFileByUUID(UUID uuid) {
      if (this.playerDataFolder.exists() && this.playerDataFolder.isDirectory()) {
         for(File file : this.playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"))) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String storedUUID = config.getString("uuid");
            if (storedUUID != null && storedUUID.equals(uuid.toString())) return file;
         }
      }
      return null;
   }
   public boolean hasPlayerData(UUID uuid) { return this.findPlayerFileByUUID(uuid) != null; }
   public void deletePlayerData(UUID uuid) {
      File file = this.findPlayerFileByUUID(uuid);
      if (file != null && file.exists()) file.delete();
   }
   public File getPlayerFile(UUID uuid) { return this.findPlayerFileByUUID(uuid); }
   public FileConfiguration getPlayerConfig(UUID uuid) {
      File file = this.getPlayerFile(uuid);
      return file != null && file.exists() ? YamlConfiguration.loadConfiguration(file) : null;
   }
   public void cleanupDuplicateFiles() {
      Map<String, File> uuidToFileMap = new HashMap();
      if (this.playerDataFolder.exists() && this.playerDataFolder.isDirectory()) {
         for(File file : this.playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"))) {
            try {
               FileConfiguration config = YamlConfiguration.loadConfiguration(file);
               String storedUUID = config.getString("uuid");
               if (storedUUID != null && !uuidToFileMap.containsKey(storedUUID)) uuidToFileMap.put(storedUUID, file);
            } catch (Exception ignored) { }
         }
      }
   }
}