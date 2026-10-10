package com.nyretha.shop.utils;

import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SoundManager {
   private final JavaPlugin plugin;
   private String clickSound;
   private String declineSound;
   private String buySound;
   private String openSound;
   private String addItemSound;
   private String failedSound;

   public SoundManager(JavaPlugin plugin) { this.plugin = plugin; this.loadSounds(); }

   public void loadSounds() {
      try {
         File configFile = new File(this.plugin.getDataFolder(), "config.yml");
         YamlConfiguration config;
         if (configFile.exists() && (config = YamlConfiguration.loadConfiguration(configFile)).contains("sounds")) {
            this.clickSound = this.getSoundName(config.getString("sounds.click-sound"), "ui.button.click");
            this.declineSound = this.getSoundName(config.getString("sounds.decline-sound"), "entity.villager.no");
            this.buySound = this.getSoundName(config.getString("sounds.buy-sound"), "entity.experience_orb.pickup");
            this.openSound = this.getSoundName(config.getString("sounds.open-sound"), (String)null);
            this.addItemSound = this.getSoundName(config.getString("sounds.add-item-sound"), "entity.experience_orb.pickup");
            this.failedSound = this.getSoundName(config.getString("sounds.failed-sound"), "entity.villager.no");
            return;
         }
         this.setDefaultSounds();
      } catch (Exception var3) { this.setDefaultSounds(); }
   }

   private String getSoundName(String name, String defaultName) { return name != null && !name.isEmpty() ? name : defaultName; }
   private void setDefaultSounds() {
      this.clickSound = "ui.button.click";
      this.declineSound = "entity.villager.no";
      this.buySound = "entity.experience_orb.pickup";
      this.openSound = null;
      this.addItemSound = "entity.experience_orb.pickup";
      this.failedSound = "entity.villager.no";
   }
   public void playClickSound(Player player) { this.playSound(player, this.clickSound); }
   public void playDeclineSound(Player player) { this.playSound(player, this.declineSound); }
   public void playBuySound(Player player) { this.playSound(player, this.buySound); }
   public void playOpenSound(Player player) { this.playSound(player, this.openSound); }
   public void playAddItemSound(Player player) { this.playSound(player, this.addItemSound); }
   public void playFailedSound(Player player) { this.playSound(player, this.failedSound); }

   private void playSound(Player player, String soundName) {
      if (soundName != null && !soundName.isEmpty() && player != null && player.isOnline()) {
         try { player.playSound(player.getLocation(), soundName, 1.0F, 1.0F); } catch (Exception var4) { }
      }
   }
   public String getClickSound() { return this.clickSound; }
   public String getDeclineSound() { return this.declineSound; }
   public String getBuySound() { return this.buySound; }
   public String getOpenSound() { return this.openSound; }
   public String getAddItemSound() { return this.addItemSound; }
   public String getFailedSound() { return this.failedSound; }
   public void reload() { this.loadSounds(); }
}