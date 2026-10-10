package com.nyretha.home;

import com.nyretha.home.Command.HomeCommand;
import com.nyretha.home.GUI.MainGUI;
import com.nyretha.home.Listener.PlayerJoinListener;
import com.nyretha.home.Manager.ChatManager;
import com.nyretha.home.Manager.DataManager;
import com.nyretha.home.Manager.GUIManager;
import com.nyretha.home.Manager.LangManager;
import com.nyretha.home.Manager.SignManager;
import com.nyretha.home.Manager.SoundManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Home extends JavaPlugin {
   private static Home instance;
   private MainGUI mainGUI;
   private LangManager langManager;
   private SoundManager soundManager;
   private GUIManager guiManager;
   private DataManager dataManager;

   private ChatManager chatManager;
   private SignManager signManager;
   private boolean isFolia;

   public void onEnable() {

      
         instance = this;

         try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            this.isFolia = true;
         } catch (ClassNotFoundException var5) {
            this.isFolia = false;
         }

         this.saveDefaultConfig();
         this.langManager = new LangManager(this);
         this.soundManager = new SoundManager(this);
         this.guiManager = new GUIManager(this);
         this.dataManager = new DataManager(this);
         this.chatManager = new ChatManager(this);
         this.signManager = new SignManager(this, this.chatManager);
         this.mainGUI = new MainGUI(this, this.langManager, this.soundManager, this.guiManager, this.dataManager);
         HomeCommand homeCommand = new HomeCommand(this, this.langManager, this.soundManager, this.guiManager);
         PluginCommand homeCmd = this.getCommand("home");
         if (homeCmd != null) {
            homeCmd.setExecutor(homeCommand);
            homeCmd.setTabCompleter(homeCommand);
         }

         PluginCommand sethomeCmd = this.getCommand("sethome");
         if (sethomeCmd != null) {
            sethomeCmd.setExecutor(homeCommand);
            sethomeCmd.setTabCompleter(homeCommand);
         }

         PluginCommand delhomeCmd = this.getCommand("delhome");
         if (delhomeCmd != null) {
            delhomeCmd.setExecutor(homeCommand);
            delhomeCmd.setTabCompleter(homeCommand);
         }

         Bukkit.getPluginManager().registerEvents(this.mainGUI, this);
         Bukkit.getPluginManager().registerEvents(this.chatManager, this);
         Bukkit.getPluginManager().registerEvents(this.signManager, this);
         Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
         this.runGlobalLater(() -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
               this.mainGUI.loadPlayerData(player);
            }

         }, 20L);
         if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
         }

      
   }

   

   public void onDisable() {
      for(Player player : Bukkit.getOnlinePlayers()) {
         this.mainGUI.savePlayerData(player);
      }

   }

   public static Home getInstance() {
      return instance;
   }

   public MainGUI getMainGUI() {
      return this.mainGUI;
   }

   public LangManager getLangManager() {
      return this.langManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public GUIManager getGuiManager() {
      return this.guiManager;
   }

   public DataManager getDataManager() {
      return this.dataManager;
   }

   public ChatManager getChatManager() {
      return this.chatManager;
   }

   public SignManager getSignManager() {
      return this.signManager;
   }

   public boolean isFolia() {
      return this.isFolia;
   }

   public void runAsync(Runnable task) {
      if (this.isFolia) {
         Bukkit.getAsyncScheduler().runNow(this, (scheduledTask) -> task.run());
      } else {
         Bukkit.getScheduler().runTaskAsynchronously(this, task);
      }

   }

   public void runAsyncLater(Runnable task, long delayTicks) {
      if (this.isFolia) {
         long delayMillis = delayTicks * 50L;
         Bukkit.getAsyncScheduler().runDelayed(this, (scheduledTask) -> task.run(), delayMillis, TimeUnit.MILLISECONDS);
      } else {
         Bukkit.getScheduler().runTaskLaterAsynchronously(this, task, delayTicks);
      }

   }

   public void runGlobal(Runnable task) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().run(this, (scheduledTask) -> task.run());
      } else {
         Bukkit.getScheduler().runTask(this, task);
      }

   }

   public void runGlobalLater(Runnable task, long delayTicks) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delayTicks);
      } else {
         Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   public void runAtLocation(Location location, Runnable task) {
      if (location != null && location.getWorld() != null) {
         if (this.isFolia) {
            Bukkit.getGlobalRegionScheduler().run(this, (scheduledTask) -> task.run());
         } else {
            Bukkit.getScheduler().runTask(this, task);
         }

      } else {
         this.runGlobal(task);
      }
   }

   public void runAtPlayer(Player player, Runnable task) {
      if (player == null) {
         this.runGlobal(task);
      } else {
         if (this.isFolia) {
            player.getScheduler().run(this, (scheduledTask) -> task.run(), (Runnable)null);
         } else {
            Bukkit.getScheduler().runTask(this, task);
         }

      }
   }

   public void runAtPlayerLater(Player player, Runnable task, long delayTicks) {
      if (player == null) {
         this.runGlobalLater(task, delayTicks);
      } else {
         if (this.isFolia) {
            player.getScheduler().runDelayed(this, (scheduledTask) -> task.run(), (Runnable)null, delayTicks);
         } else {
            Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
         }

      }
   }

   public void runAtLocationLater(Location location, Runnable task, long delayTicks) {
      if (location != null && location.getWorld() != null) {
         if (this.isFolia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delayTicks);
         } else {
            Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
         }

      } else {
         this.runGlobalLater(task, delayTicks);
      }
   }

   public Object runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
      if (this.isFolia) {
         if (delayTicks <= 0L) {
            delayTicks = 1L;
         }

         return Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), delayTicks, periodTicks);
      } else {
         return Bukkit.getScheduler().runTaskTimer(this, task, delayTicks, periodTicks);
      }
   }

   public void cancelTask(Object task) {
      if (task != null) {
         if (this.isFolia) {
            if (task instanceof ScheduledTask) {
               ((ScheduledTask)task).cancel();
            }
         } else if (task instanceof Integer) {
            Bukkit.getScheduler().cancelTask((Integer)task);
         }

      }
   }
}
