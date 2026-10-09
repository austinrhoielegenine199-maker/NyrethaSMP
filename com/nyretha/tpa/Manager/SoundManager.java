package com.nyretha.tpa.Manager;

import com.nyretha.tpa.TPA;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
   private final TPA plugin;
   private Sound teleportCooldownSound, teleportingSound, buttonClickSound, teleportSuccessSound, teleportFailSound, reloadSound;
   public SoundManager(TPA plugin) { this.plugin = plugin; }

   public void loadSounds() {
      this.teleportCooldownSound = getSound(this.plugin.getConfig().getString("sound.teleport-cooldown", "block.note_block.pling"));
      this.teleportingSound = getSound(this.plugin.getConfig().getString("sound.teleporting", "entity.enderman.teleport"));
      this.buttonClickSound = getSound(this.plugin.getConfig().getString("sound.button-click", "ui.button.click"));
      this.teleportSuccessSound = getSound(this.plugin.getConfig().getString("sound.teleport-success", "entity.player.levelup"));
      this.teleportFailSound = getSound(this.plugin.getConfig().getString("sound.teleport-fail", "entity.villager.no"));
      this.reloadSound = getSound(this.plugin.getConfig().getString("sound.reload", "entity.experience_orb.pickup"));
   }
   private Sound getSound(String name) {
      if (name == null || name.isEmpty()) return null;
      try { return Registry.SOUNDS.get(NamespacedKey.minecraft(name)); } catch (Exception ignored) { return null; }
   }
   public void playTeleportCooldownSound(Player p) { playSound(p, teleportCooldownSound); }
   public void playTeleportingSound(Player p) { playSound(p, teleportingSound); }
   public void playButtonClickSound(Player p) { playSound(p, buttonClickSound); }
   public void playTeleportSuccessSound(Player p) { playSound(p, teleportSuccessSound); }
   public void playTeleportFailSound(Player p) { playSound(p, teleportFailSound); }
   public void playReloadSound(Player p) { playSound(p, reloadSound); }
   private void playSound(Player p, Sound s) {
      if (p != null && s != null && p.isOnline()) p.playSound(p.getLocation(), s, 1.0F, 1.0F);
   }
}