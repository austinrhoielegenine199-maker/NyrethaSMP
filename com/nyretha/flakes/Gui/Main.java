package de.elivb.shards.GUI;

import de.elivb.shards.GemsPlugin;
import de.elivb.shards.Manager.PlaceholderManager;
import de.elivb.shards.Utlis.HexUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main implements Listener {
   private final JavaPlugin plugin;
   private FileConfiguration viewConfig;
   private File viewFile;
   private File guiFolder;
   private final Map<Player, Inventory> openGUIs = new ConcurrentHashMap();

   public Main(JavaPlugin plugin) {
      this.plugin = plugin;
      this.guiFolder = new File(plugin.getDataFolder(), "GUI");
      if (!plugin.getDataFolder().exists()) {
         plugin.getDataFolder().mkdirs();
      }

      this.loadViewConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void loadViewConfig() {
      if (!this.guiFolder.exists()) {
         this.guiFolder.mkdirs();
      }

      this.viewFile = new File(this.guiFolder, "gui.view.yml");
      if (!this.viewFile.exists()) {
         this.copyFromResources();
      }

      this.viewConfig = YamlConfiguration.loadConfiguration(this.viewFile);
      if (this.viewConfig.getKeys(false).isEmpty()) {
         this.createDefaultConfig();
         this.viewConfig = YamlConfiguration.loadConfiguration(this.viewFile);
      }

   }

   private void copyFromResources() {
      String[] possiblePaths = new String[]{"GUI/gui.view.yml", "gui.view.yml"};
      InputStream in = null;
      String foundPath = null;

      for(String path : possiblePaths) {
         in = this.plugin.getResource(path);
         if (in != null) {
            foundPath = path;
            break;
         }
      }

      if (in != null && foundPath != null) {
         try {
            Files.copy(in, this.viewFile.toPath(), new CopyOption[0]);
         } catch (IOException var16) {
            this.createDefaultConfig();
         } finally {
            try {
               in.close();
            } catch (IOException var15) {
            }

         }
      } else {
         this.createDefaultConfig();
      }

   }

   private void createDefaultConfig() {
      if (this.guiFolder == null) {
         this.guiFolder = new File(this.plugin.getDataFolder(), "GUI");
      }

      if (!this.guiFolder.exists()) {
         this.guiFolder.mkdirs();
      }

      if (this.viewFile == null) {
         this.viewFile = new File(this.guiFolder, "gui.view.yml");
      }

      try {
         if (!this.viewFile.exists()) {
            this.viewFile.createNewFile();
         }
      } catch (IOException var3) {
      }

      if (this.viewConfig == null) {
         this.viewConfig = new YamlConfiguration();
      }

      this.viewConfig.set("gui.title", "&8ꜱʜᴀʀᴅ");
      this.viewConfig.set("gui.rows", 4);
      this.viewConfig.set("background.fill", false);
      this.viewConfig.set("background.material", "GRAY_STAINED_GLASS_PANE");
      this.viewConfig.set("background.name", "");
      this.viewConfig.set("background.lore", Collections.emptyList());
      this.viewConfig.set("background.slots", "0-8");
      this.viewConfig.set("items.gems-balance.material", "AMETHYST_SHARD");
      this.viewConfig.set("items.gems-balance.name", "&#A303F9ꜱʜᴀʀᴅ");
      this.viewConfig.set("items.gems-balance.lore", Arrays.asList("&fClick to pay"));
      this.viewConfig.set("items.gems-balance.slot", 13);
      this.viewConfig.set("items.close.material", "RED_STAINED_GLASS_PANE");
      this.viewConfig.set("items.close.name", "&#FF0000ᴄʟᴏꜱᴇ");
      this.viewConfig.set("items.close.lore", Arrays.asList("&fClick to return"));
      this.viewConfig.set("items.close.slot", 27);
      this.viewConfig.set("items.back-page.material", "ARROW");
      this.viewConfig.set("items.back-page.name", "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ");
      this.viewConfig.set("items.back-page.lore", Arrays.asList("&fClick to go to the previous page"));
      this.viewConfig.set("items.back-page.slot", 30);
      this.viewConfig.set("items.refresh.material", "ANVIL");
      this.viewConfig.set("items.refresh.name", "&#A303F9ꜱʜᴀʀᴅ");
      this.viewConfig.set("items.refresh.lore", Arrays.asList("&fClick to refresh"));
      this.viewConfig.set("items.refresh.slot", 31);
      this.viewConfig.set("items.next-page.material", "ARROW");
      this.viewConfig.set("items.next-page.name", "&#00fc88ɴᴇxᴛ");
      this.viewConfig.set("items.next-page.lore", Arrays.asList("&fClick to go to the next page"));
      this.viewConfig.set("items.next-page.slot", 32);

      try {
         this.viewConfig.save(this.viewFile);
      } catch (IOException var2) {
      }

   }

   public void reloadViewConfig() {
      this.closeAllOpenGUIs();
      if (!this.viewFile.exists()) {
         this.copyFromResources();
      }

      this.viewConfig = YamlConfiguration.loadConfiguration(this.viewFile);
   }

   private void closeAllOpenGUIs() {
      for(Player player : new ArrayList(this.openGUIs.keySet())) {
         if (player.isOnline() && player.getOpenInventory() != null) {
            if (this.plugin instanceof GemsPlugin) {
               GemsPlugin var10000 = (GemsPlugin)this.plugin;
               Objects.requireNonNull(player);
               var10000.runForEntity(player, player::closeInventory);
            } else {
               player.closeInventory();
            }
         }
      }

      this.openGUIs.clear();
   }

   public boolean isGUIEnabled() {
      return this.viewConfig.getBoolean("gui.enabled", true);
   }

   public String getGUITitle() {
      return HexUtils.colorize(this.viewConfig.getString("gui.title", "&8ꜱʜᴀʀᴅ"));
   }

   public int getGUISize() {
      int rows = this.viewConfig.getInt("gui.rows", 4);
      return rows * 9;
   }

   public int getGUIRows() {
      return this.viewConfig.getInt("gui.rows", 4);
   }

   public boolean isBackgroundFillEnabled() {
      return this.viewConfig.getBoolean("background.fill", true);
   }

   public ItemStack getBackgroundItem() {
      Material material = Material.valueOf(this.viewConfig.getString("background.material", "GRAY_STAINED_GLASS_PANE"));
      String name = HexUtils.colorize(this.viewConfig.getString("background.name", " "));
      List<String> lore = this.viewConfig.getStringList("background.lore");
      ItemStack item = new ItemStack(material, 1);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   public List<Integer> getBackgroundSlots() {
      if (!this.isBackgroundFillEnabled()) {
         return new ArrayList();
      } else {
         return this.viewConfig.isString("background.slots") ? this.parseSlotRanges(this.viewConfig.getString("background.slots", "")) : this.viewConfig.getIntegerList("background.slots");
      }
   }

   private List<Integer> parseSlotRanges(String slotRanges) {
      ArrayList<Integer> slots = new ArrayList();
      if (slotRanges != null && !slotRanges.isEmpty()) {
         try {
            for(String range : slotRanges.split(",")) {
               if ((range = range.trim()).contains("-")) {
                  String[] parts = range.split("-");
                  if (parts.length == 2) {
                     int start = Integer.parseInt(parts[0].trim());
                     int end = Integer.parseInt(parts[1].trim());

                     for(int i = start; i <= end; ++i) {
                        slots.add(i);
                     }
                  }
               } else {
                  slots.add(Integer.parseInt(range.trim()));
               }
            }
         } catch (NumberFormatException var12) {
         }

         return slots;
      } else {
         return slots;
      }
   }

   public ItemStack getItem(String itemKey, Player player, int gems) {
      String path = "items." + itemKey;
      if (!this.viewConfig.contains(path)) {
         return null;
      } else {
         Material material = Material.valueOf(this.viewConfig.getString(path + ".material", "STONE"));
         String name = this.viewConfig.getString(path + ".name", " ");
         List<String> lore = this.viewConfig.getStringList(path + ".lore");
         name = PlaceholderManager.replacePlaceholders(name, player, gems);
         ArrayList<String> formattedLore = new ArrayList();

         for(String line : lore) {
            formattedLore.add(HexUtils.colorize(PlaceholderManager.replacePlaceholders(line, player, gems)));
         }

         ItemStack item = new ItemStack(material, 1);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(HexUtils.colorize(name));
            meta.setLore(formattedLore);
            item.setItemMeta(meta);
         }

         return item;
      }
   }

   public int getItemSlot(String itemKey) {
      return this.viewConfig.getInt("items." + itemKey + ".slot", -1);
   }

   public boolean hasItem(String itemKey) {
      return this.viewConfig.contains("items." + itemKey);
   }

   public void openGUI(Player player) {
      if (this.plugin instanceof GemsPlugin) {
         ((GemsPlugin)this.plugin).runForEntity(player, () -> {
            try {
               UUID playerUUID = player.getUniqueId();
               int gems = 0;
               if (this.plugin instanceof GemsPlugin) {
                  gems = ((GemsPlugin)this.plugin).getDatabaseManager().getGems(playerUUID);
               }

               Inventory gui = Bukkit.createInventory((InventoryHolder)null, this.getGUISize(), this.getGUITitle());
               if (this.isBackgroundFillEnabled()) {
                  ItemStack background = this.getBackgroundItem();

                  for(int slot : this.getBackgroundSlots()) {
                     if (slot >= 0 && slot < gui.getSize()) {
                        gui.setItem(slot, background);
                     }
                  }
               }

               this.setConfiguredItem(gui, "gems-balance", player, gems);
               this.setConfiguredItem(gui, "info", player, gems);
               this.setConfiguredItem(gui, "close", player, gems);
               this.setConfiguredItem(gui, "refresh", player, gems);
               this.setConfiguredItem(gui, "back-page", player, gems);
               this.setConfiguredItem(gui, "next-page", player, gems);
               player.openInventory(gui);
               this.openGUIs.put(player, gui);
            } catch (Exception var8) {
            }

         });
      } else {
         try {
            UUID playerUUID = player.getUniqueId();
            int gems = 0;
            if (this.plugin instanceof GemsPlugin) {
               gems = ((GemsPlugin)this.plugin).getDatabaseManager().getGems(playerUUID);
            }

            Inventory gui = Bukkit.createInventory((InventoryHolder)null, this.getGUISize(), this.getGUITitle());
            if (this.isBackgroundFillEnabled()) {
               ItemStack background = this.getBackgroundItem();

               for(int slot : this.getBackgroundSlots()) {
                  if (slot >= 0 && slot < gui.getSize()) {
                     gui.setItem(slot, background);
                  }
               }
            }

            this.setConfiguredItem(gui, "gems-balance", player, gems);
            this.setConfiguredItem(gui, "info", player, gems);
            this.setConfiguredItem(gui, "close", player, gems);
            this.setConfiguredItem(gui, "refresh", player, gems);
            this.setConfiguredItem(gui, "back-page", player, gems);
            this.setConfiguredItem(gui, "next-page", player, gems);
            player.openInventory(gui);
            this.openGUIs.put(player, gui);
         } catch (Exception var8) {
         }
      }

   }

   private void setConfiguredItem(Inventory gui, String itemKey, Player player, int gems) {
      if (this.hasItem(itemKey)) {
         int slot = this.getItemSlot(itemKey);
         ItemStack item = this.getItem(itemKey, player, gems);
         if (slot >= 0 && slot < gui.getSize() && item != null) {
            gui.setItem(slot, item);
         }
      }

   }

   public void updateGUI(Player player) {
      if (player.getOpenInventory() != null && player.getOpenInventory().getTitle().equals(this.getGUITitle())) {
         this.openGUI(player);
      }

   }

   public void closeGUI(Player player) {
      this.openGUIs.remove(player);
   }

   public boolean isGemGUI(Inventory inventory) {
      return this.openGUIs.containsValue(inventory);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) throws SQLException {
      if (event.getClickedInventory() != null) {
         if (this.isGemGUI(event.getInventory())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
               Player player = (Player)event.getWhoClicked();
               if (event.getCurrentItem() != null && event.getClickedInventory().equals(event.getInventory())) {
                  if (this.plugin instanceof GemsPlugin) {
                     ((GemsPlugin)this.plugin).getSoundManager().playClickSound(player);
                  }

                  this.handleButtonClick(player, event.getSlot(), event.getCurrentItem());
               }
            }
         }

      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (this.isGemGUI(event.getInventory())) {
         event.setCancelled(true);
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         this.closeGUI((Player)event.getPlayer());
      }

   }

   private void handleButtonClick(Player player, int slot, ItemStack item) throws SQLException {
      if (slot == this.getItemSlot("close")) {
         if (this.plugin instanceof GemsPlugin) {
            GemsPlugin var10000 = (GemsPlugin)this.plugin;
            Objects.requireNonNull(player);
            var10000.runForEntity(player, player::closeInventory);
         } else {
            player.closeInventory();
         }

      } else if (slot == this.getItemSlot("gems-balance")) {
         if (this.plugin instanceof GemsPlugin) {
            GemsPlugin gemsPlugin = (GemsPlugin)this.plugin;
            gemsPlugin.getPayGUI().openPayGUI(player);
         }

      } else {
         if (slot == this.getItemSlot("refresh")) {
            this.updateGUI(player);
            this.sendMessage(player, "gui-updated");
         }

      }
   }

   private void sendMessage(Player player, String messageKey) throws SQLException {
      if (this.plugin instanceof GemsPlugin) {
         GemsPlugin gemsPlugin = (GemsPlugin)this.plugin;
         int gems = gemsPlugin.getDatabaseManager().getGems(player.getUniqueId());
         String message = gemsPlugin.getLangManager().getMessage(messageKey, player, gems);
         player.sendMessage(message);
      }

   }

   public void updateGemGUI(Player player, int newGems) {
      if (this.openGUIs.containsKey(player)) {
         if (this.plugin instanceof GemsPlugin) {
            ((GemsPlugin)this.plugin).runForEntity(player, () -> {
               player.closeInventory();
               this.openGUI(player);
            });
         } else {
            player.closeInventory();
            this.openGUI(player);
         }
      }

   }
}
