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
package de.elivb.shop;

import de.elivb.shop.models.CategoryInventory;
import de.elivb.shop.models.ConfirmMenu;
import de.elivb.shop.models.ShardConfirmMenu;
import de.elivb.shop.models.ShopCategory;
import de.elivb.shop.models.ShopItem;
import de.elivb.shop.shards.ShardsManager;
import de.elivb.shop.utils.EconomyManager;
import de.elivb.shop.utils.HexColor;
import de.elivb.shop.utils.LangManager;
import de.elivb.shop.utils.PlayerDataManager;
import de.elivb.shop.utils.ShopPlaceholders;
import de.elivb.shop.utils.ShopTabCompleter;
import de.elivb.shop.utils.SoundManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public class Shop extends JavaPlugin implements Listener {
   private HashMap<String, ShopCategory> categories = new HashMap();
   private HashMap<String, CategoryInventory> categoryInventories = new HashMap();
   private File configFile;
   private FileConfiguration config;
   private File mainguiFile;
   private FileConfiguration mainguiConfig;
   private String mainMenuTitle;
   private int mainMenuRows;
   private Economy economy;
   private EconomyManager economyManager;
   private File shopsFolder;
   private LangManager langManager;
   private SoundManager soundManager;
   private ConfirmMenu confirmMenu;
   private ShardConfirmMenu shardConfirmMenu;
   private ShardsManager shardsManager;
   private ItemStack backButton;
   private boolean economyEnabled = false;
   private boolean pluginEnabled = false;
   private HashMap<UUID, PurchaseData> pendingPurchases = new HashMap();
   private HashMap<UUID, ShardPurchaseData> pendingShardPurchases = new HashMap();
   private String priceLoreFormat;
   private int priceLoreLine;
   private Object gemsPlugin;
   private boolean gemsIntegrationEnabled = false;
   private PlayerDataManager playerDataManager;
   private boolean fillEnabled = false;
   private ItemStack fillItem = null;
   private String[] fillSlots = new String[0];
   private LicenseManager licenseManager;
   private int buyCooldown = 3;
   private HashMap<UUID, Long> cooldownMap = new HashMap();
   private boolean isFolia = false;
   private HashMap<String, HashMap<UUID, Integer>> categoryPages = new HashMap();

   public LangManager getLangManager() {
      return this.langManager;
   }

   public void onEnable() {
      this.licenseManager = new LicenseManager(this);
      if (this.licenseManager.validateLicenseOnStartup()) {
         try {
            try {
               Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
               this.isFolia = true;
            } catch (ClassNotFoundException var2) {
               this.isFolia = false;
            }

            if (!this.getDataFolder().exists()) {
               this.getDataFolder().mkdirs();
            }

            this.playerDataManager = new PlayerDataManager(this);
            this.ensureAllResources();
            this.getServer().getPluginManager().registerEvents(this, this);
            ((PluginCommand)Objects.requireNonNull(this.getCommand("shop"))).setExecutor(this);
            ((PluginCommand)Objects.requireNonNull(this.getCommand("shop"))).setTabCompleter(new ShopTabCompleter(this));
            this.langManager = new LangManager(this);
            this.soundManager = new SoundManager(this);
            this.economyManager = new EconomyManager(this);
            this.confirmMenu = new ConfirmMenu(this);
            this.shardConfirmMenu = new ShardConfirmMenu(this);
            this.shardsManager = new ShardsManager(this);
            this.setupGemsIntegration();
            this.loadConfigurations();
            this.setupPlaceholderAPI();
            if (this.isFolia) {
               this.getServer().getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> {
                  if (!this.setupEconomy()) {
                     this.economyEnabled = false;
                  } else {
                     this.economyEnabled = true;
                  }

                  this.pluginEnabled = true;
               }, 40L);
            } else {
               Bukkit.getScheduler().runTaskLater(this, () -> {
                  if (!this.setupEconomy()) {
                     this.economyEnabled = false;
                  } else {
                     this.economyEnabled = true;
                  }

                  this.pluginEnabled = true;
               }, 40L);
            }
         } catch (Exception e) {
            e.printStackTrace();
            this.pluginEnabled = true;
            this.economyEnabled = false;
         }

      }
   }

   public LicenseManager getLicenseManager() {
      return this.licenseManager;
   }

   private void setupPlaceholderAPI() {
      if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         try {
            ShopPlaceholders shopPlaceholders = new ShopPlaceholders(this);
            if (shopPlaceholders.register()) {
            }
         } catch (Exception var2) {
         }
      }

   }

   private void debugAvailablePlugins() {
      for(Plugin var10000 : this.getServer().getPluginManager().getPlugins()) {
         ;
      }

   }

   private void findGemsPluginByFile() {
      try {
         File pluginsFolder = new File("plugins");
         File[] jarFiles;
         if (pluginsFolder.exists() && (jarFiles = pluginsFolder.listFiles((dir, name) -> name.toLowerCase().contains("gem") && name.endsWith(".jar"))) != null) {
            for(File var10000 : jarFiles) {
               ;
            }
         }
      } catch (Exception var7) {
      }

   }

   private void setupGemsIntegration() {
      try {
         this.debugAvailablePlugins();
         this.findGemsPluginByFile();
         Plugin gems = this.getServer().getPluginManager().getPlugin("Shards");
         if (gems == null) {
            gems = this.getServer().getPluginManager().getPlugin("Shards");
         }

         if (gems != null && gems.isEnabled()) {
            this.gemsPlugin = gems;
            this.gemsIntegrationEnabled = true;
         } else {
            this.gemsIntegrationEnabled = false;
         }
      } catch (Exception var2) {
         this.gemsIntegrationEnabled = false;
      }

   }

   public boolean processGemsCommand(Player player, String command, int price) {
      if (this.gemsIntegrationEnabled && this.gemsPlugin != null) {
         try {
            UUID playerUUID = player.getUniqueId();
            if (command.contains("take") || command.contains("remove")) {
               boolean success = this.executeGemsMethod("takeGems", playerUUID, price);
               if (!success) {
                  String actionBarMessage = this.langManager.getActionBar("not-enough-shards");
                  String chatMessage = this.langManager.getMessage("not-enough-shards");
                  this.sendActionBar(player, actionBarMessage);
                  player.sendMessage(HexColor.translateHexCodes(chatMessage));
               }

               return success;
            }

            if (command.contains("give") || command.contains("add")) {
               return this.executeGemsMethod("addGems", playerUUID, price);
            }

            if (command.contains("get") || command.contains("check")) {
               Object gemsResult = this.executeGemsMethodWithReturn("getGems", playerUUID);
               if (gemsResult instanceof Integer) {
                  int gems = (Integer)gemsResult;
                  return true;
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean executeGemsMethod(String methodName, UUID playerUUID, int amount) {
      try {
         Class<?> gemsClass = this.gemsPlugin.getClass();
         Method method = gemsClass.getMethod(methodName, UUID.class, Integer.TYPE);
         Object result = method.invoke(this.gemsPlugin, playerUUID, amount);
         return result instanceof Boolean ? (Boolean)result : false;
      } catch (Exception var7) {
         return false;
      }
   }

   private Object executeGemsMethodWithReturn(String methodName, UUID playerUUID) {
      try {
         Class<?> gemsClass = this.gemsPlugin.getClass();
         Method method = gemsClass.getMethod(methodName, UUID.class);
         return method.invoke(this.gemsPlugin, playerUUID);
      } catch (Exception var5) {
         return 0;
      }
   }

   public boolean isGemsIntegrationEnabled() {
      return this.gemsIntegrationEnabled;
   }

   public void reloadGemsIntegration() {
      this.setupGemsIntegration();
   }

   private void ensureAllResources() {
      File guiFolder = new File(this.getDataFolder(), "gui");
      if (!guiFolder.exists()) {
         guiFolder.mkdirs();
      }

      this.shopsFolder = new File(this.getDataFolder(), "shops");
      if (!this.shopsFolder.exists()) {
         this.shopsFolder.mkdirs();
      }

      this.createConfigIfNotExists();
      this.createLangIfNotExists();
      this.createMainGuiIfNotExists();
      this.createBuyGuiIfNotExists();
      this.createShopFilesIfNotExists();
      this.createShardsConfigIfNotExists();
   }

   private void createShardsConfigIfNotExists() {
      try {
         File shardsFile = new File(this.getDataFolder(), "shards.yml");
         if (!shardsFile.exists()) {
            this.saveResource("shards.yml", false);
         }
      } catch (Exception var2) {
      }

   }

   private void createConfigIfNotExists() {
      try {
         this.configFile = new File(this.getDataFolder(), "config.yml");
         if (!this.configFile.exists()) {
            this.saveResource("config.yml", false);
         }
      } catch (Exception var2) {
         this.createDefaultConfig();
      }

   }

   private void createLangIfNotExists() {
      try {
         File langFile = new File(this.getDataFolder(), "lang.yml");
         if (!langFile.exists()) {
            this.saveResource("lang.yml", false);
         }
      } catch (Exception var2) {
         this.createDefaultLangConfig();
      }

   }

   private void createMainGuiIfNotExists() {
      try {
         this.mainguiFile = new File(this.getDataFolder(), "gui/maingui.yml");
         if (!this.mainguiFile.exists()) {
            this.saveResource("gui/maingui.yml", false);
         }
      } catch (Exception var2) {
         this.createDefaultMainGuiConfig();
      }

   }

   private void createBuyGuiIfNotExists() {
      try {
         File buyGuiFile = new File(this.getDataFolder(), "gui/buy.yml");
         if (!buyGuiFile.exists()) {
            this.saveResource("gui/buy.yml", false);
         }
      } catch (Exception var2) {
         this.createDefaultBuyConfig();
      }

   }

   private void createShopFilesIfNotExists() {
      String[] shopFiles = new String[]{"nether.yml", "gear.yml", "food.yml", "end.yml"};

      for(String shopFile : shopFiles) {
         try {
            File file = new File(this.shopsFolder, shopFile);
            if (!file.exists()) {
               this.saveResource("shops/" + shopFile, false);
            }
         } catch (Exception var7) {
            this.createDefaultShopFile(shopFile.replace(".yml", ""));
         }
      }

   }

   private void createDefaultBuyConfig() {
      try {
         File buyGuiFile = new File(this.getDataFolder(), "gui/buy.yml");
         File parent = buyGuiFile.getParentFile();
         if (!parent.exists()) {
            parent.mkdirs();
         }

         YamlConfiguration config = new YamlConfiguration();
         String defaultContent = "# Placeholder for buy.yml\n# - %price% - Price of the item\n# - %clicked-item% - The item you clicked to buy\n# - %clicked-name% - The displayname of the item you clicked to buy\n# - %clicked-amount% - The amount of the item you clicked to buy\n# - %amount% - The amount you want to buy\n\n#Fill-Material Settings\nfill:\n  enabled: false\n  material: \"BLACK_STAINED_GLASS_PANE\"\n  display-name:\n  sound: \"ENTITY_VILLAGER_NO\"\n  slots: \"1\"\n\nconfirm-menu:\n  rows: 3\n  title: \"&8ʙᴜʏɪɴɢ\"\nconfirm-purchase:\n  slot: 23\n  material: LIME_STAINED_GLASS_PANE\n  displayname: '&#04fc04ᴄᴏɴғɪʀᴍ'\n  lore:\n    - '&fClick to buy'\ndecline-purchase:\n  displayname: '&#fc0404ᴄᴀɴᴄᴇʟ'\n  slot: 21\n  material: RED_STAINED_GLASS_PANE\n  lore:\n    - '&fClick to cancel'\nitem-purchase:\n  material: '%clicked-item%'\n  slot: 13\n  amount: '%clicked-amount%'\n  lore:\n    - '&fAmount: &a%amount%'\n    - '&fPrice: &a%price%'\nadd1:\n  slot: 15\n  amount: 1\n  action: add1\n  displayname: '&a+1'\n  material: LIME_STAINED_GLASS_PANE\nadd10:\n  action: add10\n  slot: 16\n  material: LIME_STAINED_GLASS_PANE\n  displayname: '&a+10'\n  amount: 10\nadd64:\n  displayname: '&a+64'\n  material: LIME_STAINED_GLASS_PANE\n  action: add64\n  slot: 17\n  amount: 64\nremove1:\n  slot: 11\n  action: remove1\n  displayname: '&c-1'\n  amount: 1\n  material: RED_STAINED_GLASS_PANE\nremove10:\n  material: RED_STAINED_GLASS_PANE\n  slot: 10\n  action: remove10\n  amount: 10\n  displayname: '&c-10'\nremove64:\n  displayname: '&c-64'\n  material: RED_STAINED_GLASS_PANE\n  slot: 9\n  action: remove64\n  amount: 64";
         config.loadFromString(defaultContent);
         config.save(buyGuiFile);
      } catch (Exception var5) {
      }

   }

   public void onDisable() {
      this.pluginEnabled = false;
   }

   private boolean setupEconomy() {
      try {
         if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
         } else {
            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
               return false;
            } else {
               this.economy = (Economy)rsp.getProvider();
               return this.economy != null;
            }
         }
      } catch (Exception var2) {
         return false;
      }
   }

   private void loadConfigurations() {
      try {
         this.loadConfig();
         this.loadGuiTitles();
         this.loadPriceLoreConfig();
         this.loadCooldownConfig();
         this.economyManager.reload();
         this.loadMainGuiConfig();
         this.loadShops();
         this.loadBackButtonConfig();
      } catch (Exception var2) {
         this.createDefaultConfigurations();
      }

   }

   private void createDefaultConfigurations() {
      try {
         this.createDefaultConfig();
         this.createDefaultMainGuiConfig();
         this.createDefaultLangConfig();
         this.createDefaultShopCategories();
         this.loadConfig();
         this.loadGuiTitles();
         this.loadPriceLoreConfig();
         this.loadCooldownConfig();
         this.economyManager.reload();
         this.loadMainGuiConfig();
         this.initializeCategories();
         this.loadBackButtonConfig();
      } catch (Exception var2) {
         this.initializeDefaultCategories();
      }

   }

   private void createDefaultConfig() {
      try {
         File configFile = new File(this.getDataFolder(), "config.yml");
         if (!configFile.exists()) {
            YamlConfiguration config = new YamlConfiguration();
            String defaultContent = "####################################################################\n####################################################################\n####################################################################\n#██    ██ ██    ██ ██      ██ ███    ███  █████  ██ ████████ ███████\n#██    ██ ██    ██ ██      ██ ████  ████ ██   ██ ██    ██    ██\n#██    ██ ██    ██ ██      ██ ██ ████ ██ ███████ ██    ██    █████\n#██  ██   ██  ██  ██      ██ ██  ██  ██ ██   ██ ██    ██    ██\n#████     ████   ███████ ██ ██      ██ ██   ██ ██    ██    ███████\n####################################################################\n####################################################################\n####################################################################\n\n#This Plugin is from EliVB\n\n#This is a Plugin like DonutSMP Shop, you can change all of the messages.\n#For more Premium Skripts or Plugins join our Discord: \"https://discord.gg/V3fcWZrccj\"\n\n#permissions\n#shop.reload\n\n#Plugin Placeholders\n#%shop_total_items% - Total items in the shop\n#%shop_total_buyed% - Total money spend in the shop\n#%shop_formatted_total% - Formatted total money spend in the shop\n#%shop_buyed_money_1% - Total money spend by the player\n#%shop_buyed_items_1% - Total items buyed by the player\n#%shop_potion_me_money% - Rank of the player by money spend\n#%shop_potion_me_items% - Rank of the player by items buyed\n\n#Cooldown in seconds, this cooldown is buy cooldown\ncooldown: 3\n\n#You can change here the price lore and the lore line of the price in the item lore\nprice-lore: \"&a%price%\"\nlore-line: 1\n\neconomy:\n  provider: VAULT\n  currency-format: \"#,##0.##\"\n  abbreviations:\n    enabled: true #Accept 100k, 10k, 1m, etc... in the sign of price.\n    formats:\n      - 'k'\n      - 'm'\n      - 'b'\n      - 't'\n\ngui-titles:\n  end: '&8ѕʜᴏᴘ - ᴇɴᴅ'\n  ausrüstung: '&8ѕʜᴏᴘ - ɢᴇᴀʀ'\n  food: '&8ѕʜᴏᴘ - ꜰᴏᴏᴅ'\n  nether: '&8ѕʜᴏᴘ - ɴᴇᴛʜᴇʀ'\n\n#Here you can change the back button in the GUI\nback-button:\n  displayname: '&#FF0000ʙᴀᴄᴋ'\n  material: RED_STAINED_GLASS_PANE\n  lore:\n    - '&fClick to return'\n\n#This Plugin supports custom sounds, you can change them here\nsounds:\n  click-sound: \"ui.button.click\"\n  decline-sound: \"entity.villager.no\"\n  buy-sound: \"entity.experience_orb.pickup\"\n  open-sound: \"\"";
            config.loadFromString(defaultContent);
            config.save(configFile);
         }
      } catch (Exception var4) {
      }

   }

   private void createDefaultLangConfig() {
      try {
         File langFile = new File(this.getDataFolder(), "lang.yml");
         if (!langFile.exists()) {
            YamlConfiguration langConfig = new YamlConfiguration();
            String defaultContent = "#Here you can change all the messages and actions bars of the plugin\n\nmessages:\n  no-permission: '&#FF0000Du hast keine Berechtigung dafür!'\n  player-only: '&#FF0000Nur Spieler können diesen Befehl verwenden!'\n  reload-success: '&#00FF00Shop-Konfiguration wurde neu geladen!'\n  category-unavailable: '&#FF0000Diese Kategorie ist derzeit nicht verfügbar!'\n  shop-unavailable: '&#FF0000Der Shop ist derzeit nicht verfügbar!'\n  not-enough-money: '&#FF0000Du hast nicht genug Geld!'\n  not-enough-gems: '&#FF0000Du hast nicht genug Gems!'\n  purchase-success: '&fDu hast &#11B0E3{item} &ffür &#00FF00{price} &fgekauft'\n  purchase-gems: \"Du hast &#11B0E3{item} &ffür &#A303F9{price} Gems &fgekauft\"\n  transaction-failed: '&#FF0000Transaktion fehlgeschlagen: {error}!'\n  inventory-full: '&#FF0000Dein Inventar ist voll!'\n  cooldown: '&cBitte warte noch &e{time} &cSekunden bevor du wieder kaufst!'\n\naction-bars:\n  reload-success: '&#00FF00Shop-Konfiguration wurde neu geladen!'\n  not-enough-gems: '&#FF0000Du hast nicht genug Gems!'\n  not-enough-money: '&#FF0000Du hast nicht genug Geld!'\n  purchase-success: '&fDu hast &#11B0E3{item} &ffür &#00FF00{price} &fgekauft'\n  purchase-gems: \"Du hast &#11B0E3{item} &ffür &#A303F9{price} Gems &fgekauft\"\n  transaction-failed: '&#FF0000Transaktion fehlgeschlagen: {error}!'";
            langConfig.loadFromString(defaultContent);
            langConfig.save(langFile);
         }
      } catch (Exception var4) {
      }

   }

   private void loadGuiTitles() {
      try {
         ConfigurationSection var1 = this.config.getConfigurationSection("gui-titles");
      } catch (Exception var2) {
      }

   }

   private void loadPriceLoreConfig() {
      try {
         this.priceLoreFormat = this.config.getString("price-lore", "&a%price%");
         this.priceLoreLine = this.config.getInt("lore-line", 1);
         if (this.priceLoreLine < 1) {
            this.priceLoreLine = 1;
         }
      } catch (Exception var2) {
         this.priceLoreFormat = "&a%price%";
         this.priceLoreLine = 1;
      }

   }

   private void loadCooldownConfig() {
      this.buyCooldown = this.config.getInt("cooldown", 3);
      if (this.buyCooldown < 0) {
         this.buyCooldown = 0;
      }

   }

   public boolean checkCooldown(Player player) {
      if (this.buyCooldown <= 0) {
         return true;
      } else {
         UUID uuid = player.getUniqueId();
         long currentTime = System.currentTimeMillis();
         long lastPurchase = (Long)this.cooldownMap.getOrDefault(uuid, 0L);
         long timeSinceLast = (currentTime - lastPurchase) / 1000L;
         if (timeSinceLast < (long)this.buyCooldown) {
            long remaining = (long)this.buyCooldown - timeSinceLast;
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("time", String.valueOf(remaining));
            String message = this.langManager.getMessage("cooldown", placeholders);
            player.sendMessage(HexColor.translateHexCodes(message));
            this.soundManager.playDeclineSound(player);
            return false;
         } else {
            this.cooldownMap.put(uuid, currentTime);
            return true;
         }
      }
   }

   public int getBuyCooldown() {
      return this.buyCooldown;
   }

   public void clearCooldowns() {
      this.cooldownMap.clear();
   }

   public String getGuiTitleFromConfig(String categoryKey, int currentPage, int totalPages) {
      String title = this.config.getString("gui-titles." + categoryKey);
      if (title == null) {
         return categoryKey + " &8ѕʜᴏᴘ";
      } else {
         String pageDisplay = "";
         if (totalPages > 1) {
            pageDisplay = String.valueOf(currentPage);
         }

         title = title.replace("%page%", pageDisplay);
         title = title.replace("  ", " ").trim();
         return HexColor.translateHexCodes(title);
      }
   }

   private void loadBackButtonConfig() {
      try {
         String displayName = this.config.getString("back-button.displayname", "&#FF0000ʙᴀᴄᴋ");
         String materialName = this.config.getString("back-button.material", "RED_STAINED_GLASS_PANE");
         List<String> lore = this.config.contains("back-button.lore") && this.config.isList("back-button.lore") ? this.config.getStringList("back-button.lore") : Arrays.asList("&fClick to return");

         Material material;
         try {
            material = Material.valueOf(materialName.toUpperCase());
         } catch (IllegalArgumentException var9) {
            material = Material.RED_STAINED_GLASS_PANE;
         }

         this.backButton = new ItemStack(material, 1);
         ItemMeta meta = this.backButton.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(HexColor.translateHexCodes(displayName));
            ArrayList<String> formattedLore = new ArrayList();

            for(String line : lore) {
               formattedLore.add(HexColor.translateHexCodes(line));
            }

            meta.setLore(formattedLore);
            this.backButton.setItemMeta(meta);
         }
      } catch (Exception var10) {
         this.createDefaultBackButton();
      }

   }

   private void createDefaultBackButton() {
      this.backButton = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
      ItemMeta meta = this.backButton.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColor.translateHexCodes("&#FF0000ʙᴀᴄᴋ"));
         meta.setLore(Arrays.asList(HexColor.translateHexCodes("&fClick to return")));
         this.backButton.setItemMeta(meta);
      }

   }

   private void loadConfig() {
      try {
         this.configFile = new File(this.getDataFolder(), "config.yml");
         if (!this.configFile.exists()) {
            this.saveDefaultConfig();
         }

         this.config = YamlConfiguration.loadConfiguration(this.configFile);
         this.reloadConfig();
      } catch (Exception var2) {
         this.config = new YamlConfiguration();
         this.initializeDefaultCategories();
      }

   }

   private void loadMainGuiConfig() {
      try {
         this.mainguiFile = new File(this.getDataFolder(), "gui/maingui.yml");
         if (!this.mainguiFile.exists()) {
            this.createDefaultMainGuiConfig();
         }

         this.mainguiConfig = YamlConfiguration.loadConfiguration(this.mainguiFile);
         this.mainMenuTitle = this.mainguiConfig.getString("main-menu.title", "&8ѕʜᴏᴘ");
         this.mainMenuTitle = HexColor.translateHexCodes(this.mainMenuTitle);
         this.mainMenuRows = this.mainguiConfig.getInt("main-menu.rows", 3);
         if (this.mainguiConfig.contains("main-menu.fill")) {
            this.fillEnabled = this.mainguiConfig.getBoolean("main-menu.fill.enabled", false);
            if (this.fillEnabled) {
               String materialName = this.mainguiConfig.getString("main-menu.fill.material", "BLACK_STAINED_GLASS_PANE");
               Material fillMaterial = Material.BLACK_STAINED_GLASS_PANE;

               try {
                  fillMaterial = Material.valueOf(materialName.toUpperCase());
               } catch (IllegalArgumentException var5) {
               }

               this.fillItem = new ItemStack(fillMaterial, 1);
               ItemMeta meta = this.fillItem.getItemMeta();
               if (meta != null) {
                  String displayName = this.mainguiConfig.getString("main-menu.fill.display-name", "");
                  if (displayName != null && !displayName.trim().isEmpty()) {
                     meta.setDisplayName(HexColor.translateHexCodes(displayName));
                  }

                  meta.setLore(new ArrayList());
                  this.fillItem.setItemMeta(meta);
               }

               String slotsConfig = this.mainguiConfig.getString("main-menu.fill.slots", "");
               if (slotsConfig != null && !slotsConfig.trim().isEmpty()) {
                  this.fillSlots = slotsConfig.split(",");
               }
            }
         }
      } catch (Exception e) {
         this.mainMenuTitle = "&8ѕʜᴏᴘ";
         this.mainMenuRows = 3;
         this.fillEnabled = false;
         e.printStackTrace();
      }

   }

   private void createDefaultMainGuiConfig() {
      try {
         File guiFolder = new File(this.getDataFolder(), "gui");
         if (!guiFolder.exists()) {
            guiFolder.mkdirs();
         }

         this.mainguiFile = new File(guiFolder, "maingui.yml");
         if (!this.mainguiFile.exists()) {
            YamlConfiguration config = new YamlConfiguration();
            String defaultContent = "main-menu:\n  title: '&8ѕʜᴏᴘ'\n  rows: 3\n\n  #Fill-Material Settings\n  fill:\n    enabled: false\n    material: \"BLACK_STAINED_GLASS_PANE\"\n    display-name: \"\"\n    sound: \"ENTITY_VILLAGER_NO\"\n    slots: \"1-9\"\n\n  categories:\n    end:\n      displayname: '&#00fc88ᴇɴᴅ'\n      material: END_STONE\n      action: end\n      slot: 11\n      lore:\n        - '&fClick to view the end shop'\n\n    nether:\n      displayname: '&#00fc88ɴᴇᴛʜᴇʀ'\n      material: NETHERRACK\n      action: nether\n      slot: 12\n      lore:\n        - '&fClick to view the nether shop'\n\n    gear:\n      displayname: '&#00fc88ɢᴇᴀʀ'\n      material: TOTEM_OF_UNDYING\n      action: gear\n      slot: 13\n      lore:\n        - '&fClick to view the gear shop'\n\n    food:\n      displayname: '&#00fc88ꜰᴏᴏᴅ'\n      material: COOKED_BEEF\n      action: food\n      slot: 14\n      lore:\n        - '&fClick to view the food shop'";
            config.loadFromString(defaultContent);
            config.save(this.mainguiFile);
         }
      } catch (Exception var4) {
      }

   }

   private void loadShops() {
      try {
         if (this.shopsFolder == null) {
            this.shopsFolder = new File(this.getDataFolder(), "shops");
         }

         if (!this.shopsFolder.exists()) {
            this.shopsFolder.mkdirs();
            this.createDefaultShopCategories();
         }

         this.initializeCategories();
      } catch (Exception var2) {
      }

   }

   private void createDefaultShopCategories() {
      String[] defaultCategories = new String[]{"end", "nether", "gear", "food"};

      for(String category : defaultCategories) {
         File categoryFile = new File(this.shopsFolder, category + ".yml");
         if (!categoryFile.exists()) {
            this.createDefaultShopFile(category);
         }
      }

   }

   public void reloadConfig() {
      try {
         if (this.configFile == null) {
            this.configFile = new File(this.getDataFolder(), "config.yml");
         }

         this.config = YamlConfiguration.loadConfiguration(this.configFile);
         this.loadGuiTitles();
         this.loadPriceLoreConfig();
         this.loadCooldownConfig();
         this.economyManager.reload();
         this.loadMainGuiConfig();
         this.loadBackButtonConfig();
         this.soundManager.loadSounds();
         if (this.confirmMenu != null) {
            this.confirmMenu.reloadConfig();
         }

         if (this.shardConfirmMenu != null) {
            this.shardConfirmMenu.reloadConfig();
         }

         if (this.shardsManager != null) {
            this.shardsManager.reloadConfig();
         }

         this.reloadGemsIntegration();
         this.initializeCategories();
         this.clearCooldowns();
      } catch (Exception var2) {
      }

   }

   private void initializeCategories() {
      this.categories.clear();
      this.categoryInventories.clear();
      if (this.mainguiConfig == null) {
         this.loadMainGuiConfig();
      } else {
         ConfigurationSection categoriesSection = this.mainguiConfig.getConfigurationSection("main-menu.categories");
         if (categoriesSection == null) {
            this.initializeDefaultCategories();
         } else {
            try {
               Set<String> categoryKeys = categoriesSection.getKeys(false);
               if (categoryKeys == null || categoryKeys.isEmpty()) {
                  this.initializeDefaultCategories();
                  return;
               }

               for(String key : categoryKeys) {
                  String path = "main-menu.categories." + key;
                  String displayName = this.mainguiConfig.getString(path + ".displayname", "&c" + key);
                  String materialName = this.mainguiConfig.getString(path + ".material", "CHEST");
                  int slot = this.mainguiConfig.getInt(path + ".slot", 0);
                  String action = this.mainguiConfig.getString(path + ".action", key);
                  String command = this.mainguiConfig.getString(path + ".command", (String)null);
                  List<String> lore = this.mainguiConfig.getStringList(path + ".lore");
                  List<String> enchantments = this.mainguiConfig.getStringList(path + ".enchant");

                  Material material;
                  try {
                     material = Material.valueOf(materialName.toUpperCase());
                  } catch (IllegalArgumentException var21) {
                     material = Material.CHEST;
                  }

                  ShopCategory category = new ShopCategory(displayName, material, slot, action);
                  if (command != null && !command.isEmpty()) {
                     category.setCommand(command);
                  }

                  if (lore != null && !lore.isEmpty()) {
                     category.setLore(lore);
                  }

                  if (enchantments != null && !enchantments.isEmpty()) {
                     for(String enchantStr : enchantments) {
                        try {
                           String[] parts = enchantStr.split(":");
                           if (parts.length == 2) {
                              Enchantment enchant = Enchantment.getByName(parts[0].toUpperCase());
                              int level = Integer.parseInt(parts[1]);
                              if (enchant != null) {
                                 category.addEnchantment(enchant, level);
                              }
                           }
                        } catch (Exception var20) {
                        }
                     }
                  }

                  this.categories.put(key, category);
                  if (!"command".equalsIgnoreCase(action)) {
                     this.loadCategoryItems(action);
                  }
               }
            } catch (Exception var22) {
               this.initializeDefaultCategories();
            }

         }
      }
   }

   private void loadCategoryItems(String categoryKey) {
      if (this.shopsFolder == null) {
         this.shopsFolder = new File(this.getDataFolder(), "shops");
      }

      File categoryFile;
      if (!(categoryFile = new File(this.shopsFolder, categoryKey.toLowerCase() + ".yml")).exists()) {
         this.createDefaultShopFile(categoryKey);
      } else {
         try {
            YamlConfiguration categoryConfig = YamlConfiguration.loadConfiguration(categoryFile);
            CategoryInventory categoryInventory = new CategoryInventory(categoryKey);
            boolean categoryPremium = categoryConfig.getBoolean("premium", false);
            String categoryPermission = categoryConfig.getString("permission", "");
            categoryInventory.setPremium(categoryPremium);
            categoryInventory.setPermission(categoryPermission);
            int rows = categoryConfig.getInt("rows", 3);
            rows = Math.max(1, Math.min(6, rows));
            categoryInventory.setRows(rows);
            int totalPages = categoryConfig.getInt("pages", 1);
            totalPages = Math.max(1, totalPages);
            categoryInventory.setTotalPages(totalPages);
            if (categoryConfig.contains("icons")) {
               int prevSlot = categoryConfig.getInt("icons.previous-page-slot", 19);
               categoryInventory.setPreviousPageSlot(prevSlot);
               if (categoryConfig.contains("icons.previous-page")) {
                  String prevMaterial = categoryConfig.getString("icons.previous-page.material", "ARROW");
                  String prevDisplayName = categoryConfig.getString("icons.previous-page.displayname", "&#00fc88ᴘʀᴇᴠɪᴏᴜꜱ");
                  List<String> prevLore = categoryConfig.getStringList("icons.previous-page.lore");

                  try {
                     Material mat = Material.valueOf(prevMaterial.toUpperCase());
                     ItemStack prevItem = new ItemStack(mat, 1);
                     ItemMeta meta = prevItem.getItemMeta();
                     if (meta != null) {
                        meta.setDisplayName(HexColor.translateHexCodes(prevDisplayName));
                        if (prevLore != null && !prevLore.isEmpty()) {
                           List<String> formattedLore = new ArrayList();

                           for(String line : prevLore) {
                              formattedLore.add(HexColor.translateHexCodes(line));
                           }

                           meta.setLore(formattedLore);
                        }

                        prevItem.setItemMeta(meta);
                     }

                     categoryInventory.setPreviousPageItem(prevItem);
                  } catch (IllegalArgumentException var40) {
                  }
               }

               int nextSlot = categoryConfig.getInt("icons.next-page-slot", 26);
               categoryInventory.setNextPageSlot(nextSlot);
               if (categoryConfig.contains("icons.next-page")) {
                  String nextMaterial = categoryConfig.getString("icons.next-page.material", "ARROW");
                  String nextDisplayName = categoryConfig.getString("icons.next-page.displayname", "&#00fc88ɴᴇxᴛ");
                  List<String> nextLore = categoryConfig.getStringList("icons.next-page.lore");

                  try {
                     Material mat = Material.valueOf(nextMaterial.toUpperCase());
                     ItemStack nextItem = new ItemStack(mat, 1);
                     ItemMeta meta = nextItem.getItemMeta();
                     if (meta != null) {
                        meta.setDisplayName(HexColor.translateHexCodes(nextDisplayName));
                        if (nextLore != null && !nextLore.isEmpty()) {
                           List<String> formattedLore = new ArrayList();

                           for(String line : nextLore) {
                              formattedLore.add(HexColor.translateHexCodes(line));
                           }

                           meta.setLore(formattedLore);
                        }

                        nextItem.setItemMeta(meta);
                     }

                     categoryInventory.setNextPageItem(nextItem);
                  } catch (IllegalArgumentException var39) {
                  }
               }
            }

            if (!categoryConfig.contains("items")) {
               this.categoryInventories.put(categoryKey, categoryInventory);
            } else {
               ConfigurationSection itemsSection = categoryConfig.getConfigurationSection("items");
               if (itemsSection == null) {
                  this.categoryInventories.put(categoryKey, categoryInventory);
               } else {
                  Set<String> itemKeys = itemsSection.getKeys(false);
                  if (itemKeys == null || itemKeys.isEmpty()) {
                     this.categoryInventories.put(categoryKey, categoryInventory);
                  } else {
                     Iterator var48 = itemKeys.iterator();

                     while(true) {
                        String path;
                        String serializedItem;
                        Material material;
                        while(true) {
                           if (!var48.hasNext()) {
                              this.categoryInventories.put(categoryKey, categoryInventory);
                              return;
                           }

                           String itemKey = (String)var48.next();
                           path = "items." + itemKey;
                           serializedItem = categoryConfig.getString(path + ".serialized-item");
                           if (serializedItem != null && !serializedItem.isEmpty()) {
                              String materialName = categoryConfig.getString(path + ".material");
                              if (materialName != null) {
                                 try {
                                    material = Material.valueOf(materialName.toUpperCase());
                                    break;
                                 } catch (IllegalArgumentException var38) {
                                 }
                              }
                           } else {
                              String materialName = categoryConfig.getString(path + ".material");
                              if (materialName != null) {
                                 Material material;
                                 try {
                                    material = Material.valueOf(materialName.toUpperCase());
                                 } catch (IllegalArgumentException var37) {
                                    continue;
                                 }

                                 String displayName = categoryConfig.getString(path + ".displayname");
                                 String priceString = categoryConfig.getString(path + ".price", "0");
                                 double price = this.economyManager.parsePrice(priceString);
                                 int amount = categoryConfig.getInt(path + ".amount", 1);
                                 List<String> lore = categoryConfig.getStringList(path + ".lore");
                                 List<String> enchantments = categoryConfig.getStringList(path + ".enchant");
                                 int slot = categoryConfig.getInt(path + ".slot", -1);
                                 int page = categoryConfig.getInt(path + ".page", 1);
                                 String potionEffect = categoryConfig.getString(path + ".effect");
                                 int potionDuration = categoryConfig.getInt(path + ".duration", 3600);
                                 int potionAmplifier = categoryConfig.getInt(path + ".amplifier", 0);
                                 boolean potionUpgraded = categoryConfig.getBoolean(path + ".upgraded", false);
                                 List<String> purchaseCommands = categoryConfig.getStringList(path + ".command");
                                 ArrayList<String> formattedLore = new ArrayList();
                                 if (lore != null) {
                                    for(String line : lore) {
                                       formattedLore.add(HexColor.translateHexCodes(line));
                                    }
                                 }

                                 ShopItem shopItem = new ShopItem(material, price, amount, displayName, formattedLore, slot);
                                 shopItem.setPage(page);
                                 if (potionEffect != null && !potionEffect.isEmpty()) {
                                    shopItem.setPotionEffect(potionEffect);
                                    shopItem.setPotionDuration(potionDuration);
                                    shopItem.setPotionAmplifier(potionAmplifier);
                                    shopItem.setPotionUpgraded(potionUpgraded);
                                 }

                                 if (purchaseCommands != null && !purchaseCommands.isEmpty()) {
                                    shopItem.setPurchaseCommands(purchaseCommands);
                                 }

                                 ArrayList<String> originalLoreWithoutPrice = new ArrayList(formattedLore);
                                 shopItem.setOriginalLore(originalLoreWithoutPrice);
                                 List<String> displayLore = this.createDisplayLore(shopItem, price, enchantments, originalLoreWithoutPrice);
                                 shopItem.setLore(displayLore);
                                 String priceLore = this.createPriceLore(price);
                                 shopItem.setPriceLore(priceLore);
                                 categoryInventory.addItem(shopItem);
                              }
                           }
                        }

                        String displayName = categoryConfig.getString(path + ".displayname");
                        String priceString = categoryConfig.getString(path + ".price", "0");
                        double price = this.economyManager.parsePrice(priceString);
                        int amount = categoryConfig.getInt(path + ".amount", 1);
                        List<String> lore = categoryConfig.getStringList(path + ".lore");
                        int slot = categoryConfig.getInt(path + ".slot", -1);
                        int page = categoryConfig.getInt(path + ".page", 1);
                        ArrayList<String> formattedLore = new ArrayList();
                        if (lore != null) {
                           for(String line : lore) {
                              formattedLore.add(HexColor.translateHexCodes(line));
                           }
                        }

                        ShopItem shopItem = new ShopItem(material, price, amount, displayName, formattedLore, slot);
                        shopItem.setPage(page);
                        shopItem.setSerializedItemStack(serializedItem);
                        String priceLore = this.createPriceLore(price);
                        shopItem.setPriceLore(priceLore);
                        categoryInventory.addItem(shopItem);
                     }
                  }
               }
            }
         } catch (Exception exception) {
            exception.printStackTrace();
         }
      }
   }

   private List<String> createDisplayLore(ShopItem shopItem, double price, List<String> enchantments, List<String> originalLore) {
      ArrayList<String> displayLore = new ArrayList();
      boolean hasEnchantments = enchantments != null && !enchantments.isEmpty();
      if (hasEnchantments) {
         for(String enchantStr : enchantments) {
            try {
               String[] parts = enchantStr.split(":");
               if (parts.length == 2) {
                  Enchantment enchant = Enchantment.getByName(parts[0].toUpperCase());
                  int level = Integer.parseInt(parts[1]);
                  if (enchant != null) {
                     String enchantName = this.getEnchantmentDisplayName(enchant);
                     String romanLevel = this.toRoman(level);
                     displayLore.add("&7" + enchantName + " " + romanLevel);
                     shopItem.addEnchantment(enchant, level);
                  }
               }
            } catch (Exception var15) {
            }
         }
      }

      if (originalLore != null && !originalLore.isEmpty()) {
         if (hasEnchantments && !displayLore.isEmpty()) {
            displayLore.add("");
         }

         displayLore.addAll(originalLore);
      }

      String priceLore = this.createPriceLore(price);
      if (!displayLore.isEmpty()) {
         displayLore.add("");
      }

      displayLore.add(priceLore);
      return displayLore;
   }

   private String createPriceLore(double price) {
      String formattedPrice = this.economy != null && this.economyEnabled ? this.economyManager.formatMoneyWithCurrency(price, this.economy.currencyNamePlural()) : this.economyManager.formatMoneyWithCurrency(price, "$");
      String lore = this.priceLoreFormat.replace("%price%", formattedPrice);
      return HexColor.translateHexCodes(lore);
   }

   private void createDefaultShopFile(String categoryKey) {
      try {
         if (this.shopsFolder == null) {
            this.shopsFolder = new File(this.getDataFolder(), "shops");
         }

         File categoryFile = new File(this.shopsFolder, categoryKey + ".yml");
         if (categoryFile.exists()) {
            return;
         }

         YamlConfiguration defaultConfig = new YamlConfiguration();
         switch (categoryKey.toLowerCase()) {
            case "gear":
               defaultConfig.set("items.obsidian.material", "OBSIDIAN");
               defaultConfig.set("items.obsidian.price", 100);
               defaultConfig.set("items.obsidian.slot", 9);
               defaultConfig.set("items.obsidian.amount", 1);
               defaultConfig.set("items.end-crystal.material", "END_CRYSTAL");
               defaultConfig.set("items.end-crystal.price", 350);
               defaultConfig.set("items.end-crystal.slot", 10);
               defaultConfig.set("items.end-crystal.amount", 1);
               defaultConfig.set("items.respawn-anchor.material", "RESPAWN_ANCHOR");
               defaultConfig.set("items.respawn-anchor.price", 1000);
               defaultConfig.set("items.respawn-anchor.slot", 11);
               defaultConfig.set("items.respawn-anchor.amount", 1);
               defaultConfig.set("items.glowstone.material", "GLOWSTONE");
               defaultConfig.set("items.glowstone.price", 100);
               defaultConfig.set("items.glowstone.slot", 12);
               defaultConfig.set("items.glowstone.amount", 1);
               defaultConfig.set("items.totem-of-undying.material", "TOTEM_OF_UNDYING");
               defaultConfig.set("items.totem-of-undying.price", 1500);
               defaultConfig.set("items.totem-of-undying.slot", 13);
               defaultConfig.set("items.totem-of-undying.amount", 1);
               defaultConfig.set("items.ender-pearl.material", "ENDER_PEARL");
               defaultConfig.set("items.ender-pearl.price", 75);
               defaultConfig.set("items.ender-pearl.slot", 14);
               defaultConfig.set("items.ender-pearl.amount", 1);
               defaultConfig.set("items.golden-apple.material", "GOLDEN_APPLE");
               defaultConfig.set("items.golden-apple.price", 250);
               defaultConfig.set("items.golden-apple.slot", 15);
               defaultConfig.set("items.golden-apple.amount", 1);
               defaultConfig.set("items.bottle-o-enchanting.material", "EXPERIENCE_BOTTLE");
               defaultConfig.set("items.bottle-o-enchanting.price", 100);
               defaultConfig.set("items.bottle-o-enchanting.slot", 16);
               defaultConfig.set("items.bottle-o-enchanting.amount", 1);
               defaultConfig.set("items.arrow.material", "ARROW");
               defaultConfig.set("items.arrow.price", 500);
               defaultConfig.set("items.arrow.slot", 17);
               defaultConfig.set("items.arrow.amount", 1);
               break;
            case "nether":
               defaultConfig.set("items.blaze-rod.material", "BLAZE_ROD");
               defaultConfig.set("items.blaze-rod.price", 150);
               defaultConfig.set("items.blaze-rod.slot", 9);
               defaultConfig.set("items.blaze-rod.amount", 1);
               defaultConfig.set("items.nether-wart.material", "NETHER_WART");
               defaultConfig.set("items.nether-wart.price", 96);
               defaultConfig.set("items.nether-wart.slot", 10);
               defaultConfig.set("items.nether-wart.amount", 1);
               defaultConfig.set("items.glowstone-dust.material", "GLOWSTONE_DUST");
               defaultConfig.set("items.glowstone-dust.price", 15);
               defaultConfig.set("items.glowstone-dust.slot", 11);
               defaultConfig.set("items.glowstone-dust.amount", 1);
               defaultConfig.set("items.magma-cream.material", "MAGMA_CREAM");
               defaultConfig.set("items.magma-cream.price", 96);
               defaultConfig.set("items.magma-cream.slot", 12);
               defaultConfig.set("items.magma-cream.amount", 1);
               defaultConfig.set("items.ghast-tear.material", "GHAST_TEAR");
               defaultConfig.set("items.ghast-tear.price", 350);
               defaultConfig.set("items.ghast-tear.slot", 13);
               defaultConfig.set("items.ghast-tear.amount", 1);
               defaultConfig.set("items.quartz.material", "QUARTZ");
               defaultConfig.set("items.quartz.price", 30);
               defaultConfig.set("items.quartz.slot", 14);
               defaultConfig.set("items.quartz.amount", 1);
               defaultConfig.set("items.soul-sand.material", "SOUL_SAND");
               defaultConfig.set("items.soul-sand.price", 50);
               defaultConfig.set("items.soul-sand.slot", 15);
               defaultConfig.set("items.soul-sand.amount", 1);
               defaultConfig.set("items.magma-block.material", "MAGMA_BLOCK");
               defaultConfig.set("items.magma-block.price", 35);
               defaultConfig.set("items.magma-block.slot", 16);
               defaultConfig.set("items.magma-block.amount", 1);
               defaultConfig.set("items.crying-obsidian.material", "CRYING_OBSIDIAN");
               defaultConfig.set("items.crying-obsidian.price", 150);
               defaultConfig.set("items.crying-obsidian.slot", 17);
               defaultConfig.set("items.crying-obsidian.amount", 1);
               break;
            case "end":
               defaultConfig.set("items.ender-chest.material", "ENDER_CHEST");
               defaultConfig.set("items.ender-chest.price", 2500);
               defaultConfig.set("items.ender-chest.slot", 9);
               defaultConfig.set("items.ender-chest.amount", 1);
               defaultConfig.set("items.ender-pearl.material", "ENDER_PEARL");
               defaultConfig.set("items.ender-pearl.price", 75);
               defaultConfig.set("items.ender-pearl.slot", 10);
               defaultConfig.set("items.ender-pearl.amount", 1);
               defaultConfig.set("items.end-stone.material", "END_STONE");
               defaultConfig.set("items.end-stone.price", 128);
               defaultConfig.set("items.end-stone.slot", 11);
               defaultConfig.set("items.end-stone.amount", 16);
               defaultConfig.set("items.dragon-breath.material", "DRAGON_BREATH");
               defaultConfig.set("items.dragon-breath.price", 1000);
               defaultConfig.set("items.dragon-breath.slot", 12);
               defaultConfig.set("items.dragon-breath.amount", 1);
               defaultConfig.set("items.end-rod.material", "END_ROD");
               defaultConfig.set("items.end-rod.price", 100);
               defaultConfig.set("items.end-rod.slot", 13);
               defaultConfig.set("items.end-rod.amount", 1);
               defaultConfig.set("items.chorus-fruit.material", "CHORUS_FRUIT");
               defaultConfig.set("items.chorus-fruit.price", 108);
               defaultConfig.set("items.chorus-fruit.slot", 14);
               defaultConfig.set("items.chorus-fruit.amount", 1);
               defaultConfig.set("items.popped-chorus-fruit.material", "POPPED_CHORUS_FRUIT");
               defaultConfig.set("items.popped-chorus-fruit.price", 24);
               defaultConfig.set("items.popped-chorus-fruit.slot", 15);
               defaultConfig.set("items.popped-chorus-fruit.amount", 1);
               defaultConfig.set("items.shulker-shell.material", "SHULKER_SHELL");
               defaultConfig.set("items.shulker-shell.price", 350);
               defaultConfig.set("items.shulker-shell.slot", 16);
               defaultConfig.set("items.shulker-shell.amount", 1);
               defaultConfig.set("items.shulker-box.material", "SHULKER_BOX");
               defaultConfig.set("items.shulker-box.price", 800);
               defaultConfig.set("items.shulker-box.slot", 17);
               defaultConfig.set("items.shulker-box.amount", 1);
               break;
            case "food":
               defaultConfig.set("items.potato.material", "POTATO");
               defaultConfig.set("items.potato.price", 96);
               defaultConfig.set("items.potato.slot", 9);
               defaultConfig.set("items.potato.amount", 1);
               defaultConfig.set("items.sweet-berries.material", "SWEET_BERRIES");
               defaultConfig.set("items.sweet-berries.price", 50);
               defaultConfig.set("items.sweet-berries.slot", 10);
               defaultConfig.set("items.sweet-berries.amount", 1);
               defaultConfig.set("items.melon-slice.material", "MELON_SLICE");
               defaultConfig.set("items.melon-slice.price", 36);
               defaultConfig.set("items.melon-slice.slot", 11);
               defaultConfig.set("items.melon-slice.amount", 1);
               defaultConfig.set("items.carrot.material", "CARROT");
               defaultConfig.set("items.carrot.price", 96);
               defaultConfig.set("items.carrot.slot", 12);
               defaultConfig.set("items.carrot.amount", 1);
               defaultConfig.set("items.apple.material", "APPLE");
               defaultConfig.set("items.apple.price", 25);
               defaultConfig.set("items.apple.slot", 13);
               defaultConfig.set("items.apple.amount", 1);
               defaultConfig.set("items.cooked-chicken.material", "COOKED_CHICKEN");
               defaultConfig.set("items.cooked-chicken.price", 48);
               defaultConfig.set("items.cooked-chicken.slot", 14);
               defaultConfig.set("items.cooked-chicken.amount", 1);
               defaultConfig.set("items.cooked-beef.material", "COOKED_BEEF");
               defaultConfig.set("items.cooked-beef.price", 35);
               defaultConfig.set("items.cooked-beef.slot", 15);
               defaultConfig.set("items.cooked-beef.amount", 1);
               defaultConfig.set("items.golden-carrot.material", "GOLDEN_CARROT");
               defaultConfig.set("items.golden-carrot.price", 120);
               defaultConfig.set("items.golden-carrot.slot", 16);
               defaultConfig.set("items.golden-carrot.amount", 1);
               defaultConfig.set("items.golden-apple.material", "GOLDEN_APPLE");
               defaultConfig.set("items.golden-apple.price", 250);
               defaultConfig.set("items.golden-apple.slot", 17);
               defaultConfig.set("items.golden-apple.amount", 1);
               break;
            default:
               defaultConfig.set("items.default_item.material", "CHEST");
               defaultConfig.set("items.default_item.displayname", "&fDefault Item");
               defaultConfig.set("items.default_item.price", 100);
               defaultConfig.set("items.default_item.amount", 1);
               defaultConfig.set("items.default_item.slot", 13);
         }

         defaultConfig.save(categoryFile);
         this.loadCategoryItems(categoryKey);
      } catch (Exception exception) {
         exception.printStackTrace();
      }

   }

   private void initializeDefaultCategories() {
      this.categories.clear();
      this.categoryInventories.clear();
      ShopCategory end = new ShopCategory("&#00fc88ᴇɴᴅ", Material.END_STONE, 11, "end");
      end.addLoreLine("&fClick to view the end shop");
      this.categories.put("end", end);
      ShopCategory nether = new ShopCategory("&#00fc88ɴᴇᴛʜᴇʀ", Material.NETHERRACK, 12, "nether");
      nether.addLoreLine("&fClick to view the nether shop");
      this.categories.put("nether", nether);
      ShopCategory gear = new ShopCategory("&#00fc88ɢᴇᴀʀ", Material.TOTEM_OF_UNDYING, 13, "gear");
      gear.addLoreLine("&fClick to view the gear shop");
      this.categories.put("gear", gear);
      ShopCategory food = new ShopCategory("&#00fc88ꜰᴏᴏᴅ", Material.COOKED_BEEF, 14, "food");
      food.addLoreLine("&fClick to view the food shop");
      this.categories.put("food", food);

      for(String key : this.categories.keySet()) {
         this.categoryInventories.put(key, new CategoryInventory(key));
         this.loadCategoryItems(key);
      }

   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (cmd.getName().equalsIgnoreCase("shop")) {
         if (!this.pluginEnabled) {
            return true;
         } else if (args.length > 0 && args[0].equalsIgnoreCase("addhanditem")) {
            if (!sender.hasPermission("shop.add")) {
               sender.sendMessage(this.langManager.getMessage("no-permission"));
               return true;
            } else if (!(sender instanceof Player)) {
               sender.sendMessage(this.langManager.getMessage("player-only"));
               return true;
            } else {
               Player player = (Player)sender;
               this.handleAddHandItemCommand(player, args);
               return true;
            }
         } else if (args.length > 0 && args[0].equalsIgnoreCase("removeitem")) {
            if (!sender.hasPermission("shop.remove")) {
               sender.sendMessage(this.langManager.getMessage("no-permission"));
               return true;
            } else if (!(sender instanceof Player)) {
               sender.sendMessage(this.langManager.getMessage("player-only"));
               return true;
            } else {
               Player player = (Player)sender;
               this.handleRemoveItemCommand(player, args);
               return true;
            }
         } else if (args.length > 0 && args[0].equalsIgnoreCase("addshop")) {
            if (!sender.hasPermission("shop.addshop")) {
               sender.sendMessage(this.langManager.getMessage("no-permission"));
               return true;
            } else if (!(sender instanceof Player)) {
               sender.sendMessage(this.langManager.getMessage("player-only"));
               return true;
            } else {
               Player player = (Player)sender;
               this.handleAddShopCommand(player, args);
               return true;
            }
         } else if (args.length > 0 && args[0].equalsIgnoreCase("removeshop")) {
            if (!sender.hasPermission("shop.removeshop")) {
               sender.sendMessage(this.langManager.getMessage("no-permission"));
               return true;
            } else if (!(sender instanceof Player)) {
               sender.sendMessage(this.langManager.getMessage("player-only"));
               return true;
            } else {
               Player player = (Player)sender;
               this.handleRemoveShopCommand(player, args);
               return true;
            }
         } else if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("shop.reload")) {
               sender.sendMessage(this.langManager.getMessage("no-permission"));
               return true;
            } else {
               try {
                  String BRIGHT_CYAN = "\u001b[96m";
                  String BRIGHT_RED = "\u001b[91m";
                  String RESET = "\u001b[0m";
                  this.getLogger().info("\u001b[96mReloading configuration...\u001b[0m");
                  this.reloadConfig();
                  this.getLogger().info("\u001b[91m╠ Reloaded config.yml!\u001b[0m");
                  this.langManager.reload();
                  this.getLogger().info("\u001b[91m╠ Reloaded lang.yml!\u001b[0m");
                  this.loadShops();
                  this.getLogger().info("\u001b[91m╠ Reloaded Shops!\u001b[0m");
                  this.loadMainGuiConfig();
                  this.getLogger().info("\u001b[91m╠ Reloaded Guis!\u001b[0m");
                  this.getLogger().info("\u001b[96m╚ Successful reload!\u001b[0m");
                  this.economyEnabled = this.setupEconomy();
                  if (sender instanceof Player) {
                     Player player = (Player)sender;
                     String actionBarMessage = this.langManager.getActionBar("reload-success");
                     String chatMessage = this.langManager.getMessage("reload-success");
                     this.sendActionBar(player, actionBarMessage);
                     player.sendMessage(chatMessage);
                  } else {
                     sender.sendMessage(this.langManager.getMessage("reload-success"));
                  }
               } catch (Exception var11) {
               }

               return true;
            }
         } else if (!(sender instanceof Player)) {
            sender.sendMessage(this.langManager.getMessage("player-only"));
            return true;
         } else {
            Player player = (Player)sender;
            this.soundManager.playOpenSound(player);
            this.openMainShop(player);
            return true;
         }
      } else {
         return false;
      }
   }

   private void handleRemoveShopCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeshop-usage")));
      } else {
         String slotStr = args[1];

         int slot;
         try {
            slot = Integer.parseInt(slotStr);
            int maxSlots = this.mainMenuRows * 9;
            if (slot < 0 || slot >= maxSlots) {
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("max", String.valueOf(maxSlots - 1));
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeshop-invalid-slot", placeholders)));
               return;
            }
         } catch (NumberFormatException var13) {
            player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeshop-invalid-slot")));
            return;
         }

         String categoryToRemove = null;

         for(Map.Entry<String, ShopCategory> entry : this.categories.entrySet()) {
            if (((ShopCategory)entry.getValue()).getSlot() == slot) {
               categoryToRemove = (String)entry.getKey();
               break;
            }
         }

         if (categoryToRemove == null) {
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("slot", String.valueOf(slot));
            player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeshop-no-shop-found", placeholders)));
         } else {
            try {
               File mainguiFile = new File(this.getDataFolder(), "gui/maingui.yml");
               FileConfiguration mainguiConfig = YamlConfiguration.loadConfiguration(mainguiFile);
               if (mainguiConfig.contains("main-menu.categories." + categoryToRemove)) {
                  mainguiConfig.set("main-menu.categories." + categoryToRemove, (Object)null);
                  mainguiConfig.save(mainguiFile);
               }

               File configFile = new File(this.getDataFolder(), "config.yml");
               FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
               if (config.contains("gui-titles." + categoryToRemove)) {
                  config.set("gui-titles." + categoryToRemove, (Object)null);
                  config.save(configFile);
                  this.config = YamlConfiguration.loadConfiguration(configFile);
               }

               File shopFile = new File(this.shopsFolder, categoryToRemove + ".yml");
               if (shopFile.exists()) {
                  shopFile.delete();
               }

               this.loadMainGuiConfig();
               this.loadGuiTitles();
               this.initializeCategories();
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("slot", String.valueOf(slot));
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeshop-success", placeholders)));
               this.soundManager.playAddItemSound(player);
            } catch (Exception e) {
               e.printStackTrace();
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("error", e.getMessage());
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeshop-failed", placeholders)));
               this.soundManager.playFailedSound(player);
            }

         }
      }
   }

   private void handleAddShopCommand(Player player, String[] args) {
      if (args.length < 6) {
         player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addshop-usage")));
      } else {
         String shopName = args[1].toLowerCase();
         String slotStr = args[2];
         String materialName = args[3].toUpperCase();
         String displayName = args[4];
         String guiName = args[5].toLowerCase();
         if (this.categoryExists(shopName)) {
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("name", shopName);
            player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addshop-already-exists", placeholders)));
         } else {
            int slot;
            try {
               slot = Integer.parseInt(slotStr);
               int maxSlots = this.mainMenuRows * 9;
               if (slot < 0 || slot >= maxSlots) {
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("max", String.valueOf(maxSlots - 1));
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addshop-invalid-slot", placeholders)));
                  return;
               }
            } catch (NumberFormatException var20) {
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addshop-invalid-slot")));
               return;
            }

            Material material;
            try {
               material = Material.valueOf(materialName);
            } catch (IllegalArgumentException var19) {
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("material", materialName);
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addshop-invalid-material", placeholders)));
               return;
            }

            try {
               File configFile = new File(this.getDataFolder(), "config.yml");
               FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
               if (!config.contains("gui-titles." + shopName)) {
                  String existingTitle = config.getString("gui-titles." + guiName);
                  if (existingTitle != null) {
                     config.set("gui-titles." + shopName, existingTitle);
                  } else {
                     String defaultTitle = "" + shopName.toUpperCase();
                     config.set("gui-titles." + shopName, defaultTitle);
                  }

                  config.save(configFile);
                  this.config = YamlConfiguration.loadConfiguration(configFile);
               }

               File mainguiFile = new File(this.getDataFolder(), "gui/maingui.yml");
               FileConfiguration mainguiConfig = YamlConfiguration.loadConfiguration(mainguiFile);
               if (!mainguiConfig.contains("main-menu.categories")) {
                  mainguiConfig.createSection("main-menu.categories");
               }

               String categoryPath = "main-menu.categories." + shopName;
               mainguiConfig.set(categoryPath + ".displayname", displayName);
               mainguiConfig.set(categoryPath + ".material", material.toString());
               mainguiConfig.set(categoryPath + ".action", shopName);
               mainguiConfig.set(categoryPath + ".slot", slot);
               List<String> defaultLore = Arrays.asList(HexColor.translateHexCodes("&fClick to view the " + shopName + " shop"));
               mainguiConfig.set(categoryPath + ".lore", defaultLore);
               mainguiConfig.save(mainguiFile);
               File shopFile = new File(this.shopsFolder, shopName + ".yml");
               if (!shopFile.exists()) {
                  YamlConfiguration shopConfig = new YamlConfiguration();
                  shopConfig.set("rows", 3);
                  shopConfig.set("pages", 1);
                  shopConfig.save(shopFile);
               }

               this.loadMainGuiConfig();
               this.loadGuiTitles();
               this.initializeCategories();
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("name", shopName);
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addshop-success", placeholders)));
               this.soundManager.playAddItemSound(player);
            } catch (Exception e) {
               e.printStackTrace();
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("error", e.getMessage());
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addshop-failed", placeholders)));
               this.soundManager.playFailedSound(player);
            }

         }
      }
   }

   private void handleAddHandItemCommand(Player player, String[] args) {
      if (args.length < 5) {
         player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-usage")));
      } else {
         String shopName = args[1].toLowerCase();
         String priceStr = args[2];
         String slotStr = args[3];
         String pageStr = args[4];
         if (!this.categoryExists(shopName)) {
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("shop", shopName);
            player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-shop-not-exist", placeholders)));
         } else {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem != null && handItem.getType() != Material.AIR) {
               String serializedItem = null;

               try {
                  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                  BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                  dataOutput.writeObject(handItem);
                  dataOutput.close();
                  serializedItem = Base64Coder.encodeLines(outputStream.toByteArray());
               } catch (Exception e) {
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("error", e.getMessage());
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-failed", placeholders)));
                  return;
               }

               double price;
               try {
                  price = this.economyManager.parsePriceFromString(priceStr);
                  if (price <= (double)0.0F) {
                     player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-invalid-price")));
                     return;
                  }
               } catch (NumberFormatException var30) {
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-invalid-price")));
                  return;
               }

               int slot;
               try {
                  slot = Integer.parseInt(slotStr);
                  CategoryInventory categoryInv = (CategoryInventory)this.categoryInventories.get(shopName);
                  int maxSlots = (categoryInv != null ? categoryInv.getRows() : 3) * 9;
                  if (slot < 0 || slot >= maxSlots) {
                     HashMap<String, String> placeholders = new HashMap();
                     placeholders.put("max", String.valueOf(maxSlots - 1));
                     player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-invalid-slot", placeholders)));
                     return;
                  }
               } catch (NumberFormatException var29) {
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-invalid-slot")));
                  return;
               }

               int page;
               try {
                  page = Integer.parseInt(pageStr);
                  if (page < 1) {
                     player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-invalid-page")));
                     return;
                  }
               } catch (NumberFormatException var28) {
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-invalid-page")));
                  return;
               }

               Material material = handItem.getType();
               int amount = handItem.getAmount();
               String displayName = null;
               List<String> lore = new ArrayList();
               Map<Enchantment, Integer> enchantments = new HashMap();
               if (handItem.hasItemMeta()) {
                  ItemMeta meta = handItem.getItemMeta();
                  if (meta.hasDisplayName()) {
                     displayName = meta.getDisplayName();
                  }

                  if (meta.hasLore()) {
                     lore = new ArrayList(meta.getLore());
                  }

                  if (meta.hasEnchants()) {
                     enchantments.putAll(meta.getEnchants());
                  }
               }

               String var10000 = material.toString().toLowerCase();
               String itemKey = var10000 + "_" + System.currentTimeMillis();
               File shopFile = new File(this.shopsFolder, shopName + ".yml");
               FileConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);
               if (!shopConfig.contains("items")) {
                  shopConfig.createSection("items");
               }

               if (!shopConfig.contains("rows")) {
                  shopConfig.set("rows", 3);
               }

               if (!shopConfig.contains("pages")) {
                  int maxPage = 1;
                  ConfigurationSection itemsSection = shopConfig.getConfigurationSection("items");
                  if (itemsSection != null) {
                     for(String key : itemsSection.getKeys(false)) {
                        int itemPage = shopConfig.getInt("items." + key + ".page", 1);
                        if (itemPage > maxPage) {
                           maxPage = itemPage;
                        }
                     }
                  }

                  shopConfig.set("pages", maxPage);
               }

               int currentPages = shopConfig.getInt("pages", 1);
               if (page > currentPages) {
                  shopConfig.set("pages", page);
                  CategoryInventory categoryInv = (CategoryInventory)this.categoryInventories.get(shopName);
                  if (categoryInv != null) {
                     categoryInv.setTotalPages(page);
                  }
               }

               String itemPath = "items." + itemKey;
               shopConfig.set(itemPath + ".material", material.toString());
               shopConfig.set(itemPath + ".price", price);
               shopConfig.set(itemPath + ".slot", slot);
               shopConfig.set(itemPath + ".amount", amount);
               shopConfig.set(itemPath + ".page", page);
               shopConfig.set(itemPath + ".serialized-item", serializedItem);
               if (displayName != null && !displayName.isEmpty()) {
                  shopConfig.set(itemPath + ".displayname", displayName);
               }

               if (!lore.isEmpty()) {
                  shopConfig.set(itemPath + ".lore", lore);
               }

               if (!enchantments.isEmpty()) {
                  List<String> enchantList = new ArrayList();

                  for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                     String var10001 = ((Enchantment)entry.getKey()).getKey().getKey();
                     enchantList.add(var10001 + ":" + String.valueOf(entry.getValue()));
                  }

                  shopConfig.set(itemPath + ".enchant", enchantList);
               }

               try {
                  shopConfig.save(shopFile);
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("shop", shopName);
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-success", placeholders)));
                  this.loadCategoryItems(shopName);
                  this.soundManager.playAddItemSound(player);
               } catch (Exception e) {
                  e.printStackTrace();
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("error", e.getMessage());
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-failed", placeholders)));
                  this.soundManager.playFailedSound(player);
               }

            } else {
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("addhanditem-no-item")));
            }
         }
      }
   }

   private void handleRemoveItemCommand(Player player, String[] args) {
      if (args.length < 4) {
         player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-usage")));
      } else {
         String shopName = args[1].toLowerCase();
         String slotStr = args[2];
         String pageStr = args[3];
         if (!this.categoryExists(shopName)) {
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("shop", shopName);
            player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-shop-not-exist", placeholders)));
         } else {
            int slot;
            try {
               slot = Integer.parseInt(slotStr);
               CategoryInventory categoryInv = (CategoryInventory)this.categoryInventories.get(shopName);
               int maxSlots = (categoryInv != null ? categoryInv.getRows() : 3) * 9;
               if (slot < 0 || slot >= maxSlots) {
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("max", String.valueOf(maxSlots - 1));
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-invalid-slot", placeholders)));
                  return;
               }
            } catch (NumberFormatException var18) {
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-invalid-slot")));
               return;
            }

            int page;
            try {
               page = Integer.parseInt(pageStr);
               if (page < 1) {
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-invalid-page")));
                  return;
               }
            } catch (NumberFormatException var17) {
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-invalid-page")));
               return;
            }

            File shopFile = new File(this.shopsFolder, shopName + ".yml");
            if (!shopFile.exists()) {
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("shop", shopName);
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-shop-not-exist", placeholders)));
            } else {
               FileConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);
               ConfigurationSection itemsSection = shopConfig.getConfigurationSection("items");
               if (itemsSection == null) {
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("slot", String.valueOf(slot));
                  placeholders.put("page", String.valueOf(page));
                  placeholders.put("shop", shopName);
                  player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-item-not-found", placeholders)));
               } else {
                  String itemKeyToRemove = null;

                  for(String itemKey : itemsSection.getKeys(false)) {
                     int itemSlot = shopConfig.getInt("items." + itemKey + ".slot", -1);
                     int itemPage = shopConfig.getInt("items." + itemKey + ".page", 1);
                     if (itemSlot == slot && itemPage == page) {
                        itemKeyToRemove = itemKey;
                        break;
                     }
                  }

                  if (itemKeyToRemove == null) {
                     HashMap<String, String> placeholders = new HashMap();
                     placeholders.put("slot", String.valueOf(slot));
                     placeholders.put("page", String.valueOf(page));
                     placeholders.put("shop", shopName);
                     player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-item-not-found", placeholders)));
                  } else {
                     try {
                        shopConfig.set("items." + itemKeyToRemove, (Object)null);
                        if (shopConfig.getConfigurationSection("items").getKeys(false).isEmpty()) {
                           shopConfig.set("items", (Object)null);
                        }

                        shopConfig.save(shopFile);
                        HashMap<String, String> placeholders = new HashMap();
                        placeholders.put("shop", shopName);
                        placeholders.put("slot", String.valueOf(slot));
                        placeholders.put("page", String.valueOf(page));
                        player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-success", placeholders)));
                        this.loadCategoryItems(shopName);
                        this.soundManager.playAddItemSound(player);
                     } catch (Exception e) {
                        e.printStackTrace();
                        HashMap<String, String> placeholders = new HashMap();
                        placeholders.put("error", e.getMessage());
                        player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("removeitem-failed", placeholders)));
                        this.soundManager.playFailedSound(player);
                     }

                  }
               }
            }
         }
      }
   }

   private void openMainShop(Player player) {
      if (this.categories.isEmpty()) {
         player.sendMessage(this.langManager.getMessage("shop-unavailable"));
      } else {
         int size = this.mainMenuRows * 9;
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, size, this.mainMenuTitle);
         Set<Integer> usedSlots = new HashSet();

         for(ShopCategory category : this.categories.values()) {
            if (category.getSlot() >= 0 && category.getSlot() < size) {
               usedSlots.add(category.getSlot());
            }
         }

         if (this.shardsManager != null && this.shardsManager.isShardsEnabled()) {
            int shardsSlotTemp = this.shardsManager.getMainMenuSlot();
            if (shardsSlotTemp >= 0 && shardsSlotTemp < size) {
               usedSlots.add(shardsSlotTemp);
            }
         }

         if (this.fillEnabled && this.fillItem != null && this.fillSlots.length > 0) {
            Set<Integer> fillSlotSet = new HashSet();

            for(String slotRange : this.fillSlots) {
               try {
                  if (slotRange.contains("-")) {
                     String[] rangeParts = slotRange.split("-");
                     int start = Integer.parseInt(rangeParts[0].trim());
                     int end = Integer.parseInt(rangeParts[1].trim());

                     for(int i = start; i <= end; ++i) {
                        if (i >= 0 && i < size && !usedSlots.contains(i)) {
                           fillSlotSet.add(i);
                        }
                     }
                  } else {
                     int slot = Integer.parseInt(slotRange.trim());
                     if (slot >= 0 && slot < size && !usedSlots.contains(slot)) {
                        fillSlotSet.add(slot);
                     }
                  }
               } catch (NumberFormatException var15) {
               }
            }

            for(int slot : fillSlotSet) {
               inv.setItem(slot, this.fillItem.clone());
            }
         }

         for(ShopCategory category : this.categories.values()) {
            if (category.getSlot() < size) {
               inv.setItem(category.getSlot(), category.getDisplayItem());
            }
         }

         int shardsSlot;
         if (this.shardsManager != null && this.shardsManager.isShardsEnabled() && (shardsSlot = this.shardsManager.getMainMenuSlot()) < size) {
            inv.setItem(shardsSlot, this.shardsManager.getMainMenuItem());
         }

         player.openInventory(inv);
      }
   }

   private void openCategory(Player player, String categoryKey) {
      this.openCategory(player, categoryKey, 1);
   }

   private void openCategory(Player player, String categoryKey, int page) {
      CategoryInventory categoryInventory = (CategoryInventory)this.categoryInventories.get(categoryKey);
      if (categoryInventory == null) {
         player.sendMessage(this.langManager.getMessage("category-unavailable"));
      } else {
         if (categoryInventory.isPremium()) {
            String requiredPerm = categoryInventory.getPermission();
            if (requiredPerm != null && !requiredPerm.isEmpty() && !player.hasPermission(requiredPerm)) {
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("permission", requiredPerm);
               player.sendMessage(HexColor.translateHexCodes(this.langManager.getMessage("no-access", placeholders)));
               return;
            }
         }

         int totalPages = categoryInventory.getTotalPages();
         if (page < 1) {
            page = 1;
         }

         if (page > totalPages) {
            page = totalPages;
         }

         ((HashMap)this.categoryPages.computeIfAbsent(categoryKey, (k) -> new HashMap())).put(player.getUniqueId(), page);
         int rows = categoryInventory.getRows();
         int size = rows * 9;
         String categoryTitle = this.getGuiTitleFromConfig(categoryKey, page, totalPages);
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, size, categoryTitle);
         categoryInventory.populateInventory(inv, page);
         if (page == 1) {
            if (this.backButton != null) {
               int backSlot = this.config.getInt("back-button.slot", size - 5);
               if (backSlot >= 0 && backSlot < size) {
                  inv.setItem(backSlot, this.backButton);
               } else {
                  inv.setItem(size - 1, this.backButton);
               }
            } else {
               this.createDefaultBackButton();
               int backSlot = this.config.getInt("back-button.slot", size - 5);
               if (backSlot >= 0 && backSlot < size) {
                  inv.setItem(backSlot, this.backButton);
               } else {
                  inv.setItem(size - 1, this.backButton);
               }
            }
         }

         player.openInventory(inv);
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      String title = event.getView().getTitle();
      String shardShopTitle = "";
      if (this.shardsManager != null) {
         shardShopTitle = this.shardsManager.getShardShopTitle();
      }

      String shardConfirmTitle = "";
      if (this.shardConfirmMenu != null) {
         shardConfirmTitle = this.shardConfirmMenu.getTitle();
      }

      if (title.equals(this.mainMenuTitle)) {
         event.setCancelled(true);
         if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
            return;
         }

         Player player = (Player)event.getWhoClicked();
         ItemStack clickedItem = event.getCurrentItem();
         if (this.fillEnabled && this.fillItem != null && clickedItem.isSimilar(this.fillItem)) {
            return;
         }

         if (this.shardsManager != null && this.shardsManager.isShardsEnabled() && this.shardsManager.getMainMenuItem().isSimilar(clickedItem)) {
            this.soundManager.playClickSound(player);
            this.shardsManager.openShardShop(player);
            return;
         }

         for(ShopCategory category : this.categories.values()) {
            if (category.getDisplayItem().isSimilar(clickedItem)) {
               this.soundManager.playClickSound(player);
               String action = category.getAction();
               String command = category.getCommand();
               if ("command".equalsIgnoreCase(action) && command != null && !command.isEmpty()) {
                  player.performCommand(command);
               } else {
                  this.openCategory(player, action);
               }
               break;
            }
         }
      } else if (this.isCategoryTitle(title)) {
         if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
         }

         if (event.getCurrentItem() == null) {
            return;
         }

         Player player = (Player)event.getWhoClicked();
         ItemStack clickedItem = event.getCurrentItem();
         int clickedSlot = event.getSlot();
         if (this.backButton != null && clickedItem.isSimilar(this.backButton)) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
               event.setCancelled(true);
            }

            this.soundManager.playClickSound(player);
            this.openMainShop(player);
            return;
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            String categoryName = this.getCategoryNameFromTitle(title);
            if (categoryName.isEmpty()) {
               return;
            }

            CategoryInventory categoryInventory = (CategoryInventory)this.categoryInventories.get(categoryName);
            if (categoryInventory == null) {
               return;
            }

            int currentPage = 1;
            if (this.categoryPages.containsKey(categoryName) && ((HashMap)this.categoryPages.get(categoryName)).containsKey(player.getUniqueId())) {
               currentPage = (Integer)((HashMap)this.categoryPages.get(categoryName)).get(player.getUniqueId());
            }

            if (categoryInventory.getPreviousPageItem() != null && clickedItem.isSimilar(categoryInventory.getPreviousPageItem()) && clickedSlot == categoryInventory.getPreviousPageSlot() && currentPage > 1) {
               this.soundManager.playClickSound(player);
               this.openCategory(player, categoryName, currentPage - 1);
               return;
            }

            if (categoryInventory.getNextPageItem() != null && clickedItem.isSimilar(categoryInventory.getNextPageItem()) && clickedSlot == categoryInventory.getNextPageSlot() && currentPage < categoryInventory.getTotalPages()) {
               this.soundManager.playClickSound(player);
               this.openCategory(player, categoryName, currentPage + 1);
               return;
            }

            if (!clickedItem.hasItemMeta()) {
               return;
            }

            ShopItem shopItem;
            if (categoryInventory != null && (shopItem = categoryInventory.getItem(clickedItem)) != null) {
               this.soundManager.playClickSound(player);
               UUID playerId = player.getUniqueId();
               this.pendingPurchases.put(playerId, new PurchaseData(this, shopItem, clickedItem, clickedItem.getAmount(), categoryName));
               if (this.confirmMenu != null) {
                  this.confirmMenu.openConfirmMenu(player, shopItem, clickedItem);
               } else {
                  this.processPurchase(player, shopItem, clickedItem.getAmount());
               }

               return;
            }
         }
      } else if (this.confirmMenu != null && title.equals(this.confirmMenu.getTitle())) {
         if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
         }

         if (!(event.getWhoClicked() instanceof Player)) {
            return;
         }

         Player player = (Player)event.getWhoClicked();
         ItemStack clickedItem = event.getCurrentItem();
         if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            UUID playerId = player.getUniqueId();
            PurchaseData purchaseData = (PurchaseData)this.pendingPurchases.get(playerId);
            if (purchaseData == null) {
               return;
            }

            if (event.getSlot() == 13) {
               this.soundManager.playClickSound(player);
               return;
            }

            this.handleConfirmMenuAction(player, purchaseData, clickedItem, event.getSlot());
         }
      } else if (this.shardsManager != null && title.equals(shardShopTitle)) {
         event.setCancelled(true);
         if (event.getCurrentItem() == null) {
            return;
         }

         Player player = (Player)event.getWhoClicked();
         ItemStack clickedItem = event.getCurrentItem();
         int clickedSlot = event.getSlot();
         if (this.backButton != null && clickedItem.isSimilar(this.backButton)) {
            this.soundManager.playClickSound(player);
            this.openMainShop(player);
            return;
         }

         if (!clickedItem.hasItemMeta()) {
            return;
         }

         ShardsManager.ShardShopItem shardItem = this.shardsManager.getShardItem(clickedItem, clickedSlot);
         if (shardItem != null) {
            this.soundManager.playClickSound(player);
            if (this.shardConfirmMenu != null) {
               UUID playerId = player.getUniqueId();
               this.pendingShardPurchases.put(playerId, new ShardPurchaseData(this, shardItem, clickedItem, clickedItem.getAmount()));
               this.shardConfirmMenu.openConfirmMenu(player, shardItem, clickedItem);
            } else {
               boolean success = shardItem.purchaseItem(player, clickedItem.getAmount());
               if (success) {
                  this.soundManager.playBuySound(player);
               } else {
                  this.soundManager.playDeclineSound(player);
               }
            }

            return;
         }
      } else if (this.shardConfirmMenu != null && title.equals(shardConfirmTitle)) {
         if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
         }

         if (!(event.getWhoClicked() instanceof Player)) {
            return;
         }

         Player player = (Player)event.getWhoClicked();
         ItemStack clickedItem = event.getCurrentItem();
         if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            UUID playerId = player.getUniqueId();
            ShardPurchaseData purchaseData = (ShardPurchaseData)this.pendingShardPurchases.get(playerId);
            if (purchaseData == null) {
               return;
            }

            String action = this.shardConfirmMenu.getButtonAction(event.getSlot());
            if (action == null) {
               return;
            }

            switch (action) {
               case "confirm":
                  boolean success = purchaseData.shardItem.purchaseItem(player, purchaseData.amount);
                  if (success) {
                     this.soundManager.playBuySound(player);
                  } else {
                     this.soundManager.playDeclineSound(player);
                  }
                  break;
               case "back":
                  this.soundManager.playClickSound(player);
                  this.pendingShardPurchases.remove(playerId);
                  this.shardsManager.openShardShop(player);
            }
         }
      }

   }

   private String getCategoryNameFromTitle(String title) {
      ConfigurationSection titlesSection = this.config.getConfigurationSection("gui-titles");
      if (titlesSection != null) {
         for(String key : titlesSection.getKeys(false)) {
            String configTitle = this.config.getString("gui-titles." + key);
            if (configTitle != null) {
               String cleanConfigTitle = configTitle.replace("%page%", "").trim();
               String cleanTitle = title.replaceAll("\\s+\\d+$", "").trim();
               if (cleanTitle.equals(HexColor.translateHexCodes(cleanConfigTitle))) {
                  return key;
               }
            }
         }
      }

      return "";
   }

   private boolean isCategoryTitle(String title) {
      ConfigurationSection titlesSection = this.config.getConfigurationSection("gui-titles");
      if (titlesSection != null) {
         for(String key : titlesSection.getKeys(false)) {
            String configTitle = this.config.getString("gui-titles." + key);
            if (configTitle != null) {
               String cleanConfigTitle = configTitle.replace("%page%", "").trim();
               String cleanTitle = title.replaceAll("\\s+\\d+$", "").trim();
               if (cleanTitle.equals(HexColor.translateHexCodes(cleanConfigTitle))) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void handleConfirmMenuAction(Player player, PurchaseData purchaseData, ItemStack clickedItem, int slot) {
      if (this.confirmMenu != null) {
         String action = this.confirmMenu.getButtonAction(slot);
         if (action != null) {
            Material material = purchaseData.shopItem.getMaterial();
            int maxStack = 64;
            if (material == Material.ENDER_PEARL) {
               maxStack = 16;
            } else if (material == Material.TOTEM_OF_UNDYING) {
               maxStack = 1;
            }

            switch (action) {
               case "confirm":
                  this.processPurchase(player, purchaseData.shopItem, purchaseData.amount);
                  break;
               case "back":
                  this.soundManager.playClickSound(player);
                  this.openCategory(player, purchaseData.categoryName);
                  this.pendingPurchases.remove(player.getUniqueId());
                  break;
               case "add1":
                  this.soundManager.playClickSound(player);
                  purchaseData.amount = Math.min(maxStack, purchaseData.amount + 1);
                  this.updateConfirmMenu(player, purchaseData);
                  break;
               case "add10":
                  this.soundManager.playClickSound(player);
                  purchaseData.amount = Math.min(maxStack, purchaseData.amount + 10);
                  this.updateConfirmMenu(player, purchaseData);
                  break;
               case "add64":
                  this.soundManager.playClickSound(player);
                  purchaseData.amount = maxStack;
                  this.updateConfirmMenu(player, purchaseData);
                  break;
               case "remove1":
                  this.soundManager.playClickSound(player);
                  purchaseData.amount = Math.max(1, purchaseData.amount - 1);
                  this.updateConfirmMenu(player, purchaseData);
                  break;
               case "remove10":
                  this.soundManager.playClickSound(player);
                  purchaseData.amount = Math.max(1, purchaseData.amount - 10);
                  this.updateConfirmMenu(player, purchaseData);
                  break;
               case "remove64":
                  this.soundManager.playClickSound(player);
                  purchaseData.amount = 1;
                  this.updateConfirmMenu(player, purchaseData);
            }

         }
      }
   }

   private void updateConfirmMenu(Player player, PurchaseData purchaseData) {
      ItemStack updatedItem = purchaseData.originalItem.clone();
      updatedItem.setAmount(purchaseData.amount);
      this.confirmMenu.openConfirmMenu(player, purchaseData.shopItem, updatedItem);
   }

   private void processPurchase(Player player, ShopItem shopItem, int amount) {
      try {
         double totalPrice = shopItem.getPrice() * (double)amount;
         if (!this.checkCooldown(player)) {
            return;
         }

         String itemName = shopItem.getDisplayName();
         if (itemName == null || itemName.isEmpty()) {
            itemName = this.getLocalizedItemName(shopItem.getMaterial());
         }

         boolean hasCommands = shopItem.getPurchaseCommands() != null && !shopItem.getPurchaseCommands().isEmpty();
         if (!hasCommands) {
            boolean hasSpace = this.hasEnoughInventorySpace(player, shopItem, amount);
            if (!hasSpace) {
               this.soundManager.playDeclineSound(player);
               String message = this.langManager.getMessage("inventory-full");
               if (message != null) {
                  player.sendMessage(HexColor.translateHexCodes(message));
               }

               return;
            }
         }

         if (hasCommands) {
            if (this.economy != null && this.economyEnabled) {
               double balance = this.economy.getBalance(player);
               if (balance < totalPrice) {
                  this.soundManager.playDeclineSound(player);
                  String formattedPrice = this.economyManager.formatMoneyWithCurrency(totalPrice, this.economy.currencyNamePlural());
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("price", formattedPrice);
                  placeholders.put("currency", this.economy.currencyNamePlural());
                  this.langManager.sendModuleMessage(player, "not-enough-money", placeholders);
                  return;
               }

               EconomyResponse response = this.economy.withdrawPlayer(player, totalPrice);
               if (!response.transactionSuccess()) {
                  this.soundManager.playDeclineSound(player);
                  HashMap<String, String> placeholders = new HashMap();
                  placeholders.put("error", response.errorMessage);
                  this.langManager.sendModuleMessage(player, "transaction-failed", placeholders);
                  return;
               }
            }

            for(String command : shopItem.getPurchaseCommands()) {
               String processedCommand = command.replace("%player%", player.getName()).replace("%amount%", String.valueOf(amount));
               if (this.isFolia) {
                  this.getServer().getGlobalRegionScheduler().execute(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand));
               } else {
                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
               }
            }

            this.soundManager.playBuySound(player);
            String formattedPrice = this.economy != null && this.economyEnabled ? this.economyManager.formatMoneyWithCurrency(totalPrice, this.economy.currencyNamePlural()) : this.economyManager.formatMoneyWithCurrency(totalPrice, "$");
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("item", amount + "x " + itemName);
            placeholders.put("price", formattedPrice);
            this.langManager.sendModuleMessage(player, "purchase-success", placeholders);
            if (this.playerDataManager != null) {
               String itemNameLog = shopItem.getMaterial().toString();
               this.playerDataManager.logPurchase(player, itemNameLog, amount, totalPrice);
            }

            return;
         }

         if (this.economy == null || !this.economyEnabled) {
            ItemStack itemToGive = this.createItemWithoutPriceLore(shopItem, amount);
            this.addItemToPlayer(player, itemToGive);
            this.soundManager.playBuySound(player);
            String formattedPrice = this.economyManager.formatMoneyWithCurrency(totalPrice, "$");
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("item", amount + "x " + itemName);
            placeholders.put("price", formattedPrice);
            placeholders.put("currency", "$");
            this.langManager.sendModuleMessage(player, "purchase-success", placeholders);
            if (this.playerDataManager != null) {
               String itemNameLog = shopItem.getMaterial().toString();
               this.playerDataManager.logPurchase(player, itemNameLog, amount, totalPrice);
            }

            return;
         }

         double balance = this.economy.getBalance(player);
         if (balance >= totalPrice) {
            EconomyResponse response = this.economy.withdrawPlayer(player, totalPrice);
            if (response.transactionSuccess()) {
               ItemStack itemToGive = this.createItemWithoutPriceLore(shopItem, amount);
               this.addItemToPlayer(player, itemToGive);
               this.soundManager.playBuySound(player);
               String formattedPrice = this.economyManager.formatMoneyWithCurrency(totalPrice, this.economy.currencyNamePlural());
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("item", amount + "x " + itemName);
               placeholders.put("price", formattedPrice);
               placeholders.put("currency", this.economy.currencyNamePlural());
               this.langManager.sendModuleMessage(player, "purchase-success", placeholders);
               if (this.playerDataManager != null) {
                  String itemNameLog = shopItem.getMaterial().toString();
                  this.playerDataManager.logPurchase(player, itemNameLog, amount, totalPrice);
               }
            } else {
               this.soundManager.playDeclineSound(player);
               HashMap<String, String> placeholders = new HashMap();
               placeholders.put("error", response.errorMessage);
               this.langManager.sendModuleMessage(player, "transaction-failed", placeholders);
            }
         } else {
            this.soundManager.playDeclineSound(player);
            String formattedPrice = this.economyManager.formatMoneyWithCurrency(totalPrice, this.economy.currencyNamePlural());
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("price", formattedPrice);
            placeholders.put("currency", this.economy.currencyNamePlural());
            this.langManager.sendModuleMessage(player, "not-enough-money", placeholders);
         }
      } catch (Exception e) {
         e.printStackTrace();
         this.soundManager.playDeclineSound(player);
      }

   }

   private boolean hasEnoughInventorySpace(Player player, ShopItem shopItem, int amount) {
      int remainingAmount = amount;
      Material material = shopItem.getMaterial();
      int maxStackSize = material.getMaxStackSize();

      for(int i = 0; i <= 35; ++i) {
         ItemStack item = player.getInventory().getItem(i);
         if (remainingAmount <= 0) {
            break;
         }

         if (item != null && item.getType() != Material.AIR) {
            if (item.getType() == material && this.itemsAreSimilarForStacking(item, shopItem)) {
               int spaceInStack = maxStackSize - item.getAmount();
               if (spaceInStack > 0) {
                  int canTake = Math.min(remainingAmount, spaceInStack);
                  remainingAmount -= canTake;
               }
            }
         } else {
            int canTake = Math.min(remainingAmount, maxStackSize);
            remainingAmount -= canTake;
         }
      }

      return remainingAmount <= 0;
   }

   private boolean itemsAreSimilarForStacking(ItemStack existing, ShopItem newItem) {
      if (existing.hasItemMeta() || newItem.getDisplayName() != null && !newItem.getDisplayName().isEmpty()) {
         ItemMeta existingMeta = existing.getItemMeta();
         if (existingMeta != null) {
            String existingName = existingMeta.hasDisplayName() ? existingMeta.getDisplayName() : "";
            String newName = newItem.getDisplayName() != null ? newItem.getDisplayName() : "";
            if (!existingName.equals(newName)) {
               return false;
            } else {
               List<String> existingLore = (List<String>)(existingMeta.hasLore() ? existingMeta.getLore() : new ArrayList());
               List<String> newLore = (List<String>)(newItem.getLore() != null ? newItem.getLore() : new ArrayList());
               return existingLore.equals(newLore);
            }
         } else {
            return newItem.getDisplayName() == null || newItem.getDisplayName().isEmpty();
         }
      } else {
         return true;
      }
   }

   private String getLocalizedItemName(Material material) {
      if (material == null) {
         return "";
      } else {
         try {
            ItemStack tempItem = new ItemStack(material);
            ItemMeta meta = tempItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
               return meta.getDisplayName();
            }
         } catch (Exception var4) {
         }

         return this.materialToReadableName(material.toString());
      }
   }

   private String materialToReadableName(String materialName) {
      if (materialName == null) {
         return "";
      } else {
         String[] words = materialName.toLowerCase().split("_");
         StringBuilder result = new StringBuilder();

         for(String word : words) {
            if (word.length() > 0) {
               result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
         }

         return result.toString().trim();
      }
   }

   public void sendActionBar(Player player, String message) {
      try {
         player.sendActionBar(HexColor.translateHexCodes(message));
      } catch (Exception var4) {
         player.sendMessage(HexColor.translateHexCodes(message));
      }

   }

   private ItemStack createItemWithoutPriceLore(ShopItem shopItem, int amount) {
      if (shopItem.shouldUseSerializedItem() && shopItem.getSerializedItemStack() != null && !shopItem.getSerializedItemStack().isEmpty()) {
         try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(shopItem.getSerializedItemStack()));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack)dataInput.readObject();
            dataInput.close();
            item.setAmount(amount);
            return item;
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      List<String> originalLore = shopItem.getOriginalLore();
      Map<Enchantment, Integer> enchantments = shopItem.getEnchantments();
      boolean hasEnchantments = enchantments != null && !enchantments.isEmpty();
      ItemStack item;
      if (shopItem.getPotionEffect() != null && !shopItem.getPotionEffect().isEmpty()) {
         Material potionMaterial = shopItem.getMaterial();
         if (potionMaterial != Material.POTION && potionMaterial != Material.SPLASH_POTION && potionMaterial != Material.LINGERING_POTION) {
            potionMaterial = Material.POTION;
         }

         item = new ItemStack(potionMaterial, amount);
         PotionMeta meta = (PotionMeta)item.getItemMeta();
         if (meta != null) {
            String effectStr = shopItem.getPotionEffect();
            String[] parts = effectStr.split(":");
            String effectName = parts[0];
            int amplifier = shopItem.getPotionAmplifier();
            if (parts.length > 1) {
               try {
                  amplifier = Integer.parseInt(parts[1]);
               } catch (NumberFormatException var20) {
               }
            }

            PotionEffectType effectType = PotionEffectType.getByName(effectName);
            if (effectType == null) {
               try {
                  effectType = PotionEffectType.getByKey(NamespacedKey.minecraft(effectName.toLowerCase()));
               } catch (Exception var19) {
               }
            }

            if (effectType != null) {
               PotionEffect effect = new PotionEffect(effectType, shopItem.getPotionDuration(), amplifier);
               meta.addCustomEffect(effect, true);
            }

            if (effectType != null) {
               String name = effectType.getName();
               if (name.equals("STRENGTH")) {
                  meta.setColor(Color.fromRGB(255, 128, 0));
               } else if (name.equals("SPEED")) {
                  meta.setColor(Color.fromRGB(0, 191, 255));
               } else if (name.equals("REGENERATION")) {
                  meta.setColor(Color.fromRGB(255, 105, 180));
               } else if (name.equals("INVISIBILITY")) {
                  meta.setColor(Color.fromRGB(255, 255, 255));
               } else if (!name.equals("LEAPING") && !name.equals("JUMP_BOOST")) {
                  if (name.equals("FIRE_RESISTANCE")) {
                     meta.setColor(Color.fromRGB(255, 128, 0));
                  } else if (name.equals("WATER_BREATHING")) {
                     meta.setColor(Color.fromRGB(0, 128, 255));
                  } else if (name.equals("NIGHT_VISION")) {
                     meta.setColor(Color.fromRGB(144, 238, 144));
                  } else if (name.equals("POISON")) {
                     meta.setColor(Color.fromRGB(50, 200, 50));
                  } else {
                     meta.setColor(Color.fromRGB(50, 200, 50));
                  }
               } else {
                  meta.setColor(Color.fromRGB(255, 255, 153));
               }
            }

            meta.setDisplayName(shopItem.getDisplayName());
            ArrayList<String> finalLore = new ArrayList();
            if (!hasEnchantments) {
               if (originalLore != null && !originalLore.isEmpty()) {
                  finalLore.addAll(originalLore);
               }
            } else {
               for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                  String enchantName = this.getEnchantmentDisplayName((Enchantment)entry.getKey());
                  String romanLevel = this.toRoman((Integer)entry.getValue());
                  finalLore.add("§7" + enchantName + " " + romanLevel);
               }

               if (originalLore != null && !originalLore.isEmpty()) {
                  if (!finalLore.isEmpty() && !((String)finalLore.get(finalLore.size() - 1)).isEmpty()) {
                     finalLore.add("");
                  }

                  finalLore.addAll(originalLore);
               }
            }

            meta.setLore(finalLore);
            if (hasEnchantments) {
               for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                  meta.addEnchant((Enchantment)entry.getKey(), (Integer)entry.getValue(), true);
               }

               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }

            item.setItemMeta(meta);
         }
      } else {
         item = new ItemStack(shopItem.getMaterial(), amount);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(shopItem.getDisplayName());
            ArrayList<String> finalLore = new ArrayList();
            if (!hasEnchantments) {
               if (originalLore != null && !originalLore.isEmpty()) {
                  finalLore.addAll(originalLore);
               }
            } else {
               for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                  String enchantName = this.getEnchantmentDisplayName((Enchantment)entry.getKey());
                  String romanLevel = this.toRoman((Integer)entry.getValue());
                  finalLore.add("§7" + enchantName + " " + romanLevel);
               }

               if (originalLore != null && !originalLore.isEmpty()) {
                  if (!finalLore.isEmpty() && !((String)finalLore.get(finalLore.size() - 1)).isEmpty()) {
                     finalLore.add("");
                  }

                  finalLore.addAll(originalLore);
               }
            }

            meta.setLore(finalLore);
            if (hasEnchantments) {
               for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                  meta.addEnchant((Enchantment)entry.getKey(), (Integer)entry.getValue(), true);
               }

               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }

            item.setItemMeta(meta);
         }
      }

      return item;
   }

   private String getEnchantmentDisplayName(Enchantment enchant) {
      if (enchant == null) {
         return "Invaild";
      } else {
         switch (enchant.getKey().getKey()) {
            case "unbreaking" -> {
               return "Unbreaking";
            }
            case "protection" -> {
               return "Protection";
            }
            case "fire_protection" -> {
               return "Fire protection";
            }
            case "feather_falling" -> {
               return "Falldamage";
            }
            case "blast_protection" -> {
               return "Blast protection";
            }
            case "projectile_protection" -> {
               return "Projectile protection";
            }
            case "sharpness" -> {
               return "Sharpness";
            }
            case "bane_of_arthropods" -> {
               return "Bane of arthropods";
            }
            case "smite" -> {
               return "Smite";
            }
            case "efficiency" -> {
               return "Efficiency";
            }
            case "fortune" -> {
               return "Fortune";
            }
            case "looting" -> {
               return "Looting";
            }
            case "power" -> {
               return "Power";
            }
            case "flame" -> {
               return "Flame";
            }
            case "infinity" -> {
               return "Infinity";
            }
            case "punch" -> {
               return "Punch";
            }
            case "aqua_affinity" -> {
               return "Aqua Affinity";
            }
            case "respiration" -> {
               return "Respiration";
            }
            case "thorns" -> {
               return "Thorns";
            }
            case "mending" -> {
               return "Mending";
            }
            case "silk_touch" -> {
               return "Silk Touch";
            }
            case "depth_strider" -> {
               return "Depth Strider";
            }
            case "frost_walker" -> {
               return "Frost Walker";
            }
            case "sweeping" -> {
               return "Sweeping Edge";
            }
            case "quick_charge" -> {
               return "Quick Charge";
            }
            case "multishot" -> {
               return "Multishot";
            }
            case "piercing" -> {
               return "Piercing";
            }
            case "loyalty" -> {
               return "Loyalty";
            }
            case "impaling" -> {
               return "Impaling";
            }
            case "riptide" -> {
               return "Riptide";
            }
            case "channeling" -> {
               return "Channeling";
            }
            case "swift_sneak" -> {
               return "Swift Sneak";
            }
            case "soul_speed" -> {
               return "Soul Speed";
            }
            default -> {
               return enchant.getKey().getKey();
            }
         }
      }
   }

   private String toRoman(int number) {
      switch (number) {
         case 1 -> {
            return "I";
         }
         case 2 -> {
            return "II";
         }
         case 3 -> {
            return "III";
         }
         case 4 -> {
            return "IV";
         }
         case 5 -> {
            return "V";
         }
         case 6 -> {
            return "VI";
         }
         case 7 -> {
            return "VII";
         }
         case 8 -> {
            return "VIII";
         }
         case 9 -> {
            return "IX";
         }
         case 10 -> {
            return "X";
         }
         default -> {
            return String.valueOf(number);
         }
      }
   }

   private void addItemToPlayer(Player player, ItemStack item) {
      HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack[]{item});
      if (!leftover.isEmpty()) {
         player.sendMessage(this.langManager.getMessage("inventory-full"));
      }

   }

   public List<String> getCategoryNames() {
      return new ArrayList(this.categories.keySet());
   }

   public boolean categoryExists(String categoryKey) {
      return this.categories.containsKey(categoryKey);
   }

   public Economy getEconomy() {
      return this.economy;
   }

   public boolean isEconomyEnabled() {
      return this.economyEnabled;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public ConfirmMenu getConfirmMenu() {
      return this.confirmMenu;
   }

   public ShardConfirmMenu getShardConfirmMenu() {
      return this.shardConfirmMenu;
   }

   public EconomyManager getEconomyManager() {
      return this.economyManager;
   }

   public ShardsManager getShardsManager() {
      return this.shardsManager;
   }

   public ItemStack getBackButton() {
      return this.backButton;
   }

   public PlayerDataManager getPlayerDataManager() {
      return this.playerDataManager;
   }

   private class PurchaseData {
      public ShopItem shopItem;
      public ItemStack originalItem;
      public int amount;
      public String categoryName;

      public PurchaseData(Shop shop, ShopItem shopItem, ItemStack originalItem, int amount, String categoryName) {
         this.shopItem = shopItem;
         this.originalItem = originalItem;
         this.amount = amount;
         this.categoryName = categoryName;
      }
   }

   private class ShardPurchaseData {
      public ShardsManager.ShardShopItem shardItem;
      public ItemStack originalItem;
      public int amount;

      public ShardPurchaseData(Shop shop, ShardsManager.ShardShopItem shardItem, ItemStack originalItem, int amount) {
         this.shardItem = shardItem;
         this.originalItem = originalItem;
         this.amount = amount;
      }
   }
}
