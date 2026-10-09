package com.nyretha.tpa.Manager;

import com.nyretha.tpa.TPA;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
   private final TPA plugin;
   private FileConfiguration config;
   private int requestTimeout;
   private int teleportDelay;
   private boolean cancelOnMove;

   public ConfigManager(TPA plugin) { this.plugin = plugin; }
   public void loadConfig() {
      this.plugin.saveDefaultConfig();
      this.plugin.reloadConfig();
      this.config = this.plugin.getConfig();
      this.requestTimeout = this.config.getInt("request-timeout", 30);
      this.teleportDelay = this.config.getInt("teleport-delay", 5);
      this.cancelOnMove = this.config.getBoolean("cancel-on-move", true);
   }
   public int getRequestTimeout() { return this.requestTimeout; }
   public int getTeleportDelay() { return this.teleportDelay; }
   public boolean isCancelOnMove() { return this.cancelOnMove; }
   public FileConfiguration getConfig() { return this.config; }
   public String getWorldNickname(String worldName) {
      String nickname = this.config.getString("world-nicknames." + worldName);
      return nickname == null ? worldName : nickname;
   }
}