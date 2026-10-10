package com.nyretha.tpa.Manager;

import com.nyretha.tpa.TPA;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public class DataManager {
   private final TPA plugin;
   private final File dataFile;
   private YamlConfiguration data;

   public DataManager(TPA plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
      this.loadData();
   }
   private void loadData() {
      if (!this.dataFile.exists()) { this.data = new YamlConfiguration(); this.saveData(); }
      else this.data = YamlConfiguration.loadConfiguration(this.dataFile);
   }
   private void saveData() { try { this.data.save(this.dataFile); } catch (Exception ignored) { } }

   public void loadToggleStatus(Set<UUID> disabledPlayers, Set<UUID> disabledHerePlayers) {
      disabledPlayers.clear(); disabledHerePlayers.clear();
      for (String s : this.data.getStringList("tpa-disabled")) try { disabledPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
      for (String s : this.data.getStringList("tpahere-disabled")) try { disabledHerePlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
   }
   public void saveToggleStatus(UUID id, boolean tpaDisabled, boolean hereDisabled) { updateTPAStatus(id,tpaDisabled); updateTPAHereStatus(id,hereDisabled); }
   public void updateTPAStatus(UUID id, boolean disabled) { updateList("tpa-disabled",id,disabled); }
   public void updateTPAHereStatus(UUID id, boolean disabled) { updateList("tpahere-disabled",id,disabled); }
   private void updateList(String key, UUID id, boolean enabled) {
      List<String> list = this.data.getStringList(key);
      if (enabled) { if (!list.contains(id.toString())) list.add(id.toString()); } else list.remove(id.toString());
      this.data.set(key,list); this.saveData();
   }
   public void loadGuiModeStatus(Set<UUID> guiPlayers, Set<UUID> hereGuiPlayers) {
      guiPlayers.clear(); hereGuiPlayers.clear();
      for (String s : this.data.getStringList("tpa-gui-mode")) try { guiPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
      for (String s : this.data.getStringList("tpahere-gui-mode")) try { hereGuiPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
   }
   public void updateGuiModeStatus(UUID id, boolean tpaGui, boolean hereGui) {
      updateList("tpa-gui-mode",id,tpaGui); updateList("tpahere-gui-mode",id,hereGui);
   }
   public List<String> getGuiModeList(boolean isHere) { return this.data.getStringList(isHere ? "tpahere-gui-mode" : "tpa-gui-mode"); }
   public boolean isGuiModeEnabled(UUID id, boolean isHere) { return getGuiModeList(isHere).contains(id.toString()); }
   public List<String> getAutoAcceptList() { return this.data.getStringList("tpa-auto"); }
   public void updateAutoAcceptStatus(UUID id, boolean auto) { updateList("tpa-auto",id,auto); }
   public boolean isAutoAcceptEnabled(UUID id) { return getAutoAcceptList().contains(id.toString()); }
   public List<String> getAutoAcceptHereList() { return this.data.getStringList("tpahere-auto"); }
   public void updateAutoAcceptHereStatus(UUID id, boolean auto) { updateList("tpahere-auto",id,auto); }
   public boolean isAutoAcceptHereEnabled(UUID id) { return getAutoAcceptHereList().contains(id.toString()); }
   public void removePlayer(UUID id) {
      updateTPAStatus(id,false); updateTPAHereStatus(id,false); updateGuiModeStatus(id,false,false);
      updateAutoAcceptStatus(id,false); updateAutoAcceptHereStatus(id,false);
   }
   public boolean isTPADisabled(UUID id) { return this.data.getStringList("tpa-disabled").contains(id.toString()); }
   public boolean isTPAHereDisabled(UUID id) { return this.data.getStringList("tpahere-disabled").contains(id.toString()); }
   public void closeConnection() { }
}