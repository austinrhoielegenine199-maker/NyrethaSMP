package de.elivb.teams;

import de.elivb.teams.GUI.MemberManagerGUI;
import de.elivb.teams.GUI.TeamGUI;
import de.elivb.teams.Manager.DataManager;
import de.elivb.teams.Manager.HomeManager;
import de.elivb.teams.Manager.LangManager;
import de.elivb.teams.Manager.SignManager;
import de.elivb.teams.Manager.SoundManager;
import de.elivb.teams.Manager.TeamManager;
import de.elivb.teams.Manager.TeamRankManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Team extends JavaPlugin implements Listener, TabCompleter {
   private TeamManager teamManager;
   private LangManager langManager;
   private HomeManager teamHome;
   private TeamRankManager rankManager;
   private TeamGUI teamGUI;
   private MemberManagerGUI memberManagerGUI;
   private SoundManager soundManager;
   private SignManager signManager;
   private Map<String, DataManager> playerTeams;
   private boolean isFolia;
   private LicenseManager licenseManager;
   private Set<UUID> teamChatEnabled;

   public void onEnable() {
      this.licenseManager = new LicenseManager(this);
      if (this.licenseManager.validateLicenseOnStartup()) {
         this.saveDefaultConfig();
         this.reloadConfig();
         this.isFolia = this.checkFolia();
         this.langManager = new LangManager(this);
         this.teamManager = new TeamManager(this);
         this.teamHome = new HomeManager(this);
         this.rankManager = new TeamRankManager(this);
         this.signManager = new SignManager(this);
         this.teamGUI = new TeamGUI(this);
         this.memberManagerGUI = new MemberManagerGUI(this);
         this.soundManager = new SoundManager(this);
         this.playerTeams = new HashMap();
         this.teamChatEnabled = new HashSet();
         this.getServer().getPluginManager().registerEvents(this, this);
         this.getServer().getPluginManager().registerEvents(new PvPListener(this), this);
         this.getServer().getPluginManager().registerEvents(new TeamChatListener(this), this);
         this.langManager.loadMessages();
         this.teamManager.loadAllTeams();
         this.rankManager.loadAllPermissions();
         if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new Papi1(this)).register();
         }

      }
   }

   private boolean checkFolia() {
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         return true;
      } catch (ClassNotFoundException var2) {
         return false;
      }
   }

   public void onDisable() {
      if (this.rankManager != null) {
         this.rankManager.saveAllPermissions();
      }

      if (this.teamGUI != null) {
         this.teamGUI.clearHeadCache();
         if (this.teamGUI.getGuiManager() != null) {
            this.teamGUI.getGuiManager().shutdown();
         }
      }

      if (this.teamHome != null) {
         this.teamHome.onDisable();
      }

      if (this.signManager != null) {
      }

   }

   public LicenseManager getLicenseManager() {
      return this.licenseManager;
   }

   private boolean isTeamHomeEnabled() {
      return this.getConfig().getBoolean("team-home.enabled", true);
   }

   public void setTeamChatEnabled(Player player, boolean enabled) {
      if (enabled) {
         this.teamChatEnabled.add(player.getUniqueId());
      } else {
         this.teamChatEnabled.remove(player.getUniqueId());
      }

   }

   public boolean isTeamChatEnabled(Player player) {
      return this.teamChatEnabled.contains(player.getUniqueId());
   }

   public void sendTeamMessage(Player sender, String message) {
      DataManager team = this.teamManager.getPlayerTeam(sender.getName());
      if (team == null) {
         sender.sendMessage(this.langManager.getMessage("errors.not-in-team"));
         this.soundManager.playSound(sender, "error-sound");
      } else {
         String formattedMessage = this.langManager.getMessage("team.chat-format").replace("%player%", sender.getName()).replace("%message%", message).replace("%team%", team.getName());
         String coloredMessage = HexUtils.colorize(formattedMessage);

         for(String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayer(memberName);
            if (member != null && member.isOnline()) {
               member.sendMessage(coloredMessage);
            }
         }

      }
   }

   public void runTaskAsynchronously(Runnable task) {
      if (this.isFolia) {
         this.getServer().getAsyncScheduler().runNow(this, (scheduledTask) -> task.run());
      } else {
         this.getServer().getScheduler().runTaskAsynchronously(this, task);
      }

   }

   public void runTaskLaterAsynchronously(Runnable task, long delay) {
      if (this.isFolia) {
         this.getServer().getAsyncScheduler().runDelayed(this, (scheduledTask) -> task.run(), delay * 50L, TimeUnit.MILLISECONDS);
      } else {
         this.getServer().getScheduler().runTaskLaterAsynchronously(this, task, delay);
      }

   }

   public void runTaskTimerAsynchronously(Runnable task, long delay, long period) {
      if (this.isFolia) {
         this.getServer().getAsyncScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), delay * 50L, period * 50L, TimeUnit.MILLISECONDS);
      } else {
         this.getServer().getScheduler().runTaskTimerAsynchronously(this, task, delay, period);
      }

   }

   public void runTask(Player player, Runnable task) {
      if (this.isFolia) {
         player.getScheduler().run(this, (scheduledTask) -> task.run(), (Runnable)null);
      } else {
         this.getServer().getScheduler().runTask(this, task);
      }

   }

   public void runTask(Runnable task) {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().run(this, (scheduledTask) -> task.run());
      } else {
         this.getServer().getScheduler().runTask(this, task);
      }

   }

   public void runTaskLater(Player player, Runnable task, long delay) {
      if (this.isFolia) {
         player.getScheduler().runDelayed(this, (scheduledTask) -> task.run(), (Runnable)null, delay);
      } else {
         this.getServer().getScheduler().runTaskLater(this, task, delay);
      }

   }

   public void runTaskLater(Runnable task, long delay) {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delay);
      } else {
         this.getServer().getScheduler().runTaskLater(this, task, delay);
      }

   }

   public void runTaskTimer(Player player, Runnable task, long delay, long period) {
      if (this.isFolia) {
         player.getScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), (Runnable)null, delay, period);
      } else {
         this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
      }

   }

   public void runTaskTimer(Runnable task, long delay, long period) {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), delay, period);
      } else {
         this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
      }

   }

   public void runTaskAtLocation(Location location, Runnable task) {
      if (this.isFolia) {
         this.getServer().getRegionScheduler().run(this, location, (scheduledTask) -> task.run());
      } else {
         this.getServer().getScheduler().runTask(this, task);
      }

   }

   public void runTaskLaterAtLocation(Location location, Runnable task, long delay) {
      if (this.isFolia) {
         this.getServer().getRegionScheduler().runDelayed(this, location, (scheduledTask) -> task.run(), delay);
      } else {
         this.getServer().getScheduler().runTaskLater(this, task, delay);
      }

   }

   public void runTaskTimerAtLocation(Location location, Runnable task, long delay, long period) {
      if (this.isFolia) {
         this.getServer().getRegionScheduler().runAtFixedRate(this, location, (scheduledTask) -> task.run(), delay, period);
      } else {
         this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
      }

   }

   public Object runGlobalTimer(Runnable task, long delay, long period) {
      return this.isFolia ? this.getServer().getGlobalRegionScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), delay, period) : this.getServer().getScheduler().runTaskTimer(this, task, delay, period);
   }

   public Object runGlobalOnce(Runnable task, long delay) {
      return this.isFolia ? this.getServer().getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delay) : this.getServer().getScheduler().runTaskLater(this, task, delay);
   }

   public void cancelTask(Object task) {
      if (task != null) {
         if (this.isFolia) {
            if (task instanceof ScheduledTask) {
               ((ScheduledTask)task).cancel();
            }
         } else if (task instanceof BukkitTask) {
            ((BukkitTask)task).cancel();
         } else if (task instanceof Integer) {
            this.getServer().getScheduler().cancelTask((Integer)task);
         } else if (task instanceof Number) {
            this.getServer().getScheduler().cancelTask(((Number)task).intValue());
         }

      }
   }

   public void cancelTask(Player player, Object task) {
      if (task != null) {
         if (this.isFolia) {
            if (task instanceof ScheduledTask) {
               ((ScheduledTask)task).cancel();
            }
         } else if (task instanceof BukkitTask) {
            ((BukkitTask)task).cancel();
         } else if (task instanceof Integer) {
            this.getServer().getScheduler().cancelTask((Integer)task);
         } else if (task instanceof Number) {
            this.getServer().getScheduler().cancelTask(((Number)task).intValue());
         }

      }
   }

   public void cancelAllTasks() {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().cancelTasks(this);
         this.getServer().getAsyncScheduler().cancelTasks(this);
      } else {
         this.getServer().getScheduler().cancelTasks(this);
      }

   }

   public boolean isFolia() {
      return this.isFolia;
   }

   public TeamGUI getTeamGUI() {
      return this.teamGUI;
   }

   public MemberManagerGUI getMemberManagerGUI() {
      return this.memberManagerGUI;
   }

   public TeamManager getTeamManager() {
      return this.teamManager;
   }

   public LangManager getLangManager() {
      return this.langManager;
   }

   public HomeManager getTeamHome() {
      return this.teamHome;
   }

   public TeamRankManager getRankManager() {
      return this.rankManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public SignManager getSignManager() {
      return this.signManager;
   }

   public Map<String, DataManager> getPlayerTeams() {
      return this.playerTeams;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("team")) {
         if (args.length == 0) {
            if (sender instanceof Player) {
               Player player = (Player)sender;
               this.teamGUI.openTeamGUI(player);
            }

            return true;
         }

         if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("team.admin")) {
               if (sender instanceof Player) {
                  sender.sendMessage(this.langManager.getMessage("errors.insufficient-rank"));
                  this.soundManager.playSound((Player)sender, "error-sound");
               }

               return true;
            }

            String BRIGHT_CYAN = "\u001b[96m";
            String BRIGHT_RED = "\u001b[91m";
            String RESET = "\u001b[0m";
            this.getLogger().info(BRIGHT_CYAN + "Reloading configuration..." + RESET);
            this.reloadConfig();
            this.langManager.reloadMessages();
            this.teamManager.loadAllTeams();
            this.rankManager.loadAllPermissions();
            this.teamGUI.reloadConfig();
            this.memberManagerGUI.reloadConfig();
            this.teamManager.reloadBlockedTeamNames();
            this.teamManager.reloadTeamNameLengthSettings();
            if (this.teamHome != null) {
               this.teamHome.reloadSettings();
            }

            this.getLogger().info(BRIGHT_RED + "╠ Reloaded config.yml!" + RESET);
            this.getLogger().info(BRIGHT_RED + "╠ Reloaded lang.yml!" + RESET);
            this.getLogger().info(BRIGHT_RED + "╠ Reloaded Playerdata!" + RESET);
            this.getLogger().info(BRIGHT_RED + "╠ Reloaded GUI!" + RESET);
            this.getLogger().info(BRIGHT_CYAN + "╚ Successful reload!" + RESET);
            if (sender instanceof Player) {
               sender.sendMessage(this.langManager.getMessage("team.plugin-reloaded"));
               this.soundManager.playSound((Player)sender, "plugin-reloaded");
            } else {
               sender.sendMessage(this.langManager.getMessage("team.plugin-reloaded"));
            }

            return true;
         }

         if (!(sender instanceof Player)) {
            return true;
         }

         Player player = (Player)sender;
         switch (args[0].toLowerCase()) {
            case "create":
               if (args.length < 2) {
                  return true;
               }

               this.teamManager.createTeam(player, args[1]);
               break;
            case "invite":
               if (args.length < 2) {
                  return true;
               }

               this.teamManager.invitePlayer(player, args[1]);
               break;
            case "join":
               this.teamManager.acceptInvite(player);
               break;
            case "leave":
               this.teamManager.leaveTeam(player);
               break;
            case "kick":
               if (args.length < 2) {
                  return true;
               }

               this.teamManager.kickPlayer(player, args[1]);
               break;
            case "delete":
               this.teamManager.disbandTeam(player);
               break;
            case "sethome":
               if (!this.isTeamHomeEnabled()) {
                  player.sendMessage(this.langManager.getMessage("errors.team-home-disabled"));
                  this.soundManager.playSound(player, "error-sound");
               } else {
                  this.teamHome.setHome(player);
               }
               break;
            case "home":
               if (!this.isTeamHomeEnabled()) {
                  player.sendMessage(this.langManager.getMessage("errors.team-home-disabled"));
                  this.soundManager.playSound(player, "error-sound");
               } else {
                  this.teamHome.teleportToHome(player);
               }
               break;
            case "delhome":
               if (!this.isTeamHomeEnabled()) {
                  player.sendMessage(this.langManager.getMessage("errors.team-home-disabled"));
                  this.soundManager.playSound(player, "error-sound");
               } else {
                  this.teamHome.deleteHome(player);
               }
               break;
            case "transfer":
               if (args.length < 2) {
                  return true;
               }

               this.teamManager.transferLeadership(player, args[1]);
               break;
            case "gui":
               this.teamGUI.openTeamGUI(player);
               break;
            case "chat":
               if (args.length == 1) {
                  boolean isEnabled = this.isTeamChatEnabled(player);
                  if (isEnabled) {
                     this.setTeamChatEnabled(player, false);
                     player.sendMessage(this.langManager.getMessage("team.chat-disabled"));
                  } else {
                     this.setTeamChatEnabled(player, true);
                     player.sendMessage(this.langManager.getMessage("team.chat-enabled"));
                  }
               } else if (args.length >= 2 && args[1].equalsIgnoreCase("toggle")) {
                  boolean isEnabled = this.isTeamChatEnabled(player);
                  if (isEnabled) {
                     this.setTeamChatEnabled(player, false);
                     player.sendMessage(this.langManager.getMessage("team.chat-disabled"));
                  } else {
                     this.setTeamChatEnabled(player, true);
                     player.sendMessage(this.langManager.getMessage("team.chat-enabled"));
                  }
               } else {
                  String message = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 1, args.length));
                  this.sendTeamMessage(player, message);
               }
               break;
            default:
               this.showHelp(player);
         }
      }

      return true;
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (args.length == 1) {
         List<String> commands = new ArrayList(Arrays.asList("create", "invite", "join", "leave", "kick", "delete", "transfer", "gui", "chat"));
         if (this.isTeamHomeEnabled()) {
            commands.addAll(Arrays.asList("sethome", "home", "delhome"));
         }

         if (sender.hasPermission("team.admin")) {
            commands.add("reload");
         }

         for(String cmd : commands) {
            if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
               completions.add(cmd);
            }
         }
      } else if (args.length == 2) {
         switch (args[0].toLowerCase()) {
            case "invite":
            case "kick":
            case "transfer":
               for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                     completions.add(onlinePlayer.getName());
                  }
               }
               break;
            case "join":
               if (sender instanceof Player) {
                  Player player = (Player)sender;
                  String pendingInvite = this.teamManager.getPendingInvite(player.getName());
                  if (pendingInvite != null && pendingInvite.toLowerCase().startsWith(args[1].toLowerCase())) {
                     completions.add(pendingInvite);
                  }
               }
               break;
            case "chat":
               completions.add("toggle");
         }
      }

      return completions;
   }

   private void showHelp(Player player) {
      String normalHelp = this.langManager.getMessageWithoutPrefix("command-usage");
      String[] normalLines = normalHelp.split("\n");

      for(String line : normalLines) {
         player.sendMessage(line);
      }

      if (player.hasPermission("team.admin")) {
         player.sendMessage("");
         String adminHelp = this.langManager.getMessageWithoutPrefix("command-usage-admin");
         String[] adminLines = adminHelp.split("\n");

         for(String line : adminLines) {
            player.sendMessage(line);
         }
      }

   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.teamManager.getPlayerTeam(player.getName());
      this.teamChatEnabled.remove(player.getUniqueId());
   }
}
