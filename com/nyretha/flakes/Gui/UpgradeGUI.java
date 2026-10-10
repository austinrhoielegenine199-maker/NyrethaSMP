package de.elivb.shards.GUI;

import de.elivb.shards.GemsPlugin;
import de.elivb.shards.Manager.GemManager;
import de.elivb.shards.Utlis.HexUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.economy.Economy;
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
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class UpgradeGUI implements Listener {
   private final JavaPlugin plugin;
   private FileConfiguration upgradeConfig;
   private final Map<Player, Inventory> openUpgradeGUIs = new ConcurrentHashMap();
   private Economy economy;
   private GemManager gemManager;
   private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap();
   private final Map<UUID, Long> lastUpgradeTime = new ConcurrentHashMap();

   public UpgradeGUI(JavaPlugin plugin) {
      this.plugin = plugin;
      this.setupEconomy();
      this.setupGemManager();
      if (!plugin.getDataFolder().exists()) {
         plugin.getDataFolder().mkdirs();
      }

      this.loadUpgradeConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   private void setupEconomy() {
      if (this.plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
         ServicesManager servicesManager = this.plugin.getServer().getServicesManager();
         this.economy = (Economy)servicesManager.getRegistration(Economy.class).getProvider();
         if (this.economy == null) {
         }

      }
   }

   private void setupGemManager() {
      if (this.plugin instanceof GemsPlugin) {
         this.gemManager = ((GemsPlugin)this.plugin).getGemManager();
      }

   }

   public void loadUpgradeConfig() {
      File guiFolder = new File(this.plugin.getDataFolder(), "GUI");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      File upgradeFile;
      if (!(upgradeFile = new File(guiFolder, "gui.upgrade.yml")).exists()) {
         this.copyFromResources();
      }

      this.upgradeConfig = YamlConfiguration.loadConfiguration(upgradeFile);
      if (this.upgradeConfig.getKeys(false).isEmpty()) {
      }

   }

   private void copyFromResources() {
      String resourcePath = "GUI/gui.upgrade.yml";

      try {
         InputStream in = this.plugin.getResource(resourcePath);

         label54: {
            try {
               if (in == null) {
                  break label54;
               }

               File guiFolder = new File(this.plugin.getDataFolder(), "GUI");
               File upgradeFile = new File(guiFolder, "gui.upgrade.yml");
               Files.copy(in, upgradeFile.toPath(), new CopyOption[0]);
            } catch (Throwable var6) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (in != null) {
               in.close();
            }

            return;
         }

         if (in != null) {
            in.close();
         }

      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void openUpgradeGUI(Player player) {
      if (this.gemManager == null || this.gemManager.isLevelSystemEnabled()) {
         if (this.plugin instanceof GemsPlugin) {
            ((GemsPlugin)this.plugin).runForEntity(player, () -> {
               try {
                  double balance = (double)0.0F;
                  int currentLevel = 1;
                  int gemsPerMinute = 1;
                  double upgradeCost = (double)0.0F;
                  String currency = this.gemManager != null ? this.gemManager.getUpgradeCurrency() : "VAULT";
                  if (currency.equalsIgnoreCase("VAULT") && this.economy != null) {
                     balance = this.economy.getBalance(player);
                  } else if (currency.equalsIgnoreCase("DonutShards") && this.plugin instanceof GemsPlugin) {
                     GemsPlugin gemsPlugin = (GemsPlugin)this.plugin;
                     balance = (double)gemsPlugin.getDatabaseManager().getGems(player.getUniqueId());
                  }

                  if (this.plugin instanceof GemsPlugin && this.gemManager != null) {
                     GemsPlugin gemsPlugin = (GemsPlugin)this.plugin;
                     UUID playerId = player.getUniqueId();
                     currentLevel = gemsPlugin.getDatabaseManager().getGemLevel(playerId);
                     gemsPerMinute = this.gemManager.getGemsPerMinute(player);
                     upgradeCost = this.gemManager.getUpgradeCost(currentLevel);
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

                  this.setConfiguredItem(gui, "close", player, balance, currentLevel, gemsPerMinute, upgradeCost);
                  this.setConfiguredItem(gui, "info", player, balance, currentLevel, gemsPerMinute, upgradeCost);
                  this.setConfiguredItem(gui, "upgrade", player, balance, currentLevel, gemsPerMinute, upgradeCost);
                  player.openInventory(gui);
                  this.openUpgradeGUIs.put(player, gui);
               } catch (Exception e) {
                  e.printStackTrace();
               }

            });
         } else {
            try {
               double balance = (double)0.0F;
               int currentLevel = 1;
               int gemsPerMinute = 1;
               double upgradeCost = (double)0.0F;
               String currency = this.gemManager != null ? this.gemManager.getUpgradeCurrency() : "VAULT";
               if (currency.equalsIgnoreCase("VAULT") && this.economy != null) {
                  balance = this.economy.getBalance(player);
               } else if (currency.equalsIgnoreCase("DonutShards") && this.plugin instanceof GemsPlugin) {
                  GemsPlugin gemsPlugin = (GemsPlugin)this.plugin;
                  balance = (double)gemsPlugin.getDatabaseManager().getGems(player.getUniqueId());
               }

               if (this.plugin instanceof GemsPlugin && this.gemManager != null) {
                  GemsPlugin gemsPlugin = (GemsPlugin)this.plugin;
                  UUID playerId = player.getUniqueId();
                  currentLevel = gemsPlugin.getDatabaseManager().getGemLevel(playerId);
                  gemsPerMinute = this.gemManager.getGemsPerMinute(player);
                  upgradeCost = this.gemManager.getUpgradeCost(currentLevel);
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

               this.setConfiguredItem(gui, "close", player, balance, currentLevel, gemsPerMinute, upgradeCost);
               this.setConfiguredItem(gui, "info", player, balance, currentLevel, gemsPerMinute, upgradeCost);
               this.setConfiguredItem(gui, "upgrade", player, balance, currentLevel, gemsPerMinute, upgradeCost);
               player.openInventory(gui);
               this.openUpgradeGUIs.put(player, gui);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

      }
   }

   public String getGUITitle() {
      return HexUtils.colorize(this.upgradeConfig.getString("gui.title", "&8ꜱʜᴀʀᴅꜱʟᴇᴠᴇʟ"));
   }

   public int getGUISize() {
      int rows = this.upgradeConfig.getInt("gui.rows", 3);
      return rows * 9;
   }

   public boolean isBackgroundFillEnabled() {
      return this.upgradeConfig.getBoolean("background.fill", false);
   }

   public ItemStack getBackgroundItem() {
      String materialName = this.upgradeConfig.getString("background.material", "BLACK_STAINED_GLASS_PANE");

      Material material;
      try {
         material = Material.valueOf(materialName);
      } catch (IllegalArgumentException var8) {
         material = Material.BLACK_STAINED_GLASS_PANE;
      }

      String name = HexUtils.colorize(this.upgradeConfig.getString("background.name", ""));
      List<String> lore = this.upgradeConfig.getStringList("background.lore");
      ArrayList<String> coloredLore = new ArrayList();

      for(String line : lore) {
         coloredLore.add(HexUtils.colorize(line));
      }

      ItemStack item = new ItemStack(material, 1);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         if (!coloredLore.isEmpty()) {
            meta.setLore(coloredLore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   public List<Integer> getBackgroundSlots() {
      if (!this.isBackgroundFillEnabled()) {
         return new ArrayList();
      } else {
         return this.upgradeConfig.isString("background.slots") ? this.parseSlotRanges(this.upgradeConfig.getString("background.slots", "")) : this.upgradeConfig.getIntegerList("background.slots");
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

   public ItemStack getItem(String itemKey, Player player, double balance, int currentLevel, int currentGemsPerMinute, double upgradeCost) {
      String path = "items." + itemKey;
      if (!this.upgradeConfig.contains(path)) {
         return null;
      } else {
         String materialName = this.upgradeConfig.getString(path + ".material", "STONE");

         Material material;
         try {
            material = Material.valueOf(materialName);
         } catch (IllegalArgumentException var26) {
            material = Material.STONE;
         }

         String name = this.upgradeConfig.getString(path + ".name", " ");
         List<String> lore = this.upgradeConfig.getStringList(path + ".lore");
         int nextLevel = currentLevel + 1;
         int nextGemsPerMinute = this.gemManager != null ? this.gemManager.getGemsPerMinuteForLevel(nextLevel) : currentGemsPerMinute + 1;
         double nextUpgradeCost = this.gemManager != null ? this.gemManager.getUpgradeCost(nextLevel) : upgradeCost * (double)1.25F;
         int gemsAddedDifference = nextGemsPerMinute - currentGemsPerMinute;
         String currency = this.gemManager != null ? this.gemManager.getUpgradeCurrency() : "VAULT";
         String formattedCost;
         if (currency.equalsIgnoreCase("VAULT") && this.economy != null) {
            formattedCost = this.economy.format(upgradeCost);
         } else {
            Object[] var10001 = new Object[]{upgradeCost};
            formattedCost = String.format("%,.0f", var10001) + " ✦";
         }

         HashMap<String, String> placeholders = new HashMap();
         placeholders.put("player", player.getName());
         placeholders.put("balance", currency.equalsIgnoreCase("VAULT") && this.economy != null ? this.economy.format(balance) : String.format("%,.0f", balance) + " ✦");
         placeholders.put("current_level", String.valueOf(currentLevel));
         placeholders.put("next_level", String.valueOf(nextLevel));
         placeholders.put("upgrade_cost", formattedCost);
         placeholders.put("next_upgrade_cost", currency.equalsIgnoreCase("VAULT") && this.economy != null ? this.economy.format(nextUpgradeCost) : String.format("%,.0f", nextUpgradeCost) + " ✦");
         placeholders.put("current_gems_per_minute", String.valueOf(currentGemsPerMinute));
         placeholders.put("next_shards_per_minute", String.valueOf(nextGemsPerMinute));
         placeholders.put("shards_level", String.valueOf(currentLevel));
         placeholders.put("shards_added", String.valueOf(currentGemsPerMinute));
         placeholders.put("shards_added_difference", String.valueOf(gemsAddedDifference));
         placeholders.put("price", formattedCost);
         placeholders.put("Xshards_addedX", String.valueOf(currentGemsPerMinute));
         placeholders.put("XphosX", formattedCost);
         name = this.replacePercentPlaceholders(name, placeholders);
         ArrayList<String> formattedLore = new ArrayList();

         for(String line : lore) {
            line = this.replacePercentPlaceholders(line, placeholders);
            formattedLore.add(HexUtils.colorize(line));
         }

         name = HexUtils.colorize(name);
         ItemStack item = new ItemStack(material, 1);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(formattedLore);
            item.setItemMeta(meta);
         }

         return item;
      }
   }

   private String replacePercentPlaceholders(String text, Map<String, String> placeholders) {
      if (text == null) {
         return "";
      } else {
         String result = text;

         for(Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + (String)entry.getKey() + "%", (CharSequence)entry.getValue());
         }

         return result;
      }
   }

   public int getItemSlot(String itemKey) {
      return this.upgradeConfig.getInt("items." + itemKey + ".slot", -1);
   }

   public boolean hasItem(String itemKey) {
      return this.upgradeConfig.contains("items." + itemKey);
   }

   private void setConfiguredItem(Inventory gui, String itemKey, Player player, double balance, int currentLevel, int currentGemsPerMinute, double upgradeCost) {
      if (this.hasItem(itemKey)) {
         int slot = this.getItemSlot(itemKey);
         ItemStack item = this.getItem(itemKey, player, balance, currentLevel, currentGemsPerMinute, upgradeCost);
         if (slot >= 0 && slot < gui.getSize() && item != null) {
            gui.setItem(slot, item);
         }
      }

   }

   public void closeUpgradeGUI(Player player) {
      this.openUpgradeGUIs.remove(player);
   }

   public boolean isUpgradeGUI(Inventory inventory) {
      return this.openUpgradeGUIs.containsValue(inventory);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getClickedInventory() != null) {
         if (this.isUpgradeGUI(event.getInventory())) {
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
      if (this.isUpgradeGUI(event.getInventory())) {
         event.setCancelled(true);
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         this.closeUpgradeGUI((Player)event.getPlayer());
      }

   }

   private void handleButtonClick(Player player, int slot, ItemStack item) {
      if (this.gemManager != null && !this.gemManager.isLevelSystemEnabled()) {
         player.closeInventory();
      } else if (slot == this.getItemSlot("close")) {
         if (this.plugin instanceof GemsPlugin) {
            GemsPlugin var10000 = (GemsPlugin)this.plugin;
            Objects.requireNonNull(player);
            var10000.runForEntity(player, player::closeInventory);
         } else {
            player.closeInventory();
         }

      } else if (slot == this.getItemSlot("upgrade")) {
         this.handleUpgrade(player);
      } else if (slot != this.getItemSlot("info")) {
         ;
      }
   }

   private void handleUpgrade(Player player) {
      UUID playerId = player.getUniqueId();
      long currentTime = System.currentTimeMillis();
      Long lastUpgrade = (Long)this.lastUpgradeTime.get(playerId);
      if (lastUpgrade == null || currentTime - lastUpgrade >= 1000L) {
         if (this.plugin instanceof GemsPlugin) {
            if (this.gemManager != null) {
               GemsPlugin gemsPlugin = (GemsPlugin)this.plugin;
               String currency = this.gemManager.getUpgradeCurrency();
               int currentLevel = gemsPlugin.getDatabaseManager().getGemLevel(playerId);
               double upgradeCost = this.gemManager.getUpgradeCost(currentLevel);
               int maxLevel = this.gemManager.getMaxLevel();
               int upgradeCostInt = (int)Math.round(upgradeCost);
               boolean canAfford = false;
               if (currency.equalsIgnoreCase("VAULT")) {
                  if (this.economy == null) {
                     this.sendMessageOnce(player, "economy-not-available");
                     return;
                  }

                  double currentBalance = this.economy.getBalance(player);
                  canAfford = currentBalance >= upgradeCost;
               } else if (currency.equalsIgnoreCase("DonutShards")) {
                  int currentGems = gemsPlugin.getDatabaseManager().getGems(playerId);
                  canAfford = currentGems >= upgradeCostInt;
               } else {
                  if (this.economy == null) {
                     this.sendMessageOnce(player, "economy-not-available");
                     return;
                  }

                  double currentBalance = this.economy.getBalance(player);
                  canAfford = currentBalance >= upgradeCost;
               }

               if (!canAfford) {
                  if (currency.equalsIgnoreCase("DonutShards")) {
                     this.sendMessageOnce(player, "not-enough-shards");
                  } else {
                     this.sendMessageOnce(player, "not-enough-money");
                  }

               } else if (currentLevel >= maxLevel) {
                  this.sendMessageOnce(player, "max-level-reached");
               } else {
                  try {
                     if (currency.equalsIgnoreCase("VAULT")) {
                        this.economy.withdrawPlayer(player, upgradeCost);
                     } else if (currency.equalsIgnoreCase("DonutShards")) {
                        boolean success = gemsPlugin.getDatabaseManager().removeGems(playerId, upgradeCostInt);
                        if (!success) {
                           this.sendMessageOnce(player, "not-enough-shards");
                           return;
                        }
                     } else if (this.economy != null) {
                        this.economy.withdrawPlayer(player, upgradeCost);
                     }

                     int levelBeforeUpgrade = gemsPlugin.getDatabaseManager().getGemLevel(playerId);
                     if (levelBeforeUpgrade != currentLevel) {
                        if (currency.equalsIgnoreCase("VAULT")) {
                           if (this.economy != null) {
                              this.economy.depositPlayer(player, upgradeCost);
                           }
                        } else if (currency.equalsIgnoreCase("DonutShards")) {
                           gemsPlugin.getDatabaseManager().addGems(playerId, upgradeCostInt);
                        } else if (this.economy != null) {
                           this.economy.depositPlayer(player, upgradeCost);
                        }

                        this.sendMessageOnce(player, "level-already-upgraded");
                        this.refreshGUI(player);
                        return;
                     }

                     int newLevel = levelBeforeUpgrade + 1;
                     this.gemManager.setGemLevel(player, newLevel);
                     int verifyLevel = gemsPlugin.getDatabaseManager().getGemLevel(playerId);
                     if (verifyLevel != newLevel) {
                        gemsPlugin.getDatabaseManager().setGemLevel(playerId, newLevel);
                     }

                     if (this.plugin instanceof GemsPlugin) {
                        ((GemsPlugin)this.plugin).getSoundManager().playSound(player, "level-up");
                     }

                     this.lastUpgradeTime.put(playerId, currentTime);
                     this.refreshGUI(player);
                  } catch (Exception e) {
                     e.printStackTrace();

                     try {
                        if (currency.equalsIgnoreCase("VAULT")) {
                           if (this.economy != null) {
                              this.economy.depositPlayer(player, upgradeCost);
                           }
                        } else if (currency.equalsIgnoreCase("DonutShards")) {
                           gemsPlugin.getDatabaseManager().addGems(playerId, upgradeCostInt);
                        } else if (this.economy != null) {
                           this.economy.depositPlayer(player, upgradeCost);
                        }
                     } catch (Exception ex) {
                        ex.printStackTrace();
                     }
                  }

               }
            }
         }
      }
   }

   private void refreshGUI(Player player) {
      if (this.plugin instanceof GemsPlugin) {
         ((GemsPlugin)this.plugin).runForEntity(player, () -> {
            player.closeInventory();
            ((GemsPlugin)this.plugin).runTaskLater(() -> {
               if (player.isOnline()) {
                  this.openUpgradeGUI(player);
               }

            }, 5L);
         });
      } else {
         player.closeInventory();
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
               this.openUpgradeGUI(player);
            }

         }, 5L);
      }

   }

   private void sendMessageOnce(Player player, String messageKey) {
      UUID playerId = player.getUniqueId();
      long currentTime = System.currentTimeMillis();
      if (!this.lastMessageTime.containsKey(playerId) || currentTime - (Long)this.lastMessageTime.get(playerId) >= 500L) {
         GemsPlugin gemsPlugin;
         Object message;
         if (this.plugin instanceof GemsPlugin && (message = (gemsPlugin = (GemsPlugin)this.plugin).getLangManager().getMessage(messageKey)) != null && !((String)message).trim().isEmpty()) {
            String prefix;
            if (gemsPlugin.getLangManager().isPrefixEnabled() && !((String)message).startsWith(prefix = gemsPlugin.getLangManager().getPrefix())) {
               message = prefix + (String)message;
            }

            player.sendMessage(HexUtils.colorize((String)message));
            this.lastMessageTime.put(playerId, currentTime);
         }

      }
   }

   public Economy getEconomy() {
      return this.economy;
   }

   public boolean isEconomyEnabled() {
      return this.economy != null;
   }

   public void reloadUpgradeConfig() {
      this.closeAllOpenGUIs();
      this.loadUpgradeConfig();
      if (this.gemManager != null) {
         this.gemManager.reloadLevelConfig();
      }

   }

   private void closeAllOpenGUIs() {
      for(Player player : new ArrayList(this.openUpgradeGUIs.keySet())) {
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

      this.openUpgradeGUIs.clear();
      this.lastMessageTime.clear();
      this.lastUpgradeTime.clear();
   }

   public void debugPlaceholders(Player player) {
      if (this.gemManager != null) {
         int currentLevel = this.gemManager.getGemLevel(player);
         this.gemManager.getGemsPerMinute(player);
         this.gemManager.getGemsPerMinuteForLevel(currentLevel + 1);
         this.gemManager.getUpgradeCost(player);
      }
   }
}
