package com.nyretha.tpa.Manager;

import com.nyretha.tpa.TPA;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public class GUIManager {
   private final TPA plugin;
   private final Map<String, YamlConfiguration> guiConfigs = new HashMap<>();
   public GUIManager(TPA plugin) { this.plugin = plugin; }

   public void loadGUIConfigs() {
      File folder = new File(this.plugin.getDataFolder(), "gui");
      if (!folder.exists()) folder.mkdirs();
      for (String path : new String[]{"gui/tpa-accept.yml","gui/tpa-here-accept.yml","gui/tpa-send.yml","gui/tpa-here-send.yml"}) createDefaultGUI(path);
      File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
      if (files != null) for (File file : files) {
         String name = file.getName().replace(".yml","");
         this.guiConfigs.put(convertToInternalName(name), YamlConfiguration.loadConfiguration(file));
      }
   }
   private void createDefaultGUI(String path) {
      File target = new File(this.plugin.getDataFolder(), path);
      if (target.exists()) return;
      YamlConfiguration c = new YamlConfiguration();
      c.set("rows",3);
      c.set("name", path.contains("here") ? "&8ᴛᴘᴀ ʜᴇʀᴇ" : "&8ᴛᴘᴀ");
      String[] icons = {"cancel-icon","location-icon","player-icon","confirm-icon","fly-icon"};
      int[] slots = {10,12,13,16,14};
      String[] mats = {"RED_STAINED_GLASS_PANE","GRASS_BLOCK","PLAYER_HEAD","LIME_STAINED_GLASS_PANE","FEATHER"};
      String[] names = {"&#FF0000ᴄᴀɴᴄᴇʟ","&#00f986ʟᴏᴄᴀᴛɪᴏɴ","&#00f986ᴘʟᴀʏᴇʀ","&#00FF00ᴄᴏɴғɪʀᴍ","&#00f986ғʟʏɪɴɢ"};
      for (int i=0;i<icons.length;i++) {
         c.set("icons."+icons[i]+".slot",slots[i]);
         c.set("icons."+icons[i]+".material",mats[i]);
         c.set("icons."+icons[i]+".display-name",names[i]);
      }
      c.set("icons.location-icon.lore",List.of("&7%world%"));
      c.set("icons.player-icon.lore",List.of("&7%player%"));
      c.set("icons.fly-icon.lore",List.of("&7%is_flying%"));
      c.set("icons.cancel-icon.lore",List.of("&fClick to cancel or decline"));
      c.set("icons.confirm-icon.lore",List.of("&fClick to confirm"));
      try { c.save(target); } catch (Exception e) { this.plugin.getLogger().warning("Could not save "+path+": "+e.getMessage()); }
   }
   private String convertToInternalName(String name) {
      return switch (name) {
         case "tpa-accept" -> "tpa_accept_gui";
         case "tpa-here-accept" -> "tpa_here_accept_gui";
         case "tpa-send" -> "tpa_send_gui";
         case "tpa-here-send" -> "tpa_here_send_gui";
         default -> name;
      };
   }
   public void reloadGUIConfigs() { this.guiConfigs.clear(); loadGUIConfigs(); }
   public YamlConfiguration getGUIConfig(String name) { return this.guiConfigs.get(name); }
   public String getGUIName(String name,String fallback) { YamlConfiguration c=this.guiConfigs.get(name); return c==null?fallback:c.getString("name",fallback); }
   public int getGUIRows(String name,int fallback) { YamlConfiguration c=this.guiConfigs.get(name); return c==null?fallback:c.getInt("rows",fallback); }
   public int getIconSlot(String gui,String icon,int fallback) { YamlConfiguration c=this.guiConfigs.get(gui); return c==null?fallback:c.getInt("icons."+icon+".slot",fallback); }
   public String getIconMaterial(String gui,String icon,String fallback) { YamlConfiguration c=this.guiConfigs.get(gui); return c==null?fallback:c.getString("icons."+icon+".material",fallback); }
   public String getIconDisplayName(String gui,String icon,String fallback) { YamlConfiguration c=this.guiConfigs.get(gui); return c==null?fallback:c.getString("icons."+icon+".display-name",fallback); }
   public List<String> getIconLore(String gui,String icon) { YamlConfiguration c=this.guiConfigs.get(gui); return c==null?Collections.emptyList():c.getStringList("icons."+icon+".lore"); }
}