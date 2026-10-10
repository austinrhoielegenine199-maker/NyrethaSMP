package com.nyretha.tpa;

import com.nyretha.tpa.Manager.ConfigManager;
import com.nyretha.tpa.Manager.DataManager;
import com.nyretha.tpa.Manager.GUIManager;
import com.nyretha.tpa.Manager.GUIToggleManager;
import com.nyretha.tpa.Manager.LanguageManager;
import com.nyretha.tpa.Manager.SoundManager;
import com.nyretha.tpa.Manager.TPARequest;
import com.nyretha.tpa.Manager.TPAutoManager;
import com.nyretha.tpa.gui.TPAAcceptGUI;
import com.nyretha.tpa.gui.TPAHereAcceptGUI;
import com.nyretha.tpa.gui.TPAHereSendGUI;
import com.nyretha.tpa.gui.TPASendGUI;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TPA extends JavaPlugin implements Listener {
   private HashMap<UUID, TPARequest> pendingRequests;
   private HashMap<UUID, Long> cooldowns;
   private HashSet<UUID> disabledPlayers;
   private HashSet<UUID> disabledTpaherePlayers;
   private HashMap<UUID, Location> teleportLocations;
   private HashMap<UUID, ScheduledTask> teleportTasks;
   private HashMap<UUID, ScheduledTask> timeoutTasks;
   private ConfigManager configManager;
   private LanguageManager languageManager;
   private GUIManager guiManager;
   private SoundManager soundManager;
   private DataManager dataManager;

   private GUIToggleManager guiToggleManager;
   private TPAutoManager tpaAutoManager;

   public void onEnable() {
      this.pendingRequests = new HashMap();
      this.cooldowns = new HashMap();
      this.disabledPlayers = new HashSet();
      this.disabledTpaherePlayers = new HashSet();
      this.teleportLocations = new HashMap();
      this.teleportTasks = new HashMap();
      this.timeoutTasks = new HashMap();

      
         if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
         }

         this.configManager = new ConfigManager(this);
         this.languageManager = new LanguageManager(this);
         this.guiManager = new GUIManager(this);
         this.soundManager = new SoundManager(this);
         this.dataManager = new DataManager(this);
         this.guiToggleManager = new GUIToggleManager(this);
         this.tpaAutoManager = new TPAutoManager(this);
         this.configManager.loadConfig();
         this.languageManager.loadMessages();
         this.guiManager.loadGUIConfigs();
         this.soundManager.loadSounds();
         this.dataManager.loadToggleStatus(this.disabledPlayers, this.disabledTpaherePlayers);
         this.guiToggleManager.loadGuiModes();
         this.tpaAutoManager.loadAutoAcceptStatus();
         this.getServer().getPluginManager().registerEvents(this, this);
      
   }

   public void onDisable() {
      for(UUID playerId : this.disabledPlayers) {
         this.dataManager.updateTPAStatus(playerId, true);
      }

      for(UUID playerId : this.disabledTpaherePlayers) {
         this.dataManager.updateTPAHereStatus(playerId, true);
      }

      for(UUID playerId : this.guiToggleManager.getGuiModePlayers()) {
         this.dataManager.updateGuiModeStatus(playerId, true, false);
      }

      for(UUID playerId : this.guiToggleManager.getGuiModeTpaHerePlayers()) {
         this.dataManager.updateGuiModeStatus(playerId, false, true);
      }

      for(UUID playerId : this.tpaAutoManager.getAutoAcceptPlayers()) {
         this.dataManager.updateAutoAcceptStatus(playerId, true);
      }

      for(UUID playerId : this.tpaAutoManager.getAutoAcceptHerePlayers()) {
         this.dataManager.updateAutoAcceptHereStatus(playerId, true);
      }

      for(ScheduledTask task : this.teleportTasks.values()) {
         if (task != null && !task.isCancelled()) {
            task.cancel();
         }
      }

      for(ScheduledTask task : this.timeoutTasks.values()) {
         if (task != null && !task.isCancelled()) {
            task.cancel();
         }
      }

      this.pendingRequests.clear();
      this.cooldowns.clear();
      this.disabledPlayers.clear();
      this.disabledTpaherePlayers.clear();
      this.teleportLocations.clear();
      this.teleportTasks.clear();
      this.timeoutTasks.clear();
      this.dataManager.closeConnection();
   }

   

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID playerId = player.getUniqueId();
      boolean tpaDisabled = this.disabledPlayers.contains(playerId);
      boolean tpahereDisabled = this.disabledTpaherePlayers.contains(playerId);
      this.dataManager.saveToggleStatus(playerId, tpaDisabled, tpahereDisabled);
      boolean tpaGui = this.guiToggleManager.isGuiModeEnabled(player, false);
      boolean tpahereGui = this.guiToggleManager.isGuiModeEnabled(player, true);
      this.dataManager.updateGuiModeStatus(playerId, tpaGui, tpahereGui);
      boolean auto = this.tpaAutoManager.isAutoAcceptEnabled(player);
      boolean autoHere = this.tpaAutoManager.isAutoAcceptHereEnabled(player);
      this.dataManager.updateAutoAcceptStatus(playerId, auto);
      this.dataManager.updateAutoAcceptHereStatus(playerId, autoHere);
      this.pendingRequests.values().removeIf((request) -> request.getSender().equals(playerId) || request.getTarget().equals(playerId));
      ScheduledTask timeoutTask = (ScheduledTask)this.timeoutTasks.remove(playerId);
      if (timeoutTask != null && !timeoutTask.isCancelled()) {
         timeoutTask.cancel();
      }

      ScheduledTask teleportTask = (ScheduledTask)this.teleportTasks.remove(playerId);
      if (teleportTask != null && !teleportTask.isCancelled()) {
         teleportTask.cancel();
      }

      this.teleportLocations.remove(playerId);
      this.guiToggleManager.removePlayer(playerId);
      this.tpaAutoManager.removePlayer(playerId);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      String cmd = command.getName().toLowerCase();
      if (cmd.equals("tpa")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else if (args.length != 1) {
            this.sendUsage(sender);
            return true;
         } else {
            this.handleTPARequest((Player)sender, args[0], false);
            return true;
         }
      } else if (cmd.equals("tpahere")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else if (args.length != 1) {
            this.sendUsage(sender);
            return true;
         } else {
            this.handleTPARequest((Player)sender, args[0], true);
            return true;
         }
      } else if (cmd.equals("tpaccept")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPAAccept((Player)sender);
            return true;
         }
      } else if (cmd.equals("tpadeny")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPADeny((Player)sender);
            return true;
         }
      } else if (cmd.equals("tpacancel")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPACancel((Player)sender);
            return true;
         }
      } else if (cmd.equals("tpatoggle")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPAToggle((Player)sender);
            return true;
         }
      } else if (cmd.equals("tpaheretoggle")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPAHereToggle((Player)sender);
            return true;
         }
      } else if (cmd.equals("tpareload")) {
         if (sender.hasPermission("tpa.admin")) {
            this.reloadConfigs();
            if (sender instanceof Player) {
               this.soundManager.playReloadSound((Player)sender);
            }

            this.sendActionBarIfPossible(sender, this.languageManager.getActionBar("reload-success"));
            sender.sendMessage(this.languageManager.getMessage("reload-success"));
         } else {
            sender.sendMessage(this.languageManager.getMessage("no-permission"));
            this.sendUsage(sender);
         }

         return true;
      } else if (cmd.equals("tpagui")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPAGUIToggle((Player)sender, false);
            return true;
         }
      } else if (cmd.equals("tpaheregui")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPAGUIToggle((Player)sender, true);
            return true;
         }
      } else if (cmd.equals("tpauto")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPAuto((Player)sender);
            return true;
         }
      } else if (cmd.equals("tpahereauto")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(this.languageManager.getMessage("player-only"));
            return true;
         } else {
            this.handleTPAHereAuto((Player)sender);
            return true;
         }
      } else {
         this.sendUsage(sender);
         return true;
      }
   }

   private void sendUsage(CommandSender sender) {
      String usage = this.languageManager.getCommandUsage();
      if (usage != null) {
         sender.sendMessage(usage);
      }

      if (sender.hasPermission("tpa.admin")) {
         String adminUsage = this.languageManager.getAdminCommandUsage();
         if (adminUsage != null) {
            sender.sendMessage(adminUsage);
         }
      }

   }

   private void sendClickableRequestMessage(Player target, Player sender, boolean isTpaHere) {
      String messageKey = isTpaHere ? "tpahere-received-clickable" : "tpa-received-clickable";
      String message = this.languageManager.getMessageWithoutPrefix(messageKey).replace("%player%", sender.getName());
      TextComponent mainComponent = new TextComponent(Hex.translateAllColorCodes(message));
      String acceptText = this.languageManager.getMessageWithoutPrefix("clickable-accept");
      String acceptHover = this.languageManager.getMessageWithoutPrefix("clickable-accept-hover");
      TextComponent acceptComponent = new TextComponent(acceptText);
      acceptComponent.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/tpaccept"));
      acceptComponent.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Content[]{new Text(acceptHover)}));
      String denyText = this.languageManager.getMessageWithoutPrefix("clickable-deny");
      String denyHover = this.languageManager.getMessageWithoutPrefix("clickable-deny-hover");
      TextComponent denyComponent = new TextComponent(denyText);
      denyComponent.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/tpadeny"));
      denyComponent.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Content[]{new Text(denyHover)}));
      mainComponent.addExtra(" ");
      mainComponent.addExtra(acceptComponent);
      mainComponent.addExtra(" ");
      mainComponent.addExtra(denyComponent);
      target.spigot().sendMessage(mainComponent);
   }

   private void handleTPARequest(Player sender, String targetName, boolean isTpaHere) {
      Player target = Bukkit.getPlayer(targetName);
      if (target == null) {
         sender.sendMessage(this.languageManager.getMessage("player-offline"));
      } else if (sender.equals(target)) {
         sender.sendMessage(this.languageManager.getMessage("self-request"));
      } else {
         if (isTpaHere) {
            if (this.disabledTpaherePlayers.contains(target.getUniqueId())) {
               sender.sendMessage(this.languageManager.getMessage("player-disabled-tpahere"));
               return;
            }
         } else if (this.disabledPlayers.contains(target.getUniqueId())) {
            sender.sendMessage(this.languageManager.getMessage("player-disabled"));
            return;
         }

         boolean useGui = this.guiToggleManager.isGuiModeEnabled(sender, isTpaHere);
         if (useGui) {
            if (isTpaHere) {
               TPAHereSendGUI gui = new TPAHereSendGUI(this, sender, target);
               gui.open();
            } else {
               TPASendGUI gui = new TPASendGUI(this, sender, target);
               gui.open();
            }
         } else {
            this.sendTPARequestDirect(sender, target, isTpaHere);
         }
      }

   }

   private void handleTPAAccept(Player target) {
      TPARequest request = (TPARequest)this.pendingRequests.get(target.getUniqueId());
      if (request == null) {
         target.sendMessage(this.languageManager.getMessage("no-pending-requests"));
      } else {
         Player sender = Bukkit.getPlayer(request.getSender());
         if (sender == null || !sender.isOnline()) {
            target.sendMessage(this.languageManager.getMessage("sender-offline"));
            this.pendingRequests.remove(target.getUniqueId());
            return;
         }

         boolean useGui = this.guiToggleManager.isGuiModeEnabled(target, request.isTpaHere());
         if (useGui) {
            if (!request.isTpaHere()) {
               TPAAcceptGUI gui = new TPAAcceptGUI(this, target, request);
               gui.open();
            } else {
               TPAHereAcceptGUI gui = new TPAHereAcceptGUI(this, target, request);
               gui.open();
            }
         } else {
            this.performTeleportWithDelay(request, sender, target);
            this.pendingRequests.remove(target.getUniqueId());
            ScheduledTask timeoutTask = (ScheduledTask)this.timeoutTasks.remove(target.getUniqueId());
            if (timeoutTask != null) {
               timeoutTask.cancel();
            }

            this.soundManager.playButtonClickSound(target);
            this.sendActionBarIfPossible(target, this.languageManager.getActionBar("request-accepted"));
            target.sendMessage(this.languageManager.getMessage("request-accepted"));
         }
      }

   }

   public void handleTPAAcceptDirect(Player target, TPARequest request) {
      Player sender = Bukkit.getPlayer(request.getSender());
      if (sender != null && sender.isOnline()) {
         this.performTeleportWithDelay(request, sender, target);
         this.pendingRequests.remove(target.getUniqueId());
         ScheduledTask timeoutTask = (ScheduledTask)this.timeoutTasks.remove(target.getUniqueId());
         if (timeoutTask != null) {
            timeoutTask.cancel();
         }

         this.soundManager.playButtonClickSound(target);
         this.sendActionBarIfPossible(target, this.languageManager.getActionBar("request-accepted"));
         target.sendMessage(this.languageManager.getMessage("request-accepted"));
      } else {
         target.sendMessage(this.languageManager.getMessage("sender-offline"));
         this.pendingRequests.remove(target.getUniqueId());
      }

   }

   public void handleTPAHereAcceptDirect(Player target, TPARequest request) {
      Player sender = Bukkit.getPlayer(request.getSender());
      if (sender != null && sender.isOnline()) {
         this.performTeleportWithDelay(request, sender, target);
         this.pendingRequests.remove(target.getUniqueId());
         ScheduledTask timeoutTask = (ScheduledTask)this.timeoutTasks.remove(target.getUniqueId());
         if (timeoutTask != null) {
            timeoutTask.cancel();
         }

         this.soundManager.playButtonClickSound(target);
         this.sendActionBarIfPossible(target, this.languageManager.getActionBar("request-accepted"));
         target.sendMessage(this.languageManager.getMessage("request-accepted"));
      } else {
         target.sendMessage(this.languageManager.getMessage("sender-offline"));
         this.pendingRequests.remove(target.getUniqueId());
      }

   }

   public void handleTPADenyDirect(Player target, TPARequest request) {
      Player sender = Bukkit.getPlayer(request.getSender());
      if (sender != null && sender.isOnline()) {
         sender.sendMessage(this.languageManager.getMessage("request-denied"));
      }

      target.sendMessage(this.languageManager.getMessage("request-denied"));
      this.soundManager.playButtonClickSound(target);
      this.sendActionBarIfPossible(target, this.languageManager.getActionBar("request-denied"));
      this.pendingRequests.remove(target.getUniqueId());
      ScheduledTask timeoutTask = (ScheduledTask)this.timeoutTasks.remove(target.getUniqueId());
      if (timeoutTask != null) {
         timeoutTask.cancel();
      }

   }

   public void sendTPARequestDirect(Player sender, Player target, boolean isTpaHere) {
      if (isTpaHere) {
         if (this.tpaAutoManager.isAutoAcceptHereEnabled(target)) {
            TPARequest request = new TPARequest(sender.getUniqueId(), target.getUniqueId(), isTpaHere);
            this.performTeleportWithDelay(request, sender, target);
            sender.sendMessage(this.languageManager.getMessage("tpahere-auto-accepted").replace("%player%", target.getName()));
            target.sendMessage(this.languageManager.getMessage("tpahere-auto-accepted-target").replace("%player%", sender.getName()));
            this.soundManager.playTeleportSuccessSound(sender);
            this.soundManager.playTeleportSuccessSound(target);
            return;
         }
      } else if (this.tpaAutoManager.isAutoAcceptEnabled(target)) {
         TPARequest request = new TPARequest(sender.getUniqueId(), target.getUniqueId(), isTpaHere);
         this.performTeleportWithDelay(request, sender, target);
         sender.sendMessage(this.languageManager.getMessage("tpa-auto-accepted").replace("%player%", target.getName()));
         target.sendMessage(this.languageManager.getMessage("tpa-auto-accepted-target").replace("%player%", sender.getName()));
         this.soundManager.playTeleportSuccessSound(sender);
         this.soundManager.playTeleportSuccessSound(target);
         return;
      }

      if (isTpaHere) {
         if (this.disabledTpaherePlayers.contains(target.getUniqueId())) {
            sender.sendMessage(this.languageManager.getMessage("player-disabled-tpahere"));
            return;
         }
      } else if (this.disabledPlayers.contains(target.getUniqueId())) {
         sender.sendMessage(this.languageManager.getMessage("player-disabled"));
         return;
      }

      TPARequest existingRequest = (TPARequest)this.pendingRequests.get(target.getUniqueId());
      if (existingRequest != null && existingRequest.getSender().equals(sender.getUniqueId())) {
         sender.sendMessage(this.languageManager.getMessage("already-teleporting"));
      } else {
         this.pendingRequests.values().removeIf((requestx) -> requestx.getSender().equals(sender.getUniqueId()));
         TPARequest request = new TPARequest(sender.getUniqueId(), target.getUniqueId(), isTpaHere);
         this.pendingRequests.put(target.getUniqueId(), request);
         if (isTpaHere) {
            sender.sendMessage(this.languageManager.getMessage("tpahere-sent").replace("%player%", target.getName()));
            this.sendClickableRequestMessage(target, sender, true);
         } else {
            sender.sendMessage(this.languageManager.getMessage("tpa-sent").replace("%player%", target.getName()));
            this.sendClickableRequestMessage(target, sender, false);
         }

         this.soundManager.playButtonClickSound(sender);
         int timeout = this.configManager.getRequestTimeout();
         ScheduledTask timeoutTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
            if (this.pendingRequests.containsValue(request)) {
               this.pendingRequests.remove(target.getUniqueId());
               sender.sendMessage(this.languageManager.getMessage("request-expired-sender").replace("%player%", target.getName()));
               target.sendMessage(this.languageManager.getMessage("request-expired-receiver").replace("%player%", sender.getName()));
            }

            task.cancel();
         }, (long)timeout * 20L, (long)timeout * 20L);
         this.timeoutTasks.put(target.getUniqueId(), timeoutTask);
      }
   }

   private void performTeleportWithDelay(final TPARequest request, final Player sender, final Player target) {
      final int teleportDelay = this.configManager.getTeleportDelay();
      if (teleportDelay <= 0) {
         this.executeTeleport(request, sender, target);
      } else {
         final Player playerToTeleport = request.isTpaHere() ? target : sender;
         if (this.teleportTasks.containsKey(playerToTeleport.getUniqueId())) {
            playerToTeleport.sendMessage(this.languageManager.getMessage("already-teleporting"));
            this.soundManager.playTeleportFailSound(playerToTeleport);
         } else {
            if (this.configManager.isCancelOnMove()) {
               this.teleportLocations.put(playerToTeleport.getUniqueId(), playerToTeleport.getLocation().clone());
            }

            ScheduledTask teleportTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, new Consumer<ScheduledTask>() {
               int countdown = teleportDelay;

               public void accept(ScheduledTask task) {
                  if (!playerToTeleport.isOnline()) {
                     TPA.this.cancelTeleport(playerToTeleport);
                     task.cancel();
                  } else if (TPA.this.configManager.isCancelOnMove() && TPA.this.hasPlayerMoved(playerToTeleport)) {
                     TPA.this.cancelTeleport(playerToTeleport);
                     task.cancel();
                  } else if (this.countdown <= 0) {
                     TPA.this.executeTeleport(request, sender, target);
                     TPA.this.teleportLocations.remove(playerToTeleport.getUniqueId());
                     TPA.this.teleportTasks.remove(playerToTeleport.getUniqueId());
                     task.cancel();
                  } else {
                     if (this.countdown <= 5) {
                        String countdownActionBar = TPA.this.languageManager.getActionBar("teleport-started").replace("%count%", String.valueOf(this.countdown));
                        TPA.this.sendActionBarIfPossible(playerToTeleport, countdownActionBar);
                        playerToTeleport.sendMessage(TPA.this.languageManager.getMessage("teleport-started").replace("%count%", String.valueOf(this.countdown)));
                        TPA.this.soundManager.playTeleportingSound(playerToTeleport);
                     }

                     --this.countdown;
                  }

               }
            }, 1L, 20L);
            this.teleportTasks.put(playerToTeleport.getUniqueId(), teleportTask);
         }
      }

   }

   private boolean hasPlayerMoved(Player player) {
      Location originalLocation = (Location)this.teleportLocations.get(player.getUniqueId());
      if (originalLocation == null) {
         return false;
      } else {
         Location currentLocation = player.getLocation();
         return originalLocation.getBlockX() != currentLocation.getBlockX() || originalLocation.getBlockY() != currentLocation.getBlockY() || originalLocation.getBlockZ() != currentLocation.getBlockZ();
      }
   }

   private void cancelTeleport(Player player) {
      player.sendMessage(this.languageManager.getMessage("teleport-cancelled"));
      this.sendActionBarIfPossible(player, this.languageManager.getActionBar("teleport-cancelled"));
      this.soundManager.playTeleportFailSound(player);
      this.teleportLocations.remove(player.getUniqueId());
      ScheduledTask task = (ScheduledTask)this.teleportTasks.remove(player.getUniqueId());
      if (task != null && !task.isCancelled()) {
         task.cancel();
      }

   }

   private void executeTeleport(TPARequest request, Player sender, Player target) {
      if (request.isTpaHere()) {
         target.teleportAsync(sender.getLocation()).thenAccept((success) -> {
            if (success) {
               target.sendMessage(this.languageManager.getMessage("teleported-to-sender").replace("%player%", sender.getName()));
               sender.sendMessage(this.languageManager.getMessage("target-teleported-to-you").replace("%player%", target.getName()));
               this.sendActionBarIfPossible(target, this.languageManager.getActionBar("teleported-to-sender").replace("%player%", sender.getName()));
               this.sendActionBarIfPossible(sender, this.languageManager.getActionBar("target-teleported-to-you").replace("%player%", target.getName()));
               this.soundManager.playTeleportSuccessSound(target);
               this.soundManager.playTeleportSuccessSound(sender);
            }

         });
      } else {
         sender.teleportAsync(target.getLocation()).thenAccept((success) -> {
            if (success) {
               sender.sendMessage(this.languageManager.getMessage("teleported-to-sender").replace("%player%", target.getName()));
               target.sendMessage(this.languageManager.getMessage("target-teleported-to-you").replace("%player%", sender.getName()));
               this.sendActionBarIfPossible(sender, this.languageManager.getActionBar("teleported-to-sender").replace("%player%", target.getName()));
               this.sendActionBarIfPossible(target, this.languageManager.getActionBar("target-teleported-to-you").replace("%player%", sender.getName()));
               this.soundManager.playTeleportSuccessSound(sender);
               this.soundManager.playTeleportSuccessSound(target);
            }

         });
      }

   }

   private void handleTPADeny(Player target) {
      TPARequest request = (TPARequest)this.pendingRequests.get(target.getUniqueId());
      if (request == null) {
         target.sendMessage(this.languageManager.getMessage("no-pending-requests"));
      } else {
         Player sender = Bukkit.getPlayer(request.getSender());
         if (sender != null && sender.isOnline()) {
            sender.sendMessage(this.languageManager.getMessage("request-denied"));
         }

         target.sendMessage(this.languageManager.getMessage("request-denied"));
         this.sendActionBarIfPossible(target, this.languageManager.getActionBar("request-denied"));
         this.pendingRequests.remove(target.getUniqueId());
         ScheduledTask timeoutTask = (ScheduledTask)this.timeoutTasks.remove(target.getUniqueId());
         if (timeoutTask != null) {
            timeoutTask.cancel();
         }
      }

   }

   private void handleTPACancel(Player sender) {
      TPARequest requestToRemove = null;

      for(TPARequest request : this.pendingRequests.values()) {
         if (request.getSender().equals(sender.getUniqueId())) {
            requestToRemove = request;
            break;
         }
      }

      if (requestToRemove == null) {
         sender.sendMessage(this.languageManager.getMessage("no-requests-to-cancel"));
      } else {
         Player target = Bukkit.getPlayer(requestToRemove.getTarget());
         if (target != null && target.isOnline()) {
            target.sendMessage(this.languageManager.getMessage("request-cancelled-receiver").replace("%player%", sender.getName()));
            ScheduledTask timeoutTask = (ScheduledTask)this.timeoutTasks.remove(target.getUniqueId());
            if (timeoutTask != null) {
               timeoutTask.cancel();
            }
         }

         this.pendingRequests.values().removeIf((requestx) -> requestx.getSender().equals(sender.getUniqueId()));
         sender.sendMessage(this.languageManager.getMessage("request-cancelled"));
         this.sendActionBarIfPossible(sender, this.languageManager.getActionBar("request-cancelled"));
      }

   }

   private void handleTPAToggle(Player player) {
      UUID playerId = player.getUniqueId();
      if (this.disabledPlayers.contains(playerId)) {
         this.disabledPlayers.remove(playerId);
         player.sendMessage(this.languageManager.getMessage("tpa-enabled"));
         this.dataManager.updateTPAStatus(playerId, false);
      } else {
         this.pendingRequests.values().removeIf((request) -> request.getTarget().equals(playerId) && !request.isTpaHere());
         this.disabledPlayers.add(playerId);
         player.sendMessage(this.languageManager.getMessage("tpa-disabled"));
         this.dataManager.updateTPAStatus(playerId, true);
      }

   }

   private void handleTPAHereToggle(Player player) {
      UUID playerId = player.getUniqueId();
      if (this.disabledTpaherePlayers.contains(playerId)) {
         this.disabledTpaherePlayers.remove(playerId);
         player.sendMessage(this.languageManager.getMessage("tpahere-enabled"));
         this.dataManager.updateTPAHereStatus(playerId, false);
      } else {
         this.pendingRequests.values().removeIf((request) -> request.getTarget().equals(playerId) && request.isTpaHere());
         this.disabledTpaherePlayers.add(playerId);
         player.sendMessage(this.languageManager.getMessage("tpahere-disabled"));
         this.dataManager.updateTPAHereStatus(playerId, true);
      }

   }

   private void handleTPAGUIToggle(Player player, boolean isTpaHere) {
      this.guiToggleManager.toggleGuiMode(player, isTpaHere);
   }

   private void handleTPAuto(Player player) {
      this.tpaAutoManager.toggleAutoAccept(player);
   }

   private void handleTPAHereAuto(Player player) {
      this.tpaAutoManager.toggleAutoAcceptHere(player);
   }

   private void reloadConfigs() {
      this.configManager.loadConfig();
      this.languageManager.loadMessages();
      this.guiManager.reloadGUIConfigs();
      this.soundManager.loadSounds();
   }

   private void sendActionBarIfPossible(CommandSender sender, String message) {
      if (message != null && sender instanceof Player p) {
         try {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
         } catch (Throwable var9) {
            try {
               Method m = p.getClass().getMethod("sendActionBar", String.class);
               m.invoke(p, message);
            } catch (Throwable var8) {
               try {
                  p.sendMessage(message);
               } catch (Throwable var7) {
               }
            }
         }
      }

   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public GUIManager getGUIManager() {
      return this.guiManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public DataManager getDataManager() {
      return this.dataManager;
   }

   public GUIToggleManager getGUIToggleManager() {
      return this.guiToggleManager;
   }

   public TPAutoManager getTPAutoManager() {
      return this.tpaAutoManager;
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      String cmd = command.getName().toLowerCase();
      if ((cmd.equals("tpa") || cmd.equals("tpahere")) && args.length == 1) {
         List<String> matches = new ArrayList();
         if (!(sender instanceof Player)) {
            return matches;
         } else {
            Player player = (Player)sender;
            String partialName = args[0].toLowerCase();

            for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
               if (!onlinePlayer.equals(player)) {
                  String playerName = onlinePlayer.getName();
                  if (partialName.isEmpty() || playerName.toLowerCase().startsWith(partialName)) {
                     matches.add(playerName);
                  }
               }
            }

            Collections.sort(matches);
            return matches;
         }
      } else {
         return super.onTabComplete(sender, command, alias, args);
      }
   }
}
