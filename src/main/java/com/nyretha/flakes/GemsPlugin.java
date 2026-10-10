package de.elivb.shards;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.elivb.shards.Command.GemsCommand;
import de.elivb.shards.GUI.Main;
import de.elivb.shards.GUI.PayGUI;
import de.elivb.shards.GUI.UpgradeGUI;
import de.elivb.shards.Manager.DatabaseManager;
import de.elivb.shards.Manager.GemManager;
import de.elivb.shards.Manager.LangManager;
import de.elivb.shards.Manager.ShardBoosterManager;
import de.elivb.shards.Manager.SoundManager;
import de.elivb.shards.Utlis.papi1;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GemsPlugin extends JavaPlugin implements Listener {
   private DatabaseManager databaseManager;
   private UpgradeGUI upgradeGUI;
   private LangManager langManager;
   private GemManager gemManager;
   private Main viewManager;
   private PayGUI payGUI;
   private SoundManager soundManager;
   private ShardBoosterManager shardBoosterManager;
   private FileConfiguration config;
   private papi1 papiExpansion;
   private List<String> enabledWorlds;
   private List<String> enabledRegions;
   private Economy economy;
   private LicenseManager licenseManager;
   private boolean isFolia = false;
   private boolean worldGuardEnabled = false;
   private RegionContainer regionContainer;
   private final Map<UUID, Boolean> lastRegionStatus = new ConcurrentHashMap();

   public void onEnable() {
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         this.isFolia = true;
      } catch (ClassNotFoundException var2) {
         this.isFolia = false;
      }

      this.licenseManager = new LicenseManager(this);
      if (this.licenseManager.validateLicenseOnStartup()) {
         this.runTaskLater(this::initializeAfterPluginsLoaded, 1L);
      }
   }

   public LicenseManager getLicenseManager() {
      return this.licenseManager;
   }

   public boolean isFolia() {
      return this.isFolia;
   }

   public void runAsync(Runnable task) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().execute(this, task);
      } else {
         Bukkit.getScheduler().runTaskAsynchronously(this, task);
      }

   }

   public void runSync(Runnable task) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().execute(this, task);
      } else {
         Bukkit.getScheduler().runTask(this, task);
      }

   }

   public void runTaskLater(Runnable task, long delayTicks) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delayTicks);
      } else {
         Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   public void runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), delayTicks, periodTicks);
      } else {
         Bukkit.getScheduler().runTaskTimer(this, task, delayTicks, periodTicks);
      }

   }

   public void runForEntity(Entity entity, Runnable task) {
      if (this.isFolia) {
         entity.getScheduler().run(this, (scheduledTask) -> task.run(), (Runnable)null);
      } else {
         this.runSync(task);
      }

   }

   public void runForEntityLater(Entity entity, Runnable task, long delayTicks) {
      if (this.isFolia) {
         entity.getScheduler().runDelayed(this, (scheduledTask) -> task.run(), (Runnable)null, delayTicks);
      } else {
         Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   public void runForEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
      if (this.isFolia) {
         entity.getScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), (Runnable)null, delayTicks, periodTicks);
      } else {
         Bukkit.getScheduler().runTaskTimer(this, task, delayTicks, periodTicks);
      }

   }

   public void runAtLocation(Location location, Runnable task) {
      if (this.isFolia) {
         Bukkit.getRegionScheduler().execute(this, location, task);
      } else {
         this.runSync(task);
      }

   }

   public void runAtLocationLater(Location location, Runnable task, long delayTicks) {
      if (this.isFolia) {
         Bukkit.getRegionScheduler().runDelayed(this, location, (scheduledTask) -> task.run(), delayTicks);
      } else {
         Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   private void initializeAfterPluginsLoaded() {
      try {
         this.saveDefaultConfig();
         this.config = this.getConfig();
         this.enabledWorlds = this.config.getStringList("enabled-worlds");
         this.enabledRegions = this.config.getStringList("enabled-regions");
         this.setupWorldGuard();
         this.setupEconomy();
         this.langManager = new LangManager(this);
         this.databaseManager = new DatabaseManager(this);
         this.runAsync(() -> {
            this.databaseManager.initializeDatabase();
            this.runTaskLater(() -> this.databaseManager.cleanupOldBackups(7), 100L);
         });
         this.gemManager = new GemManager(this);
         this.viewManager = new Main(this);
         this.payGUI = new PayGUI(this);
         this.soundManager = new SoundManager(this);
         this.upgradeGUI = new UpgradeGUI(this);
         this.shardBoosterManager = new ShardBoosterManager(this);
         this.runAsync(() -> this.shardBoosterManager.loadAllBoosters());
         this.runSync(this::registerCommands);
         this.getServer().getPluginManager().registerEvents(this.payGUI, this);
         this.getServer().getPluginManager().registerEvents(this.upgradeGUI, this);
         this.getServer().getPluginManager().registerEvents(this, this);
         if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.papiExpansion = new papi1(this);
            if (this.papiExpansion.register()) {
            }
         }

         for(Player player : Bukkit.getOnlinePlayers()) {
            this.runForEntity(player, () -> {
               if (this.gemManager != null) {
                  this.gemManager.resetCooldownOnQuit(player);
               }

               this.checkRegionChange(player);
            });
         }
      } catch (Exception e) {
         e.printStackTrace();
         Bukkit.getPluginManager().disablePlugin(this);
      }

   }

   private void registerCommands() {
      try {
         Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
         commandMapField.setAccessible(true);
         CommandMap commandMap = (CommandMap)commandMapField.get(Bukkit.getServer());

         try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map)knownCommandsField.get(commandMap);
            knownCommands.remove("shards");
            knownCommands.remove("shards:shards");

            for(String alias : this.getConfig().getStringList("command-aliases")) {
               knownCommands.remove(alias);
               knownCommands.remove("shards:" + alias);
            }
         } catch (Exception var9) {
         }

         GemsCommand executor = new GemsCommand(this);
         PluginCommand mainCommand = this.createCommand("shards");
         if (mainCommand != null) {
            mainCommand.setExecutor(executor);
            mainCommand.setPermission("shards.use");
            mainCommand.setUsage("/shards");
            mainCommand.setLabel("shards");
            mainCommand.setAliases(new ArrayList());
            commandMap.register("", mainCommand);
         }

         for(String alias : this.getConfig().getStringList("command-aliases")) {
            if (!alias.equals("shards")) {
               PluginCommand aliasCommand = this.createCommand(alias);
               if (aliasCommand != null) {
                  aliasCommand.setExecutor(executor);
                  aliasCommand.setPermission("shards.use");
                  aliasCommand.setUsage("/" + alias);
                  aliasCommand.setLabel(alias);
                  aliasCommand.setAliases(new ArrayList());
                  commandMap.register("", aliasCommand);
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private PluginCommand createCommand(String name) {
      try {
         Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
         constructor.setAccessible(true);
         return (PluginCommand)constructor.newInstance(name, this);
      } catch (Exception var3) {
         return null;
      }
   }

   private void setupWorldGuard() {
      Plugin wgPlugin = this.getServer().getPluginManager().getPlugin("WorldGuard");
      if (wgPlugin != null && wgPlugin.isEnabled()) {
         try {
            this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            this.worldGuardEnabled = true;
            if (this.enabledRegions.isEmpty()) {
            }
         } catch (Exception var3) {
            this.worldGuardEnabled = false;
         }
      }

   }

   public boolean isInAllowedRegion(Player player) {
      if (this.worldGuardEnabled && !this.enabledRegions.isEmpty()) {
         try {
            Location loc = player.getLocation();
            RegionQuery query = this.regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(loc));
            if (regions.size() == 0) {
               return false;
            } else {
               for(ProtectedRegion region : regions) {
                  if (this.enabledRegions.contains(region.getId())) {
                     return true;
                  }
               }

               return false;
            }
         } catch (Exception var7) {
            return true;
         }
      } else {
         return true;
      }
   }

   public List<String> getPlayerRegions(Player player) {
      List<String> playerRegions = new ArrayList();
      if (!this.worldGuardEnabled) {
         return playerRegions;
      } else {
         try {
            Location loc = player.getLocation();
            RegionQuery query = this.regionContainer.createQuery();

            for(ProtectedRegion region : query.getApplicableRegions(BukkitAdapter.adapt(loc))) {
               playerRegions.add(region.getId());
            }
         } catch (Exception var8) {
         }

         return playerRegions;
      }
   }

   public boolean canEarnGemsAtLocation(Player player) {
      if (player.hasPermission("shards.vip")) {
         return true;
      } else {
         boolean hasRegionConfig = !this.enabledRegions.isEmpty();
         if (hasRegionConfig) {
            List<String> playerRegions = this.getPlayerRegions(player);
            if (this.isInAllowedRegion(player)) {
               return true;
            } else {
               return playerRegions.isEmpty() ? this.canEarnGemsInWorld(player.getWorld().getName()) : false;
            }
         } else {
            return this.canEarnGemsInWorld(player.getWorld().getName());
         }
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.runForEntity(player, () -> {
         if (this.gemManager != null) {
            this.gemManager.resetCooldownOnQuit(player);
         }

         if (this.shardBoosterManager != null) {
            this.shardBoosterManager.loadBoosterData(player);
         }

         this.checkRegionChange(player);
      });
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID playerId = player.getUniqueId();
      this.lastRegionStatus.remove(playerId);
      if (this.gemManager != null) {
         this.gemManager.resetCooldownOnQuit(player);
      }

   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      Location from = event.getFrom();
      Location to = event.getTo();
      if (to != null) {
         if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            this.runForEntity(player, () -> this.checkRegionChange(player));
         }
      }
   }

   @EventHandler
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      Player player = event.getPlayer();
      this.runForEntity(player, () -> this.checkRegionChange(player));
   }

   private void checkRegionChange(Player player) {
      if (this.gemManager != null) {
         boolean canEarn = this.canEarnGemsAtLocation(player);
         UUID playerId = player.getUniqueId();
         Boolean lastStatus = (Boolean)this.lastRegionStatus.get(playerId);
         if (lastStatus == null || lastStatus != canEarn) {
            this.lastRegionStatus.put(playerId, canEarn);
            if (canEarn) {
               this.gemManager.onPlayerEnterArea(player);
            } else {
               this.gemManager.onPlayerLeaveArea(player);
            }
         }

      }
   }

   public void onDisable() {
      try {
         if (this.databaseManager != null) {
            this.runAsync(() -> {
               this.databaseManager.createBackup("shutdown");
               this.databaseManager.closeConnection();
            });
         }

         if (this.papiExpansion != null) {
            this.papiExpansion.unregister();
         }
      } catch (Exception var2) {
      }

   }

   public boolean createBackup() {
      return this.databaseManager.createManualBackup();
   }

   public List<String> getBackupList() {
      return this.databaseManager.getBackupList();
   }

   public boolean restoreBackup(String backupName) {
      return this.databaseManager.restoreBackup(backupName);
   }

   private void setupEconomy() {
      if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
         this.getLogger().warning("Vault not found - Economy features disabled!");
      } else {
         RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp != null) {
            this.economy = (Economy)rsp.getProvider();
            if (this.economy != null) {
            }

         }
      }
   }

   public Economy getEconomy() {
      return this.economy;
   }

   public boolean canEarnGemsInWorld(String worldName) {
      return this.enabledWorlds.contains(worldName);
   }

   public boolean teleportToWorld(Player player, String worldName) {
      World world = Bukkit.getWorld(worldName);
      if (world != null) {
         player.teleport(world.getSpawnLocation());
         return true;
      } else {
         return false;
      }
   }

   public List<String> getAvailableWorlds() {
      List<String> worlds = new ArrayList();

      for(World world : Bukkit.getWorlds()) {
         worlds.add(world.getName());
      }

      return worlds;
   }

   public boolean isWorldExists(String worldName) {
      return Bukkit.getWorld(worldName) != null;
   }

   public boolean takeGems(UUID playerUUID, int amount) {
      try {
         int currentGems = this.getDatabaseManager().getGems(playerUUID);
         if (currentGems >= amount) {
            this.getDatabaseManager().removeGems(playerUUID, amount);
            return true;
         } else {
            return false;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public int getGems(UUID playerUUID) {
      try {
         return this.getDatabaseManager().getGems(playerUUID);
      } catch (Exception var3) {
         return 0;
      }
   }

   public boolean addGems(UUID playerUUID, int amount) {
      try {
         this.getDatabaseManager().addGems(playerUUID, amount);
         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   public int getVIPShardsPerMinute() {
      return this.config.getInt("vip-perm.shards-per-minute", 1);
   }

   public boolean isVIPEnabled() {
      return this.config.getBoolean("vip-perm.enabled", true);
   }

   public DatabaseManager getDatabaseManager() {
      return this.databaseManager;
   }

   public LangManager getLangManager() {
      return this.langManager;
   }

   public GemManager getGemManager() {
      return this.gemManager;
   }

   public Main getViewManager() {
      return this.viewManager;
   }

   public PayGUI getPayGUI() {
      return this.payGUI;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public UpgradeGUI getUpgradeGUI() {
      return this.upgradeGUI;
   }

   public ShardBoosterManager getShardBoosterManager() {
      return this.shardBoosterManager;
   }

   public FileConfiguration getPluginConfig() {
      return this.config;
   }

   public List<String> getEnabledWorlds() {
      return this.enabledWorlds;
   }

   public List<String> getEnabledRegions() {
      return this.enabledRegions;
   }

   public boolean isWorldGuardEnabled() {
      return this.worldGuardEnabled;
   }

   public void reloadPluginConfig() {
      this.reloadConfig();
      this.config = this.getConfig();
      this.enabledWorlds = this.config.getStringList("enabled-worlds");
      this.enabledRegions = this.config.getStringList("enabled-regions");
      if (this.shardBoosterManager != null) {
         this.runAsync(() -> this.shardBoosterManager.reloadBoosterConfig());
      }

   }

   public static GemsPlugin getInstance() {
      return (GemsPlugin)getPlugin(GemsPlugin.class);
   }
}
