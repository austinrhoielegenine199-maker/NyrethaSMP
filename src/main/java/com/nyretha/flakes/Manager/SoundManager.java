package com.nyretha.flakes.Manager;

import com.nyretha.flakes.GemsPlugin;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SoundManager {
   private final GemsPlugin plugin;
   private final FileConfiguration config;
   private final Map<String, Sound> soundCache = new HashMap();

   public SoundManager(GemsPlugin plugin) {
      this.plugin = plugin;
      this.config = plugin.getConfig();
      this.initializeSoundCache();
   }
   private void initializeSoundCache() {
      this.cacheSound("ui.button.click", Sound.UI_BUTTON_CLICK);
      this.cacheSound("entity.player.levelup", Sound.ENTITY_PLAYER_LEVELUP);
      this.cacheSound("entity.experience_orb.pickup", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
      this.cacheSound("block.note_block.pling", Sound.BLOCK_NOTE_BLOCK_PLING);
      this.cacheSound("entity.villager.yes", Sound.ENTITY_VILLAGER_YES);
      this.cacheSound("entity.villager.no", Sound.ENTITY_VILLAGER_NO);
      this.cacheSound("block.anvil.land", Sound.BLOCK_ANVIL_LAND);
      this.cacheSound("entity.item.pickup", Sound.ENTITY_ITEM_PICKUP);
      this.cacheSound("block.note_block.bell", Sound.BLOCK_NOTE_BLOCK_BELL);
      this.cacheSound("ui.toast.in", Sound.UI_TOAST_IN);
      this.cacheSound("ui.toast.out", Sound.UI_TOAST_OUT);
   }
   private void cacheSound(String key, Sound sound) { this.soundCache.put(key.toLowerCase(), sound); }
   public void playSound(Player player, String configPath) {
      if (player != null) this.plugin.runForEntity(player, () -> {
         String key = this.config.getString("Sounds." + configPath, "");
         if (key != null && !key.isEmpty() && !key.equalsIgnoreCase("none")) {
            Sound sound = this.getSoundByKey(key);
            if (sound != null) player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
         }
      });
   }
   private Sound getSoundByKey(String key) {
      String normalized = key.toLowerCase().replace("_", ".");
      Sound cached = this.soundCache.get(normalized);
      if (cached != null) return cached;
      try { return this.findSoundByKey(normalized); } catch (Exception ignored) { return null; }
   }
   private Sound findSoundByKey(String key) {
      switch (key) {
         case "ui.button.click": return Sound.UI_BUTTON_CLICK;
         case "entity.player.levelup": return Sound.ENTITY_PLAYER_LEVELUP;
         case "entity.experience_orb.pickup": return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
         case "block.note_block.pling": return Sound.BLOCK_NOTE_BLOCK_PLING;
         case "entity.villager.yes": return Sound.ENTITY_VILLAGER_YES;
         case "entity.villager.no": return Sound.ENTITY_VILLAGER_NO;
         case "block.anvil.land": return Sound.BLOCK_ANVIL_LAND;
         case "entity.item.pickup": return Sound.ENTITY_ITEM_PICKUP;
         case "block.note_block.bell": return Sound.BLOCK_NOTE_BLOCK_BELL;
         case "ui.toast.in": return Sound.UI_TOAST_IN;
         case "ui.toast.out": return Sound.UI_TOAST_OUT;
         default: return this.getFallbackSound(key);
      }
   }
   private Sound getFallbackSound(String key) { return null; }
   public void playClickSound(Player player) { this.playSound(player, "click-sound"); }
   public void playReloadSound(Player player) { this.playSound(player, "plugin-reloaded"); }
   public void playCustomSound(Player player, String key) {
      if (player != null) this.plugin.runForEntity(player, () -> {
         Sound sound = this.getSoundByKey(key);
         if (sound != null) player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
      });
   }
   public boolean isSoundEnabled(String path) {
      String key = this.config.getString("Sounds." + path, "");
      return key != null && !key.isEmpty() && !key.equalsIgnoreCase("none");
   }
   public void reload() { this.soundCache.clear(); this.initializeSoundCache(); }
   public List<String> getAvailableSoundKeys() {
      return Arrays.asList("ui.button.click", "entity.player.levelup", "entity.experience_orb.pickup", "block.note_block.pling", "entity.villager.yes", "entity.villager.no", "block.anvil.land", "entity.item.pickup", "block.note_block.bell", "ui.toast.in", "ui.toast.out");
   }
}