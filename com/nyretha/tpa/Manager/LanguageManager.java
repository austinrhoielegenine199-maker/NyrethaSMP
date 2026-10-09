package com.nyretha.tpa.Manager;

import com.nyretha.tpa.Hex;
import com.nyretha.tpa.TPA;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageManager {
   private final TPA plugin;
   private FileConfiguration messages;
   private final File messagesFile;
   private boolean prefixEnabled;
   private String prefix;
   public LanguageManager(TPA plugin) {
      this.plugin = plugin;
      this.messagesFile = new File(plugin.getDataFolder(), "lang.yml");
   }
   public void loadMessages() {
      if (!this.messagesFile.exists()) this.plugin.saveResource("lang.yml", false);
      this.messages = YamlConfiguration.loadConfiguration(this.messagesFile);
      try {
         Reader reader = new InputStreamReader(this.plugin.getResource("lang.yml"), StandardCharsets.UTF_8);
         YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
         this.messages.setDefaults(defaults);
         reader.close();
      } catch (Exception ignored) { }
      this.prefixEnabled = this.messages.getBoolean("prefix-enable", true);
      this.prefix = Hex.translateAllColorCodes(this.messages.getString("prefix", "&#FC0000TPA &7»"));
   }
   public String getMessage(String key) {
      String message = this.messages.getString("messages." + key);
      if (message == null) { this.plugin.getLogger().warning("Missing message: " + key); return null; }
      String formatted = Hex.translateAllColorCodes(message);
      return this.prefixEnabled ? this.prefix + " " + formatted : formatted;
   }
   public String getMessageWithoutPrefix(String key) {
      String message = this.messages.getString("messages." + key);
      if (message == null) { this.plugin.getLogger().warning("Missing message: " + key); return null; }
      return Hex.translateAllColorCodes(message);
   }
   public String getActionBar(String key) {
      String message = this.messages.getString("action-bars." + key);
      if (message == null) { this.plugin.getLogger().warning("Missing actionbar: " + key); return null; }
      return Hex.translateAllColorCodes(message);
   }
   public String getCommandUsage() {
      String usage = this.messages.getString("command-usage");
      return usage == null ? "&#FC0000&lTPA\n&#FC0000➤ &f/tpa <player>\n&#FC0000➤ &f/tpahere <player>\n&#FC0000➤ &f/tpaccept\n&#FC0000➤ &f/tpadeny\n&#FC0000➤ &f/tpacancel\n&#FC0000➤ &f/tpatoggle" : Hex.translateAllColorCodes(usage);
   }
   public String getAdminCommandUsage() {
      String usage = this.messages.getString("admin-command-usage");
      return usage == null ? "&#FF0000&lTPA ADMIN\n&#FF0000➤ &f/tpareload" : Hex.translateAllColorCodes(usage);
   }
   public FileConfiguration getMessages() { return this.messages; }
   public boolean isPrefixEnabled() { return this.prefixEnabled; }
   public String getPrefix() { return this.prefix; }
}