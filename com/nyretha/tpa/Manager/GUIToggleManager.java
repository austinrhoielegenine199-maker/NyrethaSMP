package com.nyretha.tpa.Manager;

import com.nyretha.tpa.TPA;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public class GUIToggleManager {
   private final TPA plugin;
   private final Set<UUID> guiModeDisabledPlayers = new HashSet<>();
   private final Set<UUID> guiModeTpaHereDisabledPlayers = new HashSet<>();
   public GUIToggleManager(TPA plugin) { this.plugin = plugin; }

   public void loadGuiModes() {
      this.guiModeDisabledPlayers.clear();
      this.guiModeTpaHereDisabledPlayers.clear();
      for (String uuidString : this.plugin.getDataManager().getGuiModeList(false)) {
         try { this.guiModeDisabledPlayers.add(UUID.fromString(uuidString)); } catch (IllegalArgumentException ignored) { }
      }
      for (String uuidString : this.plugin.getDataManager().getGuiModeList(true)) {
         try { this.guiModeTpaHereDisabledPlayers.add(UUID.fromString(uuidString)); } catch (IllegalArgumentException ignored) { }
      }
   }
   public boolean isGuiModeEnabled(Player player, boolean isTpaHere) {
      UUID id = player.getUniqueId();
      return isTpaHere ? !this.guiModeTpaHereDisabledPlayers.contains(id) : !this.guiModeDisabledPlayers.contains(id);
   }
   public void toggleGuiMode(Player player, boolean isTpaHere) {
      UUID id = player.getUniqueId();
      if (isTpaHere) {
         if (!this.guiModeTpaHereDisabledPlayers.remove(id)) {
            this.guiModeTpaHereDisabledPlayers.add(id);
            player.sendMessage(this.plugin.getLanguageManager().getMessage("tpahere-gui-disabled"));
         } else player.sendMessage(this.plugin.getLanguageManager().getMessage("tpahere-gui-enabled"));
      } else {
         if (!this.guiModeDisabledPlayers.remove(id)) {
            this.guiModeDisabledPlayers.add(id);
            player.sendMessage(this.plugin.getLanguageManager().getMessage("tpa-gui-disabled"));
         } else player.sendMessage(this.plugin.getLanguageManager().getMessage("tpa-gui-enabled"));
      }
      this.saveGuiModes(id);
   }
   private void saveGuiModes(UUID id) {
      this.plugin.getDataManager().updateGuiModeStatus(id, this.guiModeDisabledPlayers.contains(id), this.guiModeTpaHereDisabledPlayers.contains(id));
   }
   public void removePlayer(UUID id) {
      this.guiModeDisabledPlayers.remove(id);
      this.guiModeTpaHereDisabledPlayers.remove(id);
      this.plugin.getDataManager().updateGuiModeStatus(id, false, false);
   }
   public Set<UUID> getGuiModePlayers() { return this.guiModeDisabledPlayers; }
   public Set<UUID> getGuiModeTpaHerePlayers() { return this.guiModeTpaHereDisabledPlayers; }
}