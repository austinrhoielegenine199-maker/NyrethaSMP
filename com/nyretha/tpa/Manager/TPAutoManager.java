package com.nyretha.tpa.Manager;

import com.nyretha.tpa.TPA;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public class TPAutoManager {
   private final TPA plugin;
   private final Set<UUID> autoAcceptPlayers = new HashSet<>();
   private final Set<UUID> autoAcceptHerePlayers = new HashSet<>();
   public TPAutoManager(TPA plugin) { this.plugin = plugin; }

   public void loadAutoAcceptStatus() {
      this.autoAcceptPlayers.clear();
      this.autoAcceptHerePlayers.clear();
      for (String s : this.plugin.getDataManager().getAutoAcceptList()) {
         try { this.autoAcceptPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
      }
      for (String s : this.plugin.getDataManager().getAutoAcceptHereList()) {
         try { this.autoAcceptHerePlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
      }
   }
   public boolean isAutoAcceptEnabled(Player player) { return this.autoAcceptPlayers.contains(player.getUniqueId()); }
   public void toggleAutoAccept(Player player) {
      UUID id = player.getUniqueId();
      if (this.autoAcceptPlayers.remove(id)) player.sendMessage(this.plugin.getLanguageManager().getMessage("tpa-auto-disabled"));
      else { this.autoAcceptPlayers.add(id); player.sendMessage(this.plugin.getLanguageManager().getMessage("tpa-auto-enabled")); }
      this.plugin.getDataManager().updateAutoAcceptStatus(id, this.autoAcceptPlayers.contains(id));
   }
   public boolean isAutoAcceptHereEnabled(Player player) { return this.autoAcceptHerePlayers.contains(player.getUniqueId()); }
   public void toggleAutoAcceptHere(Player player) {
      UUID id = player.getUniqueId();
      if (this.autoAcceptHerePlayers.remove(id)) player.sendMessage(this.plugin.getLanguageManager().getMessage("tpahere-auto-disabled"));
      else { this.autoAcceptHerePlayers.add(id); player.sendMessage(this.plugin.getLanguageManager().getMessage("tpahere-auto-enabled")); }
      this.plugin.getDataManager().updateAutoAcceptHereStatus(id, this.autoAcceptHerePlayers.contains(id));
   }
   public void removePlayer(UUID id) {
      this.autoAcceptPlayers.remove(id);
      this.autoAcceptHerePlayers.remove(id);
      this.plugin.getDataManager().updateAutoAcceptStatus(id, false);
      this.plugin.getDataManager().updateAutoAcceptHereStatus(id, false);
   }
   public Set<UUID> getAutoAcceptPlayers() { return this.autoAcceptPlayers; }
   public Set<UUID> getAutoAcceptHerePlayers() { return this.autoAcceptHerePlayers; }
}