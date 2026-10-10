package de.elivb.shards.Command;

import de.elivb.shards.GemsPlugin;
import de.elivb.shards.Manager.LangManager;
import de.elivb.shards.Utlis.HexUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class GemsCommand implements TabExecutor {
   private final GemsPlugin plugin;
   private final LangManager langManager;
   private final Map<UUID, String> playerNameCache = new ConcurrentHashMap();
   private long lastPlayerListRefresh = 0L;
   private List<String> cachedPlayerNames = null;

   public GemsCommand(GemsPlugin plugin) {
      this.plugin = plugin;
      this.langManager = plugin.getLangManager();
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.langManager.getMessage("player-only"));
            return true;
         } else {
            Player player = (Player)sender;
            if (!this.plugin.getViewManager().isGUIEnabled()) {
               this.handleBalanceCommand(player, args);
               return true;
            } else {
               this.plugin.getViewManager().openGUI(player);
               return true;
            }
         }
      } else if (args[0].equalsIgnoreCase("reload")) {
         if (!sender.hasPermission("shards.admin")) {
            sender.sendMessage(this.langManager.getMessage("no-permission"));
            return true;
         } else {
            this.reloadConfigs(sender);
            return true;
         }
      } else if (args[0].equalsIgnoreCase("upgrade")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.langManager.getMessage("player-only"));
            return true;
         } else if (!this.plugin.getGemManager().isLevelSystemEnabled()) {
            return true;
         } else {
            Player player = (Player)sender;
            this.plugin.getUpgradeGUI().openUpgradeGUI(player);
            return true;
         }
      } else if (args[0].equalsIgnoreCase("booster")) {
         if (!sender.hasPermission("shards.admin")) {
            sender.sendMessage(this.langManager.getMessage("no-permission"));
            return true;
         } else if (args.length < 4) {
            return true;
         } else {
            try {
               String targetName = args[1];
               int hours = Integer.parseInt(args[2]);
               int amount = Integer.parseInt(args[3]);
               Player target = Bukkit.getPlayer(targetName);
               if (target == null) {
                  sender.sendMessage(this.langManager.getMessage("player-not-found"));
                  return true;
               }

               if (hours <= 0 || amount <= 0) {
                  return true;
               }

               this.plugin.getShardBoosterManager().giveBooster(target, hours, amount);
            } catch (NumberFormatException var9) {
            }

            return true;
         }
      } else if (args[0].equalsIgnoreCase("clear")) {
         if (!sender.hasPermission("shards.admin")) {
            sender.sendMessage(this.langManager.getMessage("no-permission"));
            return true;
         } else if (args.length >= 2 && args[1].equalsIgnoreCase("all-player")) {
            int resetCount = this.resetAllPlayerData();
            String message = this.langManager.getMessage("clear-all-success");
            if (message != null && !message.isEmpty()) {
               message = message.replace("%count%", String.valueOf(resetCount));
               sender.sendMessage(HexUtils.colorize(message));
            }

            return true;
         } else {
            return true;
         }
      } else {
         if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("shards.admin")) {
               sender.sendMessage(this.langManager.getMessage("no-permission"));
               return true;
            }

            if (args.length < 3) {
               sender.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage"));
               return true;
            }

            switch (args[0].toLowerCase()) {
               case "give":
                  this.handleGiveCommand(sender, args[1], args[2]);
                  return true;
               case "take":
                  this.handleTakeCommand(sender, args[1], args[2]);
                  return true;
               case "set":
                  this.handleSetCommand(sender, args[1], args[2]);
                  return true;
            }
         }

         if (!(sender instanceof Player)) {
            sender.sendMessage(this.langManager.getMessage("player-only"));
            return true;
         } else {
            Player player = (Player)sender;
            switch (args[0].toLowerCase()) {
               case "pay":
                  if (args.length == 1) {
                     if (!this.plugin.getPayGUI().isPayGUIEnabled()) {
                        return true;
                     }

                     this.plugin.getPayGUI().openPayGUI(player);
                     return true;
                  } else {
                     if (args.length >= 3) {
                        this.handlePayCommand(player, args[1], args[2]);
                        return true;
                     }

                     player.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage"));
                     return true;
                  }
               case "bal":
               case "balance":
                  this.handleBalanceCommand(player, args);
                  return true;
               case "help":
                  this.showGemsHelp(player);
                  return true;
               default:
                  this.showGemsHelp(player);
                  return true;
            }
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (args.length == 1) {
         ArrayList<String> subCommands = new ArrayList();
         subCommands.add("pay");
         subCommands.add("bal");
         subCommands.add("balance");
         subCommands.add("help");
         if (this.plugin.getGemManager().isLevelSystemEnabled()) {
            subCommands.add("upgrade");
         }

         if (sender.hasPermission("shards.admin")) {
            subCommands.add("give");
            subCommands.add("take");
            subCommands.add("set");
            subCommands.add("reload");
            subCommands.add("booster");
            subCommands.add("clear");
         }

         completions = (List)subCommands.stream().filter((sub) -> sub.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
      } else if (args.length == 2) {
         if (args[0].equalsIgnoreCase("reload") && !(sender instanceof Player)) {
            return completions;
         }

         if (args[0].equalsIgnoreCase("pay")) {
            completions = (List)this.getCachedPlayerNames().stream().filter((name) -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
         } else if ((args[0].equalsIgnoreCase("bal") || args[0].equalsIgnoreCase("balance")) && sender.hasPermission("shards.bal")) {
            completions = (List)this.getCachedPlayerNames().stream().filter((name) -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
         } else if (args[0].equalsIgnoreCase("booster") && sender.hasPermission("shards.admin")) {
            completions = (List)this.getCachedPlayerNames().stream().filter((name) -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
         } else if (args[0].equalsIgnoreCase("clear") && sender.hasPermission("shards.admin")) {
            completions.add("all-player");
         } else if (sender.hasPermission("shards.admin") && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("set"))) {
            completions = (List)this.getCachedPlayerNames().stream().filter((name) -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
         }
      } else if (args.length == 3) {
         if (!sender.hasPermission("shards.admin") || !args[0].equalsIgnoreCase("give") && !args[0].equalsIgnoreCase("take") && !args[0].equalsIgnoreCase("set")) {
            if (args[0].equalsIgnoreCase("pay")) {
               completions.add("10");
               completions.add("50");
               completions.add("100");
               completions.add("500");
            } else if (args[0].equalsIgnoreCase("booster") && sender.hasPermission("shards.admin")) {
               completions.add("1");
               completions.add("6");
               completions.add("12");
               completions.add("24");
               completions.add("48");
            }
         } else {
            completions.add("10");
            completions.add("50");
            completions.add("100");
            completions.add("500");
         }
      } else if (args.length == 4 && args[0].equalsIgnoreCase("booster") && sender.hasPermission("shards.admin")) {
         completions.add("2");
         completions.add("3");
         completions.add("4");
         completions.add("5");
         completions.add("10");
      }

      return completions;
   }

   private List<String> getCachedPlayerNames() {
      long now = System.currentTimeMillis();
      if (this.cachedPlayerNames == null || now - this.lastPlayerListRefresh > 30000L) {
         this.cachedPlayerNames = new ArrayList();

         for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            this.cachedPlayerNames.add(onlinePlayer.getName());
         }

         this.lastPlayerListRefresh = now;
      }

      return this.cachedPlayerNames;
   }

   private List<String> getAllPlayerNames() {
      return this.getCachedPlayerNames();
   }

   private void showGemsHelp(Player player) {
      player.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage"));
      if (player.hasPermission("shards.admin")) {
         player.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage-admin"));
      }

   }

   private void handlePayCommand(Player sender, String targetName, String amountStr) {
      if (!sender.hasPermission("shards.pay")) {
         sender.sendMessage(this.langManager.getMessage("no-permission"));
      } else {
         try {
            int amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
               sender.sendMessage(this.langManager.getMessage("invalid-number"));
               return;
            }

            Player target = Bukkit.getPlayer(targetName);
            OfflinePlayer offlineTarget = null;
            if (target == null && (offlineTarget = this.findOfflinePlayer(targetName)) == null) {
               sender.sendMessage(this.langManager.getMessage("player-not-found"));
               return;
            }

            if (sender.equals(target)) {
               sender.sendMessage(this.langManager.getMessage("self-pay"));
               return;
            }

            int senderBalance = this.plugin.getDatabaseManager().getGems(sender.getUniqueId());
            if (senderBalance < amount) {
               sender.sendMessage(this.langManager.getMessage("not-enough-shards"));
               return;
            }

            String targetDisplayName;
            UUID targetUUID;
            if (target != null) {
               targetUUID = target.getUniqueId();
               targetDisplayName = target.getName();
            } else {
               targetUUID = offlineTarget.getUniqueId();
               targetDisplayName = offlineTarget.getName();
            }

            boolean success = this.plugin.getDatabaseManager().transferGems(sender.getUniqueId(), targetUUID, amount);
            if (success) {
               int newSenderBalance = this.plugin.getDatabaseManager().getGems(sender.getUniqueId());
               int newTargetBalance = this.plugin.getDatabaseManager().getGems(targetUUID);
               String senderMessage = this.langManager.getMessage("shards-send");
               if (senderMessage != null && !senderMessage.isEmpty()) {
                  senderMessage = senderMessage.replace("%shards_send%", String.valueOf(amount)).replace("%player%", targetDisplayName);
                  int currentGems = this.plugin.getDatabaseManager().getGems(sender.getUniqueId());
                  senderMessage = senderMessage.replace("%shards%", String.valueOf(currentGems)).replace("%shards_send%", String.valueOf(amount));
                  sender.sendMessage(HexUtils.colorize(senderMessage));
               }

               String receiverMessage;
               if (target != null && target.isOnline() && (receiverMessage = this.langManager.getMessage("shards-received")) != null && !receiverMessage.isEmpty()) {
                  receiverMessage = receiverMessage.replace("%shards_received%", String.valueOf(amount)).replace("%target_player%", sender.getName());
                  int currentGems = this.plugin.getDatabaseManager().getGems(target.getUniqueId());
                  receiverMessage = receiverMessage.replace("%shards%", String.valueOf(currentGems)).replace("%shards_received%", String.valueOf(amount));
                  target.sendMessage(HexUtils.colorize(receiverMessage));
               }

               this.plugin.getViewManager().updateGUI(sender);
               if (target != null) {
                  this.plugin.getViewManager().updateGUI(target);
               }
            }
         } catch (NumberFormatException var16) {
            sender.sendMessage(this.langManager.getMessage("invalid-number"));
         } catch (Exception var17) {
         }

      }
   }

   private OfflinePlayer findOfflinePlayer(String playerName) {
      for(OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
         if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(playerName)) {
            return offlinePlayer;
         }
      }

      try {
         File playerDataFolder = new File(this.plugin.getDataFolder(), "playerdata");
         File[] playerFiles;
         if (playerDataFolder.exists() && (playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"))) != null) {
            for(File playerFile : playerFiles) {
               try {
                  YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                  String storedName = config.getString("name");
                  String uuidString = config.getString("uuid");
                  if (storedName != null && storedName.equalsIgnoreCase(playerName) && uuidString != null) {
                     UUID uuid = UUID.fromString(uuidString);
                     return Bukkit.getOfflinePlayer(uuid);
                  }
               } catch (Exception var12) {
               }
            }
         }
      } catch (Exception var13) {
      }

      return null;
   }

   private void handleBalanceCommand(Player player, String[] args) {
      try {
         if (args.length >= 2 && args[1] != null && !args[1].isEmpty()) {
            String targetName = args[1];
            if (!player.hasPermission("shards.bal")) {
               player.sendMessage(this.langManager.getMessage("no-permission"));
               return;
            }

            Player targetPlayer = Bukkit.getPlayer(targetName);
            OfflinePlayer offlineTarget = null;
            UUID targetUUID = null;
            String displayName;
            if (targetPlayer != null) {
               targetUUID = targetPlayer.getUniqueId();
               displayName = targetPlayer.getName();
            } else {
               offlineTarget = this.findOfflinePlayer(targetName);
               if (offlineTarget == null) {
                  player.sendMessage(this.langManager.getMessage("player-not-found"));
                  return;
               }

               targetUUID = offlineTarget.getUniqueId();
               displayName = offlineTarget.getName();
               if (displayName == null) {
                  displayName = targetName;
               }
            }

            int gems = this.plugin.getDatabaseManager().getGems(targetUUID);
            String balanceMessage = this.langManager.getMessage("shards-balance-other");
            if (balanceMessage != null && !balanceMessage.isEmpty()) {
               balanceMessage = balanceMessage.replace("%shards%", String.valueOf(gems)).replace("%player%", displayName).replace("%target%", displayName);
               player.sendMessage(HexUtils.colorize(balanceMessage));
            }
         } else {
            int gems = this.plugin.getDatabaseManager().getGems(player.getUniqueId());
            String balanceMessage = this.langManager.getMessage("shards-balance");
            if (balanceMessage != null && !balanceMessage.isEmpty()) {
               balanceMessage = balanceMessage.replace("%shards%", String.valueOf(gems)).replace("%player%", player.getName());
               player.sendMessage(HexUtils.colorize(balanceMessage));
            }
         }
      } catch (Exception exception) {
         exception.printStackTrace();
      }

   }

   private void handleGiveCommand(CommandSender admin, String targetName, String amountStr) {
      try {
         int amount = Integer.parseInt(amountStr);
         Player target = Bukkit.getPlayer(targetName);
         OfflinePlayer offlineTarget = null;
         if (target == null && (offlineTarget = this.findOfflinePlayer(targetName)) == null) {
            admin.sendMessage(this.langManager.getMessage("player-not-found"));
            return;
         }

         String targetDisplayName;
         UUID targetUUID;
         if (target != null) {
            targetUUID = target.getUniqueId();
            targetDisplayName = target.getName();
         } else {
            targetUUID = offlineTarget.getUniqueId();
            targetDisplayName = offlineTarget.getName();
         }

         this.plugin.getDatabaseManager().addGems(targetUUID, amount);
         int newTargetBalance = this.plugin.getDatabaseManager().getGems(targetUUID);
         String adminMessage = this.langManager.getMessage("shards-given");
         if (adminMessage != null && !adminMessage.isEmpty()) {
            adminMessage = adminMessage.replace("%shards_given%", String.valueOf(amount)).replace("%player%", targetDisplayName);
            admin.sendMessage(HexUtils.colorize(adminMessage));
         }

         String targetMessage;
         if (target != null && target.isOnline() && (targetMessage = this.langManager.getMessage("shards-received")) != null && !targetMessage.isEmpty()) {
            String adminName = admin instanceof Player ? admin.getName() : "Console";
            targetMessage = targetMessage.replace("%shards_received%", String.valueOf(amount)).replace("%target_player%", adminName);
            int currentGems = this.plugin.getDatabaseManager().getGems(target.getUniqueId());
            targetMessage = targetMessage.replace("%shards", String.valueOf(currentGems)).replace("%shards_received%", String.valueOf(amount));
            target.sendMessage(HexUtils.colorize(targetMessage));
         }

         if (target != null) {
            this.plugin.getViewManager().updateGUI(target);
         }
      } catch (NumberFormatException var14) {
         admin.sendMessage(this.langManager.getMessage("invalid-number"));
      } catch (Exception var15) {
      }

   }

   private void handleTakeCommand(CommandSender admin, String targetName, String amountStr) {
      try {
         int amount = Integer.parseInt(amountStr);
         Player target = Bukkit.getPlayer(targetName);
         OfflinePlayer offlineTarget = null;
         if (target == null && (offlineTarget = this.findOfflinePlayer(targetName)) == null) {
            admin.sendMessage(this.langManager.getMessage("player-not-found"));
            return;
         }

         String targetDisplayName;
         UUID targetUUID;
         if (target != null) {
            targetUUID = target.getUniqueId();
            targetDisplayName = target.getName();
         } else {
            targetUUID = offlineTarget.getUniqueId();
            targetDisplayName = offlineTarget.getName();
         }

         this.plugin.getDatabaseManager().removeGems(targetUUID, amount);
         String adminMessage = this.langManager.getMessage("shards-taken");
         if (adminMessage != null && !adminMessage.isEmpty()) {
            adminMessage = adminMessage.replace("%shards_taken%", String.valueOf(amount)).replace("%player%", targetDisplayName);
            admin.sendMessage(HexUtils.colorize(adminMessage));
         }

         if (target != null) {
            this.plugin.getViewManager().updateGUI(target);
         }
      } catch (NumberFormatException var10) {
         admin.sendMessage(this.langManager.getMessage("invalid-number"));
      } catch (Exception var11) {
      }

   }

   private void handleSetCommand(CommandSender admin, String targetName, String amountStr) {
      try {
         int amount = Integer.parseInt(amountStr);
         Player target = Bukkit.getPlayer(targetName);
         OfflinePlayer offlineTarget = null;
         if (target == null && (offlineTarget = this.findOfflinePlayer(targetName)) == null) {
            admin.sendMessage(this.langManager.getMessage("player-not-found"));
            return;
         }

         String targetDisplayName;
         UUID targetUUID;
         if (target != null) {
            targetUUID = target.getUniqueId();
            targetDisplayName = target.getName();
         } else {
            targetUUID = offlineTarget.getUniqueId();
            targetDisplayName = offlineTarget.getName();
         }

         this.plugin.getDatabaseManager().setGems(targetUUID, amount);
         String adminMessage = this.langManager.getMessage("shards-set");
         if (adminMessage != null && !adminMessage.isEmpty()) {
            adminMessage = adminMessage.replace("%shards_set%", String.valueOf(amount)).replace("%player%", targetDisplayName);
            admin.sendMessage(HexUtils.colorize(adminMessage));
         }

         if (target != null) {
            this.plugin.getViewManager().updateGUI(target);
         }
      } catch (NumberFormatException var10) {
         admin.sendMessage(this.langManager.getMessage("invalid-number"));
      } catch (Exception var11) {
      }

   }

   private void reloadConfigs(CommandSender sender) {
      this.langManager.reloadLangConfig();
      this.plugin.getViewManager().reloadViewConfig();
      this.plugin.reloadPluginConfig();
      this.plugin.getPayGUI().reloadPayConfig();
      this.plugin.getUpgradeGUI().reloadUpgradeConfig();
      this.plugin.getGemManager().reloadLevelConfig();
      this.plugin.getSoundManager().reload();
      if (sender instanceof Player player) {
         this.plugin.getSoundManager().playReloadSound(player);
      }

      String BRIGHT_CYAN = "\u001b[96m";
      String BRIGHT_RED = "\u001b[91m";
      String RESET = "\u001b[0m";
      this.plugin.getLogger().info("\u001b[96mReloading configuration...\u001b[0m");
      this.plugin.getLogger().info("\u001b[91m╠ Reloaded lang.yml!\u001b[0m");
      this.plugin.getLogger().info("\u001b[91m╠ Reloaded config.yml\u001b[0m");
      this.plugin.getLogger().info("\u001b[91m╠ Reloaded level.yml\u001b[0m");
      this.plugin.getLogger().info("\u001b[91m╠ Reloaded GUI\u001b[0m");
      this.plugin.getLogger().info("\u001b[96m╚ Successful reload!\u001b[0m");
      sender.sendMessage(this.langManager.getMessage("reload-success"));
      this.cachedPlayerNames = null;
      this.playerNameCache.clear();
      this.lastPlayerListRefresh = 0L;
   }

   private int resetAllPlayerData() {
      int resetCount = 0;

      try {
         File playerDataFolder = new File(this.plugin.getDataFolder(), "playerdata");
         if (!playerDataFolder.exists()) {
            return 0;
         }

         File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
         if (playerFiles == null) {
            return 0;
         }

         for(File playerFile : playerFiles) {
            try {
               YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
               if (config.contains("shards") || config.contains("shards_level")) {
                  config.set("shards", 0);
                  config.set("shards_level", 1);
                  config.save(playerFile);
                  ++resetCount;
                  String var9 = config.getString("name", "Unknown");
               }
            } catch (Exception var10) {
            }
         }

         for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            this.plugin.getGemManager().setGems(onlinePlayer, 0);
            this.plugin.getDatabaseManager().setGemLevel(onlinePlayer.getUniqueId(), 1);
            this.plugin.getViewManager().updateGUI(onlinePlayer);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return resetCount;
   }
}

