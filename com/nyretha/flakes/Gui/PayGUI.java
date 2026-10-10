package de.elivb.shards.GUI;

import de.elivb.shards.GemsPlugin;
import de.elivb.shards.Manager.DatabaseManager;
import de.elivb.shards.Manager.LangManager;
import de.elivb.shards.Manager.PlaceholderManager;
import de.elivb.shards.Utlis.HexUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class PayGUI implements Listener {
   private final JavaPlugin plugin;
   private FileConfiguration payConfig;
   private File payFile;
   private File guiFolder;
   private final Map<Player, Inventory> openPayGUIs = new ConcurrentHashMap();
   private final Map<Player, OfflinePlayer> selectedPlayers = new ConcurrentHashMap();
   private final Map<Player, Boolean> awaitingChatInput = new ConcurrentHashMap();
   private final Map<Player, Integer> currentPages = new ConcurrentHashMap();
   private final Set<Player> processingChat = ConcurrentHashMap.newKeySet();
   private final Map<Player, List<OfflinePlayer>> playerLists = new ConcurrentHashMap();

   public PayGUI(JavaPlugin plugin) {
      this.plugin = plugin;
      this.guiFolder = new File(plugin.getDataFolder(), "GUI");
      File dataFolder = plugin.getDataFolder();
      if (!dataFolder.exists() && !dataFolder.mkdirs()) {
      }

      this.loadPayConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void loadPayConfig() {
      if (!this.guiFolder.exists() && !this.guiFolder.mkdirs()) {
      }

      this.payFile = new File(this.guiFolder, "gui.pay.yml");
      if (!this.payFile.exists()) {
         this.copyFromResources();
      }

      this.payConfig = YamlConfiguration.loadConfiguration(this.payFile);
      if (this.payConfig.getKeys(false).isEmpty()) {
         this.createDefaultConfig();
         this.payConfig = YamlConfiguration.loadConfiguration(this.payFile);
      }

   }

   private void copyFromResources() {
      String[] possiblePaths = new String[]{"GUI/gui.pay.yml", "gui.pay.yml"};
      InputStream in = null;

      for(String path : possiblePaths) {
         in = this.plugin.getResource(path);
         if (in != null) {
            break;
         }
      }

      if (in != null) {
         try {
            Files.copy(in, this.payFile.toPath(), new CopyOption[0]);
         } catch (IOException var15) {
            this.createDefaultConfig();
         } finally {
            try {
               in.close();
            } catch (IOException var14) {
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

      if (!this.guiFolder.exists() && !this.guiFolder.mkdirs()) {
      }

      if (this.payFile == null) {
         this.payFile = new File(this.guiFolder, "gui.pay.yml");
      }

      try {
         if (!this.payFile.exists()) {
            boolean created = this.payFile.createNewFile();
            if (!created) {
            }
         }
      } catch (IOException var3) {
      }

      if (this.payConfig == null) {
         this.payConfig = new YamlConfiguration();
      }

      this.payConfig.set("gui.title", "&8ꜱʜᴀʀᴅ (Page %current_page%)");
      this.payConfig.set("gui.rows", 6);
      this.payConfig.set("background.fill", false);
      this.payConfig.set("background.material", "BLACK_STAINED_GLASS_PANE");
      this.payConfig.set("background.name", "");
      this.payConfig.set("background.lore", Collections.emptyList());
      this.payConfig.set("items.back-page.material", "ARROW");
      this.payConfig.set("items.back-page.name", "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ");
      this.payConfig.set("items.back-page.lore", Arrays.asList("&fClick to go to the previous page"));
      this.payConfig.set("items.back-page.slot", 45);
      this.payConfig.set("items.refresh.material", "ANVIL");
      this.payConfig.set("items.refresh.name", "&#A303F9ꜱʜᴀʀᴅ");
      this.payConfig.set("items.refresh.lore", Arrays.asList("&fClick to refresh"));
      this.payConfig.set("items.refresh.slot", 49);
      this.payConfig.set("items.next-page.material", "ARROW");
      this.payConfig.set("items.next-page.name", "&#00fc88ɴᴇxᴛ");
      this.payConfig.set("items.next-page.lore", Arrays.asList("&fClick to go to the next page"));
      this.payConfig.set("items.next-page.slot", 53);
      this.payConfig.set("player.online.material", "PLAYER_HEAD");
      this.payConfig.set("player.online.name", "&#04fc04%player_name%");
      this.payConfig.set("player.online.lore", Arrays.asList("&fClick to pay shards"));
      this.payConfig.set("player.offline.material", "PLAYER_HEAD");
      this.payConfig.set("player.offline.name", "&#fc0404%player_name%");
      this.payConfig.set("player.offline.lore", Arrays.asList("&fClick to pay shards"));

      try {
         this.payConfig.save(this.payFile);
      } catch (IOException var2) {
      }

   }

   public boolean isPayGUIEnabled() {
      return this.payConfig.getBoolean("gui.enabled", true);
   }

   public void openPayGUI(Player player) {
      if (this.isPayGUIEnabled()) {
         this.openPayGUI(player, 1);
      }
   }

   public void openPayGUI(Player player, int page) {
      if (this.plugin instanceof GemsPlugin) {
         ((GemsPlugin)this.plugin).runForEntity(player, () -> {
            try {
               if ((Boolean)this.awaitingChatInput.getOrDefault(player, false)) {
                  this.resetPlayerState(player);
               }

               List<OfflinePlayer> allPlayers = this.getAllPlayers();
               allPlayers.removeIf((offlinePlayer) -> offlinePlayer.getUniqueId().equals(player.getUniqueId()));
               this.playerLists.put(player, new ArrayList(allPlayers));
               int playersPerPage = 45;
               int totalPages = (int)Math.ceil((double)allPlayers.size() / (double)playersPerPage);
               if (totalPages < 1) {
                  totalPages = 1;
               }

               int currentPage = page;
               if (page < 1) {
                  currentPage = 1;
               }

               if (currentPage > totalPages) {
                  currentPage = totalPages;
               }

               this.currentPages.put(player, currentPage);
               int startIndex = Math.max(0, (currentPage - 1) * playersPerPage);
               int endIndex = Math.min(startIndex + playersPerPage, allPlayers.size());
               String title = this.getGUITitle(currentPage, totalPages);
               Inventory gui = Bukkit.createInventory((InventoryHolder)null, this.getGUISize(), title);
               if (this.isBackgroundFillEnabled()) {
                  ItemStack background = this.getBackgroundItem();

                  for(int slot : this.getBackgroundSlots()) {
                     if (slot >= 0 && slot < gui.getSize()) {
                        gui.setItem(slot, background);
                     }
                  }
               }

               if (this.hasItem("back-page")) {
                  this.setConfiguredItem(gui, "back-page", player, currentPage, totalPages, allPlayers.size());
               }

               if (this.hasItem("previous-page")) {
                  this.setConfiguredItem(gui, "previous-page", player, currentPage, totalPages, allPlayers.size());
               }

               if (this.hasItem("refresh")) {
                  this.setConfiguredItem(gui, "refresh", player, currentPage, totalPages, allPlayers.size());
               }

               if (this.hasItem("next-page")) {
                  this.setConfiguredItem(gui, "next-page", player, currentPage, totalPages, allPlayers.size());
               }

               for(int i = startIndex; i < endIndex; ++i) {
                  OfflinePlayer targetPlayer = (OfflinePlayer)allPlayers.get(i);
                  int gems = 0;
                  if (this.plugin instanceof GemsPlugin) {
                     gems = ((GemsPlugin)this.plugin).getDatabaseManager().getGems(targetPlayer.getUniqueId());
                  }

                  ItemStack playerHead = this.createPlayerHeadFromConfig(targetPlayer, gems);
                  int slot = i - startIndex;
                  if (slot < playersPerPage) {
                     gui.setItem(slot, playerHead);
                  }
               }

               player.openInventory(gui);
               this.openPayGUIs.put(player, gui);
            } catch (Exception var16) {
            }

         });
      } else {
         try {
            if ((Boolean)this.awaitingChatInput.getOrDefault(player, false)) {
               this.resetPlayerState(player);
            }

            List<OfflinePlayer> allPlayers = this.getAllPlayers();
            allPlayers.removeIf((offlinePlayer) -> offlinePlayer.getUniqueId().equals(player.getUniqueId()));
            this.playerLists.put(player, new ArrayList(allPlayers));
            int playersPerPage = 45;
            int totalPages = (int)Math.ceil((double)allPlayers.size() / (double)playersPerPage);
            if (totalPages < 1) {
               totalPages = 1;
            }

            int currentPage = page;
            if (page < 1) {
               currentPage = 1;
            }

            if (currentPage > totalPages) {
               currentPage = totalPages;
            }

            this.currentPages.put(player, currentPage);
            int startIndex = Math.max(0, (currentPage - 1) * playersPerPage);
            int endIndex = Math.min(startIndex + playersPerPage, allPlayers.size());
            String title = this.getGUITitle(currentPage, totalPages);
            Inventory gui = Bukkit.createInventory((InventoryHolder)null, this.getGUISize(), title);
            if (this.isBackgroundFillEnabled()) {
               ItemStack background = this.getBackgroundItem();

               for(int slot : this.getBackgroundSlots()) {
                  if (slot >= 0 && slot < gui.getSize()) {
                     gui.setItem(slot, background);
                  }
               }
            }

            if (this.hasItem("back-page")) {
               this.setConfiguredItem(gui, "back-page", player, currentPage, totalPages, allPlayers.size());
            }

            if (this.hasItem("previous-page")) {
               this.setConfiguredItem(gui, "previous-page", player, currentPage, totalPages, allPlayers.size());
            }

            if (this.hasItem("refresh")) {
               this.setConfiguredItem(gui, "refresh", player, currentPage, totalPages, allPlayers.size());
            }

            if (this.hasItem("next-page")) {
               this.setConfiguredItem(gui, "next-page", player, currentPage, totalPages, allPlayers.size());
            }

            for(int i = startIndex; i < endIndex; ++i) {
               OfflinePlayer targetPlayer = (OfflinePlayer)allPlayers.get(i);
               int gems = 0;
               if (this.plugin instanceof GemsPlugin) {
                  gems = ((GemsPlugin)this.plugin).getDatabaseManager().getGems(targetPlayer.getUniqueId());
               }

               ItemStack playerHead = this.createPlayerHeadFromConfig(targetPlayer, gems);
               int slot = i - startIndex;
               if (slot < playersPerPage) {
                  gui.setItem(slot, playerHead);
               }
            }

            player.openInventory(gui);
            this.openPayGUIs.put(player, gui);
         } catch (Exception var16) {
         }
      }

   }

   public ItemStack createPlayerHeadFromConfig(OfflinePlayer targetPlayer, int gems) {
      String configPath = targetPlayer.isOnline() ? "player.online" : "player.offline";
      String materialName = this.payConfig.getString(configPath + ".material", "PLAYER_HEAD");

      Material material;
      try {
         material = Material.valueOf(materialName);
      } catch (IllegalArgumentException var13) {
         material = Material.PLAYER_HEAD;
      }

      String playerName = targetPlayer.getName();
      if (playerName == null && (playerName = this.getPlayerNameFromData(targetPlayer.getUniqueId())) == null) {
         playerName = "...";
      }

      String name = this.payConfig.getString(configPath + ".name", "player_name%");
      List<String> lore = this.payConfig.getStringList(configPath + ".lore");
      Map<String, String> placeholders = new HashMap();
      placeholders.put("player_name", playerName);
      placeholders.put("shards", String.valueOf(gems));
      name = PlaceholderManager.replacePlaceholders(name, placeholders);
      List<String> formattedLore = new ArrayList();

      for(String line : lore) {
         formattedLore.add(HexUtils.colorize(PlaceholderManager.replacePlaceholders(line, placeholders)));
      }

      ItemStack item = new ItemStack(material, 1);
      if (material == Material.PLAYER_HEAD) {
         SkullMeta skullMeta = (SkullMeta)item.getItemMeta();
         if (skullMeta != null) {
            skullMeta.setOwningPlayer(targetPlayer);
            skullMeta.setDisplayName(HexUtils.colorize(name));
            skullMeta.setLore(formattedLore);
            item.setItemMeta(skullMeta);
         }
      } else {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(HexUtils.colorize(name));
            meta.setLore(formattedLore);
            item.setItemMeta(meta);
         }
      }

      return item;
   }

   private String getPlayerNameFromData(UUID playerUUID) {
      File playerDataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      File playerFile = new File(playerDataFolder, playerUUID.toString() + ".yml");
      if (playerFile.exists()) {
         try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            return config.getString("name");
         } catch (Exception var5) {
         }
      }

      return null;
   }

   public String getGUITitle(int currentPage, int totalPages) {
      String title = this.payConfig.getString("gui.title", "&8ꜱʜᴀʀᴅ (Page %current_page%)");
      title = title.replace("%current_page%", String.valueOf(currentPage)).replace("%total_pages%", String.valueOf(totalPages));
      return HexUtils.colorize(title);
   }

   private void startChatInput(Player player, OfflinePlayer targetPlayer) {
      if (player.hasPermission("shards.pay")) {
         this.selectedPlayers.put(player, targetPlayer);
         this.awaitingChatInput.put(player, true);
         if (this.plugin instanceof GemsPlugin) {
            GemsPlugin var10000 = (GemsPlugin)this.plugin;
            Objects.requireNonNull(player);
            var10000.runForEntity(player, player::closeInventory);
         } else {
            player.closeInventory();
         }

         if (this.plugin instanceof GemsPlugin) {
            LangManager langManager = ((GemsPlugin)this.plugin).getLangManager();
            this.sendChatInputMessages(player, langManager);
         }

      }
   }

   private void sendChatInputMessages(Player player, LangManager langManager) {
      String message = langManager.getMessage("send-shards-chat-input");
      if (message != null && !message.isEmpty()) {
         for(String line : message.split("\n")) {
            if (!line.trim().isEmpty()) {
               player.sendMessage(HexUtils.colorize(line.trim()));
            }
         }
      }

   }

   public void onChatInput(Player player, String input) {
      if ((Boolean)this.awaitingChatInput.getOrDefault(player, false)) {
         if (!this.processingChat.contains(player)) {
            this.processingChat.add(player);
            OfflinePlayer targetPlayer = (OfflinePlayer)this.selectedPlayers.get(player);
            if (targetPlayer == null) {
               this.sendMessage(player, "player-not-found");
               this.resetPlayerState(player);
               if (this.plugin instanceof GemsPlugin) {
                  ((GemsPlugin)this.plugin).runTaskLater(() -> this.processingChat.remove(player), 2L);
               } else {
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processingChat.remove(player), 2L);
               }

            } else if (input.equalsIgnoreCase("cancel")) {
               this.sendSingleMessageWithoutPrefix(player, "send-shards-chat-cancel");
               this.resetPlayerState(player);
               this.openPayGUI(player);
               if (this.plugin instanceof GemsPlugin) {
                  ((GemsPlugin)this.plugin).runTaskLater(() -> this.processingChat.remove(player), 2L);
               } else {
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processingChat.remove(player), 2L);
               }

            } else {
               try {
                  int amount = Integer.parseInt(input);
                  if (amount > 0) {
                     try {
                        this.processPayment(player, targetPlayer, amount);
                     } catch (NumberFormatException var10) {
                        this.sendMessage(player, "invalid-number");
                     }

                     return;
                  }

                  this.sendMessage(player, "invalid-number");
                  if (this.plugin instanceof GemsPlugin) {
                     ((GemsPlugin)this.plugin).runTaskLater(() -> this.processingChat.remove(player), 2L);
                  } else {
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processingChat.remove(player), 2L);
                  }
               } finally {
                  if (this.plugin instanceof GemsPlugin) {
                     ((GemsPlugin)this.plugin).runTaskLater(() -> this.processingChat.remove(player), 2L);
                  } else {
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.processingChat.remove(player), 2L);
                  }

               }

            }
         }
      }
   }

   private void processPayment(Player player, OfflinePlayer targetPlayer, int amount) {
      if (targetPlayer == null) {
         this.resetPlayerState(player);
      } else if (!(this.plugin instanceof GemsPlugin)) {
         this.resetPlayerState(player);
      } else {
         DatabaseManager dbManager = ((GemsPlugin)this.plugin).getDatabaseManager();
         int playerGems = dbManager.getGems(player.getUniqueId());
         if (playerGems < amount) {
            this.sendMessage(player, "not-enough-shards");
            this.resetPlayerState(player);
         } else if (amount <= 0) {
            this.sendMessage(player, "invalid-number");
            this.resetPlayerState(player);
         } else {
            try {
               boolean success = dbManager.transferGems(player.getUniqueId(), targetPlayer.getUniqueId(), amount);
               if (success) {
                  int newGems = playerGems - amount;
                  this.sendPaymentMessage(player, "shards-send", amount, targetPlayer.getName(), newGems);
                  if (targetPlayer.isOnline() && targetPlayer.getPlayer() != null) {
                     Player receiver = targetPlayer.getPlayer();
                     int receiverGems = dbManager.getGems(receiver.getUniqueId());
                     this.sendPaymentMessage(receiver, "shards-received", amount, player.getName(), receiverGems);
                  }
               }
            } catch (Exception var13) {
            } finally {
               this.resetPlayerState(player);
            }

         }
      }
   }

   private void sendPaymentMessage(Player player, String messageKey, int amount, String targetName, int newGems) {
      if (this.plugin instanceof GemsPlugin) {
         String message = ((GemsPlugin)this.plugin).getLangManager().getMessage(messageKey);
         if (message != null && !message.isEmpty()) {
            if (messageKey.equals("shards-send")) {
               message = message.replace("%shards_send%", String.valueOf(amount)).replace("%player%", targetName);
            } else if (messageKey.equals("shards-received")) {
               message = message.replace("%shards_received%", String.valueOf(amount)).replace("%target_player%", targetName);
            }

            message = message.replace("%shards%", String.valueOf(newGems)).replace("%shards_send%", String.valueOf(amount)).replace("%shards_received%", String.valueOf(amount)).replace("%shards_given%", String.valueOf(amount)).replace("%shards_taken%", String.valueOf(amount)).replace("%shards_set%", String.valueOf(amount));
            player.sendMessage(HexUtils.colorize(message));
         }
      }

   }

   private void resetPlayerState(Player player) {
      this.awaitingChatInput.remove(player);
      this.selectedPlayers.remove(player);
      this.processingChat.remove(player);
      this.playerLists.remove(player);
   }

   private void sendMessage(Player player, String messageKey, Object... args) {
      String message;
      if (this.plugin instanceof GemsPlugin && (message = ((GemsPlugin)this.plugin).getLangManager().getMessage(messageKey)) != null && !message.isEmpty()) {
         message = this.replaceCustomPlaceholders(message, player, args);
         player.sendMessage(HexUtils.colorize(message));
      }

   }

   private void sendSingleMessageWithoutPrefix(Player player, String messageKey, Object... args) {
      String message;
      if (this.plugin instanceof GemsPlugin && (message = ((GemsPlugin)this.plugin).getLangManager().getMessageWithoutPrefix(messageKey)) != null && !message.isEmpty()) {
         message = this.replaceCustomPlaceholders(message, player, args);
         player.sendMessage(HexUtils.colorize(message));
      }

   }

   private String replaceCustomPlaceholders(String message, Player player, Object... args) {
      if (message == null) {
         return "";
      } else {
         if (this.plugin instanceof GemsPlugin) {
            int gems = ((GemsPlugin)this.plugin).getDatabaseManager().getGems(player.getUniqueId());
            message = message.replace("%player%", player.getName()).replace("%shards%", String.valueOf(gems)).replace("%shards_send%", String.valueOf(gems));
         }

         for(int i = 0; i < args.length; ++i) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
         }

         return message;
      }
   }

   public boolean isAwaitingChatInput(Player player) {
      return (Boolean)this.awaitingChatInput.getOrDefault(player, false);
   }

   @EventHandler
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      if (this.isAwaitingChatInput(player)) {
         event.setCancelled(true);
         String message = event.getMessage();
         if (this.processingChat.contains(player)) {
            return;
         }

         if (this.plugin instanceof GemsPlugin) {
            ((GemsPlugin)this.plugin).runForEntity(player, () -> this.onChatInput(player, message));
         } else {
            Bukkit.getScheduler().runTask(this.plugin, () -> this.onChatInput(player, message));
         }
      }

   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      this.resetPlayerState(player);
      this.openPayGUIs.remove(player);
      this.currentPages.remove(player);
   }

   public void reloadPayConfig() {
      this.closeAllOpenGUIs();
      this.loadPayConfig();
   }

   private void closeAllOpenGUIs() {
      for(Player player : new ArrayList(this.openPayGUIs.keySet())) {
         if (player.isOnline()) {
            if (this.plugin instanceof GemsPlugin) {
               GemsPlugin var10000 = (GemsPlugin)this.plugin;
               Objects.requireNonNull(player);
               var10000.runForEntity(player, player::closeInventory);
            } else {
               player.closeInventory();
            }
         }
      }

      this.openPayGUIs.clear();
      this.currentPages.clear();
      this.playerLists.clear();
   }

   public String getGUITitle() {
      return HexUtils.colorize(this.payConfig.getString("gui.title", "&8ꜱʜᴀʀᴅ (Page %current_page%)"));
   }

   public int getGUISize() {
      int rows = this.payConfig.getInt("gui.rows", 6);
      return rows * 9;
   }

   public boolean isBackgroundFillEnabled() {
      return this.payConfig.getBoolean("background.fill", false);
   }

   public ItemStack getBackgroundItem() {
      String materialName = this.payConfig.getString("background.material", "BLACK_STAINED_GLASS_PANE");

      Material material;
      try {
         material = Material.valueOf(materialName);
      } catch (IllegalArgumentException var8) {
         material = Material.BLACK_STAINED_GLASS_PANE;
      }

      String name = HexUtils.colorize(this.payConfig.getString("background.name", ""));
      List<String> lore = this.payConfig.getStringList("background.lore");
      List<String> coloredLore = new ArrayList();

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
         return this.payConfig.isString("background.slots") ? this.parseSlotRanges(this.payConfig.getString("background.slots", "")) : this.payConfig.getIntegerList("background.slots");
      }
   }

   private List<Integer> parseSlotRanges(String slotRanges) {
      List<Integer> slots = new ArrayList();
      if (slotRanges != null && !slotRanges.isEmpty()) {
         try {
            for(String range : slotRanges.split(",")) {
               range = range.trim();
               if (range.contains("-")) {
                  String[] parts = range.split("-");
                  if (parts.length == 2) {
                     int start = Integer.parseInt(parts[0].trim());
                     int end = Integer.parseInt(parts[1].trim());

                     for(int i = start; i <= end; ++i) {
                        slots.add(i);
                     }
                  }
               } else {
                  slots.add(Integer.parseInt(range));
               }
            }
         } catch (NumberFormatException var11) {
         }

         return slots;
      } else {
         return slots;
      }
   }

   public ItemStack getItem(String itemKey, Player player, int currentPage, int totalPages, int totalPlayers) {
      String path = "items." + itemKey;
      if (!this.payConfig.contains(path)) {
         return null;
      } else {
         String materialName = this.payConfig.getString(path + ".material", "STONE");

         Material material;
         try {
            material = Material.valueOf(materialName);
         } catch (IllegalArgumentException var17) {
            material = Material.STONE;
         }

         String name = this.payConfig.getString(path + ".name", " ");
         List<String> lore = this.payConfig.getStringList(path + ".lore");
         Map<String, String> placeholders = new HashMap();
         placeholders.put("current_page", String.valueOf(currentPage));
         placeholders.put("total_pages", String.valueOf(totalPages));
         placeholders.put("total_players", String.valueOf(totalPlayers));
         if (player != null) {
            int gems = 0;
            if (this.plugin instanceof GemsPlugin) {
               gems = ((GemsPlugin)this.plugin).getDatabaseManager().getGems(player.getUniqueId());
            }

            placeholders.put("player", player.getName());
            placeholders.put("shards", String.valueOf(gems));
         }

         name = PlaceholderManager.replacePlaceholders(name, placeholders);
         List<String> formattedLore = new ArrayList();

         for(String line : lore) {
            formattedLore.add(HexUtils.colorize(PlaceholderManager.replacePlaceholders(line, placeholders)));
         }

         ItemStack item = new ItemStack(material, 1);
         if (material == Material.PLAYER_HEAD && this.payConfig.contains(path + ".skull-owner")) {
            SkullMeta skullMeta = (SkullMeta)item.getItemMeta();
            if (skullMeta != null) {
               String skullOwner = this.payConfig.getString(path + ".skull-owner");
               if (skullOwner != null && skullOwner.equals("%player%") && player != null) {
                  skullMeta.setOwningPlayer(player);
               } else if (skullOwner != null && !skullOwner.equals("%player%")) {
                  OfflinePlayer skullPlayer = Bukkit.getOfflinePlayer(skullOwner);
                  skullMeta.setOwningPlayer(skullPlayer);
               }

               skullMeta.setDisplayName(HexUtils.colorize(name));
               skullMeta.setLore(formattedLore);
               item.setItemMeta(skullMeta);
            }
         } else {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               meta.setDisplayName(HexUtils.colorize(name));
               meta.setLore(formattedLore);
               item.setItemMeta(meta);
            }
         }

         return item;
      }
   }

   public int getItemSlot(String itemKey) {
      return this.payConfig.getInt("items." + itemKey + ".slot", -1);
   }

   public boolean hasItem(String itemKey) {
      return this.payConfig.contains("items." + itemKey);
   }

   private List<OfflinePlayer> getAllPlayers() {
      List<OfflinePlayer> allPlayers = new ArrayList(Bukkit.getOnlinePlayers());
      File playerDataFolder = new File(this.plugin.getDataFolder(), "playerdata");
      File[] playerFiles;
      if (playerDataFolder.exists() && (playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"))) != null) {
         for(File playerFile : playerFiles) {
            try {
               YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
               String uuidString = config.getString("uuid");
               if (uuidString != null) {
                  UUID uuid = UUID.fromString(uuidString);
                  OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                  boolean alreadyExists = allPlayers.stream().anyMatch((p) -> p.getUniqueId().equals(uuid));
                  if (!alreadyExists) {
                     allPlayers.add(offlinePlayer);
                  }
               }
            } catch (Exception var13) {
            }
         }
      }

      return allPlayers;
   }

   private void setConfiguredItem(Inventory gui, String itemKey, Player player, int currentPage, int totalPages, int totalPlayers) {
      if (this.hasItem(itemKey)) {
         int slot = this.getItemSlot(itemKey);
         ItemStack item = this.getItem(itemKey, player, currentPage, totalPages, totalPlayers);
         if (slot >= 0 && slot < gui.getSize() && item != null) {
            gui.setItem(slot, item);
         }
      }

   }

   public void closePayGUI(Player player) {
      this.resetPlayerState(player);
      this.openPayGUIs.remove(player);
      this.currentPages.remove(player);
   }

   public boolean isPayGUI(Inventory inventory) {
      return this.openPayGUIs.containsValue(inventory);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getClickedInventory() != null) {
         if (this.isPayGUI(event.getInventory())) {
            event.setCancelled(true);
            HumanEntity var3 = event.getWhoClicked();
            if (var3 instanceof Player) {
               Player player = (Player)var3;
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
      if (this.isPayGUI(event.getInventory())) {
         event.setCancelled(true);
      }

   }

   private void handleButtonClick(Player player, int slot, ItemStack item) {
      try {
         if (this.hasItem("back-page") && slot == this.getItemSlot("back-page")) {
            if (this.plugin instanceof GemsPlugin) {
               ((GemsPlugin)this.plugin).getViewManager().openGUI(player);
            }

            return;
         }

         if (this.hasItem("refresh") && slot == this.getItemSlot("refresh")) {
            int currentPage = (Integer)this.currentPages.getOrDefault(player, 1);
            this.openPayGUI(player, currentPage);
            return;
         }

         if (this.hasItem("previous-page") && slot == this.getItemSlot("previous-page")) {
            int currentPage = (Integer)this.currentPages.getOrDefault(player, 1);
            if (currentPage > 1) {
               this.openPayGUI(player, currentPage - 1);
            }

            return;
         }

         if (this.hasItem("next-page") && slot == this.getItemSlot("next-page")) {
            int currentPage = (Integer)this.currentPages.getOrDefault(player, 1);
            List<OfflinePlayer> allPlayers = this.getAllPlayers();
            allPlayers.removeIf((offlinePlayer) -> offlinePlayer.getUniqueId().equals(player.getUniqueId()));
            int totalPages = (int)Math.ceil((double)allPlayers.size() / (double)45.0F);
            if (totalPages < 1) {
               totalPages = 1;
            }

            if (currentPage < totalPages) {
               this.openPayGUI(player, currentPage + 1);
            }

            return;
         }

         if (slot >= 0 && slot < 45) {
            this.handlePlayerSelection(player, slot, item);
         }
      } catch (Exception var7) {
      }

   }

   private void handlePlayerSelection(Player player, int slot, ItemStack item) {
      if (item != null && item.getType() == Material.PLAYER_HEAD) {
         if (!(Boolean)this.awaitingChatInput.getOrDefault(player, false)) {
            List<OfflinePlayer> playerList = (List)this.playerLists.get(player);
            if (playerList != null) {
               int currentPage = (Integer)this.currentPages.getOrDefault(player, 1);
               int startIndex = Math.max(0, (currentPage - 1) * 45);
               int playerIndex = startIndex + slot;
               if (playerIndex >= 0 && playerIndex < playerList.size()) {
                  OfflinePlayer targetPlayer = (OfflinePlayer)playerList.get(playerIndex);
                  this.startChatInput(player, targetPlayer);
               }

            }
         }
      }
   }

   private void logException(Throwable t, String context) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      this.plugin.getLogger().severe(context + " - " + String.valueOf(sw));
   }
}
