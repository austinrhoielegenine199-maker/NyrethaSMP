package de.elivb.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class Tools extends JavaPlugin implements TabCompleter {
   private File toolsFolder;
   private EnderPearl enderPearl;
   private Jump jump;
   private ChunkDrill chunkDrill;
   private Totem totem;
   private File langFile;
   private FileConfiguration langConfig;
   private File guiFile;
   private FileConfiguration guiConfig;
   private Drill drill;
   private TreeChopper treeChopper;
   private Shovel shovel;
   private Hoe hoe;
   private Rocket rocket;
   private WaterBucket waterBucket;
   private LavaBucket lavaBucket;
   private ToolsGUI toolsGUI;
   private ToolExpiryChecker expiryChecker;
   private LicenseManager licenseManager;
   private PlotService plotService;
   private boolean isFolia = false;
   private List<Material> blacklistedBlocks = null;
   private static final String PERMISSION_ADMIN = "tools.admin";

   public void onEnable() {
      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         this.isFolia = true;
      } catch (ClassNotFoundException var2) {
         this.isFolia = false;
      }

      this.licenseManager = new LicenseManager(this);
      if (this.licenseManager.validateLicenseOnStartup()) {
         this.plotService = new PlotService(this);
         this.toolsFolder = new File(this.getDataFolder(), "Tools");
         if (!this.toolsFolder.exists()) {
            this.toolsFolder.mkdirs();
         }

         this.loadLangFile();
         this.loadGUIConfig();
         this.createDefaultToolIfNotExists("drill");
         this.createDefaultToolIfNotExists("treechopper");
         this.createDefaultToolIfNotExists("shovel");
         this.createDefaultToolIfNotExists("hoe");
         this.createDefaultToolIfNotExists("enderpearl");
         this.createDefaultToolIfNotExists("rocket");
         this.createDefaultToolIfNotExists("waterbucket");
         this.createDefaultToolIfNotExists("lavabucket");
         this.createDefaultToolIfNotExists("jump");
         this.createDefaultToolIfNotExists("chunkdrill");
         this.createDefaultToolIfNotExists("totem");
         this.reloadToolInstances();
         this.toolsGUI = new ToolsGUI(this);
         this.expiryChecker = new ToolExpiryChecker(this);
         this.expiryChecker.start();
         this.getServer().getPluginManager().registerEvents(new DrillListener(this), this);
         this.getServer().getPluginManager().registerEvents(new TreeChopperListener(this), this);
         this.getServer().getPluginManager().registerEvents(new ShovelListener(this), this);
         this.getServer().getPluginManager().registerEvents(new HoeListener(this), this);
         this.getServer().getPluginManager().registerEvents(new EnderPearlListener(this), this);
         this.getServer().getPluginManager().registerEvents(new RocketListener(this), this);
         this.getServer().getPluginManager().registerEvents(new WaterBucketListener(this), this);
         this.getServer().getPluginManager().registerEvents(new LavaBucketListener(this), this);
         this.getServer().getPluginManager().registerEvents(new JumpListener(this), this);
         this.getServer().getPluginManager().registerEvents(new ChunkDrillListener(this), this);
         TotemListener totemListener = new TotemListener(this);
         this.getServer().getPluginManager().registerEvents(totemListener, this);
         this.getServer().getPluginManager().registerEvents(this.toolsGUI, this);
         if (this.getCommand("tool") != null) {
            this.getCommand("tool").setTabCompleter(this);
         } else {
            this.getLogger().warning(this.getLang("error-command-not-found"));
         }

      }
   }

   public void onDisable() {
      if (this.expiryChecker != null) {
         this.expiryChecker.stop();
      }

   }

   public LicenseManager getLicenseManager() {
      return this.licenseManager;
   }

   public PlotService getPlotService() {
      if (this.plotService == null) {
         this.plotService = new PlotService(this);
      }

      return this.plotService;
   }

   public List<Material> getBlacklistedBlocks() {
      if (this.blacklistedBlocks == null) {
         this.blacklistedBlocks = new ArrayList();
         List<String> blockNames = this.langConfig.getStringList("blacklist-blocks");
         if (blockNames != null && !blockNames.isEmpty()) {
            for(String blockName : blockNames) {
               Material material = Material.matchMaterial(blockName);
               if (material != null) {
                  this.blacklistedBlocks.add(material);
               }
            }
         }

         if (this.blacklistedBlocks.isEmpty()) {
            this.blacklistedBlocks.add(Material.BEDROCK);
            this.blacklistedBlocks.add(Material.BARRIER);
            this.blacklistedBlocks.add(Material.SPAWNER);
            this.blacklistedBlocks.add(Material.COMMAND_BLOCK);
            this.blacklistedBlocks.add(Material.CHAIN_COMMAND_BLOCK);
            this.blacklistedBlocks.add(Material.REPEATING_COMMAND_BLOCK);
            this.blacklistedBlocks.add(Material.END_PORTAL_FRAME);
            this.blacklistedBlocks.add(Material.END_PORTAL);
            this.blacklistedBlocks.add(Material.NETHER_PORTAL);
            this.blacklistedBlocks.add(Material.STRUCTURE_BLOCK);
            this.blacklistedBlocks.add(Material.JIGSAW);
         }
      }

      return this.blacklistedBlocks;
   }

   public boolean isBlockBlacklisted(Material material) {
      return this.getBlacklistedBlocks().contains(material);
   }

   public void runGlobal(Runnable task) {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().run(this, (scheduledTask) -> task.run());
      } else {
         this.getServer().getScheduler().runTask(this, task);
      }

   }

   public void runGlobalLater(Runnable task, long delayTicks) {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delayTicks);
      } else {
         this.getServer().getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   public void runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), delayTicks, periodTicks);
      } else {
         this.getServer().getScheduler().runTaskTimer(this, task, delayTicks, periodTicks);
      }

   }

   public void runAsync(Runnable task) {
      if (this.isFolia) {
         this.getServer().getGlobalRegionScheduler().run(this, (scheduledTask) -> task.run());
      } else {
         this.getServer().getScheduler().runTaskAsynchronously(this, task);
      }

   }

   public void runEntity(Entity entity, Runnable task) {
      if (this.isFolia) {
         entity.getScheduler().run(this, (scheduledTask) -> task.run(), (Runnable)null);
      } else {
         this.getServer().getScheduler().runTask(this, task);
      }

   }

   public void runEntityLater(Entity entity, Runnable task, long delayTicks) {
      if (this.isFolia) {
         entity.getScheduler().runDelayed(this, (scheduledTask) -> task.run(), (Runnable)null, delayTicks);
      } else {
         this.getServer().getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   public void runEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
      if (this.isFolia) {
         entity.getScheduler().runAtFixedRate(this, (scheduledTask) -> task.run(), (Runnable)null, delayTicks, periodTicks);
      } else {
         this.getServer().getScheduler().runTaskTimer(this, task, delayTicks, periodTicks);
      }

   }

   public void runAtLocation(Location location, Runnable task) {
      if (this.isFolia) {
         this.getServer().getRegionScheduler().run(this, location, (scheduledTask) -> task.run());
      } else {
         this.getServer().getScheduler().runTask(this, task);
      }

   }

   public void runAtLocationLater(Location location, Runnable task, long delayTicks) {
      if (this.isFolia) {
         this.getServer().getRegionScheduler().runDelayed(this, location, (scheduledTask) -> task.run(), delayTicks);
      } else {
         this.getServer().getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   public void runAtLocationTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
      if (this.isFolia) {
         this.getServer().getRegionScheduler().runAtFixedRate(this, location, (scheduledTask) -> task.run(), delayTicks, periodTicks);
      } else {
         this.getServer().getScheduler().runTaskTimer(this, task, delayTicks, periodTicks);
      }

   }

   public void cancelTask(Object taskId) {
      if (!this.isFolia && taskId instanceof Integer) {
         this.getServer().getScheduler().cancelTask((Integer)taskId);
      }

   }

   public boolean isFolia() {
      return this.isFolia;
   }

   private void loadLangFile() {
      this.langFile = new File(this.getDataFolder(), "lang.yml");
      if (!this.langFile.exists()) {
         this.saveResource("lang.yml", false);
      }

      this.langConfig = YamlConfiguration.loadConfiguration(this.langFile);
      this.blacklistedBlocks = null;
   }

   private void loadGUIConfig() {
      this.guiFile = new File(this.getDataFolder(), "gui.yml");
      if (!this.guiFile.exists()) {
         this.saveResource("gui.yml", false);
      }

      this.guiConfig = YamlConfiguration.loadConfiguration(this.guiFile);
   }

   public void saveGUIConfig() {
      try {
         this.guiConfig.save(this.guiFile);
      } catch (IOException var2) {
      }

   }

   public String getLang(String key) {
      String message = this.langConfig.getString(key, "Message not found: " + key);
      if (message.startsWith("@")) {
         String referenceKey = message.substring(1);
         message = this.langConfig.getString(referenceKey, message);
      }

      return HexColorCode.translateAllColorCodes(message);
   }

   public String getLang(String key, String... replacements) {
      String message = this.getLang(key);

      for(int i = 0; i < replacements.length; i += 2) {
         if (i + 1 < replacements.length) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
         }
      }

      return message;
   }

   private void createDefaultToolIfNotExists(String toolName) {
      File file = new File(this.toolsFolder, toolName + ".yml");
      if (!file.exists()) {
         this.saveDefaultTool(toolName);
      }

   }

   private void saveDefaultTool(String toolName) {
      File file = new File(this.toolsFolder, toolName + ".yml");

      try {
         YamlConfiguration config = new YamlConfiguration();
         if (toolName.equals("drill")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴘɪᴄᴋᴀxᴇ");
            List<String> drillLore = new ArrayList();
            drillLore.add("&7Breaks 9 Blocks at Once");
            drillLore.add("&8Self Destruct:");
            drillLore.add("&8%durability%");
            config.set("lore", drillLore);
            config.set("custom-model-data", 1000);
            config.set("material", "NETHERITE_PICKAXE");
            config.set("durability.mode", "time");
            config.set("durability.uses", 100);
            config.set("durability.time", "3d");
            config.set("enchantments.enabled", true);
            List<String> drillEnchants = new ArrayList();
            drillEnchants.add("EFFICIENCY:5");
            drillEnchants.add("UNBREAKING:3");
            drillEnchants.add("FORTUNE:3");
            drillEnchants.add("MENDING:1");
            config.set("enchantments.list", drillEnchants);
            config.set("sound.enabled", true);
            config.set("sound.type", "BLOCK_AMETHYST_BLOCK_STEP");
            config.set("sound.volume", 0.1);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", true);
            config.set("particle.type", "PORTAL");
            config.set("particle.count", 20);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "DRAGON_BREATH");
            config.set("particle.additional-particle.count", 15);
         } else if (toolName.equals("treechopper")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ᴀxᴇ");
            List<String> treeLore = new ArrayList();
            treeLore.add("&7Breaks Trees instantly");
            treeLore.add("&8Self Destruct:");
            treeLore.add("&8%durability%");
            config.set("lore", treeLore);
            config.set("custom-model-data", 1000);
            config.set("material", "NETHERITE_AXE");
            config.set("durability.mode", "time");
            config.set("durability.uses", 100);
            config.set("durability.time", "3d");
            config.set("enchantments.enabled", true);
            List<String> axeEnchants = new ArrayList();
            axeEnchants.add("EFFICIENCY:5");
            axeEnchants.add("UNBREAKING:3");
            axeEnchants.add("SHARPNESS:3");
            axeEnchants.add("MENDING:1");
            axeEnchants.add("SWEEPING_EDGE:3");
            config.set("enchantments.list", axeEnchants);
            config.set("sound.enabled", true);
            config.set("sound.type", "ITEM_AXE_STRIP");
            config.set("sound.volume", (double)0.5F);
            config.set("sound.pitch", 1.2);
            config.set("particle.enabled", true);
            config.set("particle.type", "CRIT");
            config.set("particle.count", 15);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "CLOUD");
            config.set("particle.additional-particle.count", 10);
         } else if (toolName.equals("shovel")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ꜱʜᴏᴠᴇʟ");
            List<String> shovelLore = new ArrayList();
            shovelLore.add("&7Breaks 9 Blocks at Once");
            shovelLore.add("&8Self Destruct:");
            shovelLore.add("&8%durability%");
            config.set("lore", shovelLore);
            config.set("custom-model-data", 1000);
            config.set("material", "NETHERITE_SHOVEL");
            config.set("durability.mode", "time");
            config.set("durability.uses", 100);
            config.set("durability.time", "3d");
            config.set("enchantments.enabled", true);
            List<String> shovelEnchants = new ArrayList();
            shovelEnchants.add("EFFICIENCY:5");
            shovelEnchants.add("UNBREAKING:3");
            shovelEnchants.add("FORTUNE:3");
            shovelEnchants.add("MENDING:1");
            config.set("enchantments.list", shovelEnchants);
            config.set("sound.enabled", true);
            config.set("sound.type", "BLOCK_GRASS_BREAK");
            config.set("sound.volume", 0.3);
            config.set("sound.pitch", 1.1);
            config.set("particle.enabled", true);
            config.set("particle.type", "CLOUD");
            config.set("particle.count", 25);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "CRIT");
            config.set("particle.additional-particle.count", 15);
         } else if (toolName.equals("hoe")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ʜᴏᴇ");
            List<String> hoeLore = new ArrayList();
            hoeLore.add("&7Plant 9 Blocks at once");
            hoeLore.add("&8Self Destruct:");
            hoeLore.add("&8%durability%");
            config.set("lore", hoeLore);
            config.set("custom-model-data", 1000);
            config.set("material", "NETHERITE_HOE");
            config.set("durability.mode", "time");
            config.set("durability.uses", 100);
            config.set("durability.time", "3d");
            config.set("enchantments.enabled", true);
            List<String> hoeEnchants = new ArrayList();
            hoeEnchants.add("EFFICIENCY:5");
            hoeEnchants.add("UNBREAKING:3");
            hoeEnchants.add("FORTUNE:3");
            hoeEnchants.add("MENDING:1");
            config.set("enchantments.list", hoeEnchants);
            config.set("sound.enabled", true);
            config.set("sound.type", "BLOCK_CROP_BREAK");
            config.set("sound.volume", 0.4);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", true);
            config.set("particle.type", "VILLAGER_HAPPY");
            config.set("particle.count", 15);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "CLOUD");
            config.set("particle.additional-particle.count", 10);
         } else if (toolName.equals("enderpearl")) {
            config.set("name", "&#A20AD6ɪɴꜰɪɴɪᴛᴇ ᴇɴᴅᴇʀ ᴘᴇᴀʀʟ");
            List<String> pearlLore = new ArrayList();
            pearlLore.add("&7Ender Pearl that never breaks");
            pearlLore.add("&8Self Destruct:");
            pearlLore.add("&8%durability%");
            config.set("lore", pearlLore);
            config.set("custom-model-data", 1000);
            config.set("material", "ENDER_PEARL");
            config.set("durability.mode", "infinite");
            config.set("enchantments.enabled", false);
            config.set("enchantments.list", new ArrayList());
            config.set("sound.enabled", true);
            config.set("sound.type", "ENTITY_ENDERMAN_TELEPORT");
            config.set("sound.volume", (double)1.0F);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", true);
            config.set("particle.type", "PORTAL");
            config.set("particle.count", 50);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "DRAGON_BREATH");
            config.set("particle.additional-particle.count", 30);
         } else if (toolName.equals("rocket")) {
            config.set("name", "&#A20AD6ɪɴꜰɪɴɪᴛʏ ʀᴏᴄᴋᴇᴛ");
            List<String> rocketLore = new ArrayList();
            rocketLore.add("&7Boosts your Elytra flight!");
            rocketLore.add("&8Durability: &f%durability%");
            config.set("lore", rocketLore);
            config.set("custom-model-data", 1001);
            config.set("material", "FIREWORK_ROCKET");
            config.set("durability.mode", "infinite");
            config.set("durability.uses", 100);
            config.set("durability.time", "3d");
            config.set("enchantments.enabled", true);
            List<String> rocketEnchants = new ArrayList();
            rocketEnchants.add("UNBREAKING:3");
            rocketEnchants.add("MENDING:1");
            config.set("enchantments.list", rocketEnchants);
            config.set("rocket.flight-duration", 1);
            config.set("rocket.boost-power", (double)1.5F);
            config.set("sound.enabled", true);
            config.set("sound.type", "ENTITY_FIREWORK_ROCKET_LAUNCH");
            config.set("sound.volume", (double)1.0F);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", true);
            config.set("particle.type", "FIREWORK");
            config.set("particle.count", 30);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "FLAME");
            config.set("particle.additional-particle.count", 20);
         } else if (toolName.equals("waterbucket")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴡᴀᴛᴇʀ ʙᴜᴄᴋᴇᴛ");
            List<String> waterLore = new ArrayList();
            waterLore.add("&7Infinite Water Bucket");
            waterLore.add("&8Durability: &f%durability%");
            config.set("lore", waterLore);
            config.set("custom-model-data", 1002);
            config.set("material", "WATER_BUCKET");
            config.set("durability.mode", "infinite");
            config.set("durability.uses", 100);
            config.set("durability.time", "3d");
            config.set("enchantments.enabled", false);
            List<String> waterEnchants = new ArrayList();
            waterEnchants.add("UNBREAKING:3");
            waterEnchants.add("MENDING:1");
            config.set("enchantments.list", waterEnchants);
            config.set("sound.enabled", false);
            config.set("sound.type", "ENTITY_GENERIC_SPLASH");
            config.set("sound.volume", (double)1.0F);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", false);
            config.set("particle.type", "SPLASH");
            config.set("particle.count", 10);
            config.set("particle.additional-particle.enabled", false);
            config.set("particle.additional-particle.type", "CLOUD");
            config.set("particle.additional-particle.count", 5);
         } else if (toolName.equals("lavabucket")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ʟᴀᴠᴀ ʙᴜᴄᴋᴇᴛ");
            List<String> lavaLore = new ArrayList();
            lavaLore.add("&7Infinite Lava Bucket");
            lavaLore.add("&8Durability: &f%durability%");
            config.set("lore", lavaLore);
            config.set("custom-model-data", 1003);
            config.set("material", "LAVA_BUCKET");
            config.set("durability.mode", "infinite");
            config.set("durability.uses", 100);
            config.set("durability.time", "3d");
            config.set("enchantments.enabled", false);
            List<String> lavaEnchants = new ArrayList();
            lavaEnchants.add("UNBREAKING:3");
            lavaEnchants.add("MENDING:1");
            config.set("enchantments.list", lavaEnchants);
            config.set("sound.enabled", false);
            config.set("sound.type", "ENTITY_GENERIC_EXTINGUISH_FIRE");
            config.set("sound.volume", (double)1.0F);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", false);
            config.set("particle.type", "FLAME");
            config.set("particle.count", 10);
            config.set("particle.additional-particle.enabled", false);
            config.set("particle.additional-particle.type", "LAVA");
            config.set("particle.additional-particle.count", 5);
         } else if (toolName.equals("jump")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴊᴜᴍᴘ");
            List<String> jumpLore = new ArrayList();
            jumpLore.add("&7Boost you in the destination");
            jumpLore.add("&7that you look &#A20AD6%distance% &7blocks");
            jumpLore.add("&8Cooldown: &f%cooldown%s");
            jumpLore.add("&8Durability: &f%durability%");
            config.set("lore", jumpLore);
            config.set("custom-model-data", 1000);
            config.set("material", "WIND_CHARGE");
            config.set("durability.mode", "time");
            config.set("durability.uses", 100);
            config.set("durability.time", "1d");
            config.set("enchantments.enabled", true);
            List<String> jumpEnchants = new ArrayList();
            jumpEnchants.add("UNBREAKING:3");
            jumpEnchants.add("MENDING:1");
            config.set("enchantments.list", jumpEnchants);
            config.set("jump.distance", 4);
            config.set("jump.cooldown", 15);
            config.set("sound.enabled", true);
            config.set("sound.type", "ENTITY_WIND_CHARGE_WIND_BURST");
            config.set("sound.volume", (double)1.0F);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", true);
            config.set("particle.type", "CLOUD");
            config.set("particle.count", 30);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "CRIT");
            config.set("particle.additional-particle.count", 15);
         } else if (toolName.equals("chunkdrill")) {
            config.set("name", "&#A20AD6ᴄʜᴜɴᴋ ᴅʀɪʟʟ");
            List<String> chunkLore = new ArrayList();
            chunkLore.add("&7Breaks entire Chunk at once!");
            chunkLore.add("&8Self Destruct:");
            chunkLore.add("&8%durability%");
            config.set("lore", chunkLore);
            config.set("custom-model-data", 1004);
            config.set("material", "NETHERITE_PICKAXE");
            config.set("durability.mode", "uses");
            config.set("durability.uses", 1);
            config.set("durability.time", "1d");
            config.set("enchantments.enabled", true);
            List<String> chunkEnchants = new ArrayList();
            chunkEnchants.add("EFFICIENCY:10");
            chunkEnchants.add("UNBREAKING:3");
            chunkEnchants.add("MENDING:1");
            config.set("enchantments.list", chunkEnchants);
            config.set("sound.enabled", true);
            config.set("sound.type", "ENTITY_ENDER_DRAGON_GROWL");
            config.set("sound.volume", (double)1.0F);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", true);
            config.set("particle.type", "EXPLOSION");
            config.set("particle.count", 50);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "PORTAL");
            config.set("particle.additional-particle.count", 30);
         } else if (toolName.equals("totem")) {
            config.set("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴛᴏᴛᴇᴍ");
            List<String> totemLore = new ArrayList();
            totemLore.add("&7Revives you when you die!");
            totemLore.add("&8Durability: &f%durability%");
            config.set("lore", totemLore);
            config.set("custom-model-data", 1005);
            config.set("material", "TOTEM_OF_UNDYING");
            config.set("durability.mode", "uses");
            config.set("durability.uses", 1);
            config.set("durability.time", "1d");
            config.set("totem.revive-full-health", true);
            config.set("totem.remove-effects", true);
            config.set("totem.keep-inventory", false);
            config.set("enchantments.enabled", true);
            List<String> totemEnchants = new ArrayList();
            totemEnchants.add("UNBREAKING:3");
            totemEnchants.add("MENDING:1");
            config.set("enchantments.list", totemEnchants);
            config.set("sound.enabled", true);
            config.set("sound.type", "ENTITY_TOTEM_OF_UNDYING_USE");
            config.set("sound.volume", (double)1.0F);
            config.set("sound.pitch", (double)1.0F);
            config.set("particle.enabled", true);
            config.set("particle.type", "TOTEM_OF_UNDYING");
            config.set("particle.count", 50);
            config.set("particle.additional-particle.enabled", true);
            config.set("particle.additional-particle.type", "HEART");
            config.set("particle.additional-particle.count", 30);
         }

         config.save(file);
         this.getLogger().info(this.getLang("config-created", "tool", toolName));
      } catch (IOException e) {
         this.getLogger().warning(this.getLang("error-config-save", "error", e.getMessage()));
      }

   }

   private void reloadToolInstances() {
      this.drill = new Drill(this);
      this.treeChopper = new TreeChopper(this);
      this.shovel = new Shovel(this);
      this.hoe = new Hoe(this);
      this.enderPearl = new EnderPearl(this);
      this.rocket = new Rocket(this);
      this.waterBucket = new WaterBucket(this);
      this.lavaBucket = new LavaBucket(this);
      this.jump = new Jump(this);
      this.chunkDrill = new ChunkDrill(this);
      this.totem = new Totem(this);
   }

   private long parseDuration(String duration) {
      if (duration != null && !duration.isEmpty()) {
         duration = duration.toLowerCase().trim();

         try {
            if (duration.endsWith("d")) {
               long days = Long.parseLong(duration.substring(0, duration.length() - 1));
               return days * 24L * 60L * 60L * 1000L;
            } else if (duration.endsWith("h")) {
               long hours = Long.parseLong(duration.substring(0, duration.length() - 1));
               return hours * 60L * 60L * 1000L;
            } else if (duration.endsWith("m")) {
               long minutes = Long.parseLong(duration.substring(0, duration.length() - 1));
               return minutes * 60L * 1000L;
            } else if (duration.endsWith("w")) {
               long weeks = Long.parseLong(duration.substring(0, duration.length() - 1));
               return weeks * 7L * 24L * 60L * 60L * 1000L;
            } else {
               return Long.parseLong(duration) * 60L * 1000L;
            }
         } catch (NumberFormatException var4) {
            return 259200000L;
         }
      } else {
         return 259200000L;
      }
   }

   private String formatTime(long millis) {
      if (millis <= 0L) {
         return "0m";
      } else {
         long days = millis / 86400000L;
         long hours = millis % 86400000L / 3600000L;
         long minutes = millis % 3600000L / 60000L;
         return days > 0L ? days + "d " + hours + "h " + minutes + "m" : hours + "h " + minutes + "m";
      }
   }

   public boolean reduceUses(Player player, ItemStack item, String toolType) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         PersistentDataContainer container = meta.getPersistentDataContainer();
         NamespacedKey usesKey = this.getUsesKeyForTool(toolType);
         if (usesKey == null) {
            return false;
         } else {
            Integer currentUses = (Integer)container.getOrDefault(usesKey, PersistentDataType.INTEGER, this.getMaxUsesForTool(toolType));
            if (currentUses == -1) {
               return true;
            } else if (currentUses <= 0) {
               return false;
            } else {
               int newUses = currentUses - 1;
               container.set(usesKey, PersistentDataType.INTEGER, newUses);
               this.updateItemLore(meta, toolType, newUses);
               item.setItemMeta(meta);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public int getUses(ItemStack item, String toolType) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         PersistentDataContainer container = meta.getPersistentDataContainer();
         NamespacedKey usesKey = this.getUsesKeyForTool(toolType);
         return usesKey == null ? this.getMaxUsesForTool(toolType) : (Integer)container.getOrDefault(usesKey, PersistentDataType.INTEGER, this.getMaxUsesForTool(toolType));
      } else {
         return this.getMaxUsesForTool(toolType);
      }
   }

   public int getMaxUsesForTool(String toolType) {
      File toolFile = new File(this.toolsFolder, toolType.toLowerCase() + ".yml");
      if (!toolFile.exists()) {
         return 100;
      } else {
         YamlConfiguration config = YamlConfiguration.loadConfiguration(toolFile);
         return config.contains("durability.uses") ? config.getInt("durability.uses", 100) : config.getInt("uses", 100);
      }
   }

   private NamespacedKey getUsesKeyForTool(String toolType) {
      switch (toolType.toLowerCase()) {
         case "drill" -> {
            return this.drill.getUsesKey();
         }
         case "treechopper" -> {
            return this.treeChopper.getUsesKey();
         }
         case "shovel" -> {
            return this.shovel.getUsesKey();
         }
         case "hoe" -> {
            return this.hoe.getUsesKey();
         }
         case "enderpearl" -> {
            return this.enderPearl.getUsesKey();
         }
         case "rocket" -> {
            return this.rocket.getUsesKey();
         }
         case "waterbucket" -> {
            return this.waterBucket.getUsesKey();
         }
         case "lavabucket" -> {
            return this.lavaBucket.getUsesKey();
         }
         case "jump" -> {
            return this.jump.getUsesKey();
         }
         case "chunkdrill" -> {
            return this.chunkDrill.getUsesKey();
         }
         case "totem" -> {
            return this.totem.getUsesKey();
         }
         default -> {
            return null;
         }
      }
   }

   private void updateItemLore(ItemMeta meta, String toolType, int currentUses) {
      File toolFile = new File(this.toolsFolder, toolType.toLowerCase() + ".yml");
      if (toolFile.exists()) {
         YamlConfiguration config = YamlConfiguration.loadConfiguration(toolFile);
         List<String> loreList = config.getStringList("lore");
         int maxUses = 100;
         if (config.contains("durability.uses")) {
            maxUses = config.getInt("durability.uses", 100);
         } else {
            maxUses = config.getInt("uses", 100);
         }

         if (loreList.isEmpty()) {
            loreList = new ArrayList();
            loreList.add("");
            loreList.add("&#A20AD6⏺ &fFrom the &#A20AD6Epic Crate");
            loreList.add("&#A20AD6⏺ &f%uses%");
            loreList.add("");
            loreList.add("&#A20AD6&l➟ &#A20AD6/ꜱʜᴏᴘ");
         }

         ArrayList<String> translatedLore = new ArrayList();

         for(String line : loreList) {
            String usesText = currentUses == -1 ? "&fuses: ∞" : "&fuses: " + currentUses + "/" + maxUses;
            String translatedLine = HexColorCode.translateAllColorCodes(line.replace("%uses%", usesText));
            translatedLore.add(translatedLine);
         }

         meta.setLore(translatedLore);
      }
   }

   public boolean isTool(ItemStack item, String toolType) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         PersistentDataContainer container = meta.getPersistentDataContainer();
         NamespacedKey usesKey = this.getUsesKeyForTool(toolType);
         return usesKey == null ? false : container.has(usesKey, PersistentDataType.INTEGER);
      } else {
         return false;
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      String cmd = command.getName().toLowerCase();
      return cmd.equals("tool") ? this.handleToolCommand(sender, args) : false;
   }

   private boolean handleToolCommand(CommandSender sender, String[] args) {
      if (!sender.hasPermission("tools.admin")) {
         sender.sendMessage(this.getLang("no-permission"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.getLang("command-usage"));
         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "give":
               if (args.length >= 2) {
                  this.handleGiveCommand(sender, args);
               } else {
                  sender.sendMessage(this.getLang("command-usage"));
               }

               return true;
            case "reload":
               this.reloadTools(sender);
               return true;
            case "items":
               if (sender instanceof Player) {
                  Player player = (Player)sender;
                  this.toolsGUI.openGUI(player);
               } else {
                  sender.sendMessage(this.getLang("player-only"));
               }

               return true;
            case "help":
               sender.sendMessage(this.getLang("command-usage"));
               return true;
            default:
               sender.sendMessage(this.getLang("command-usage"));
               return true;
         }
      }
   }

   private void handleGiveCommand(CommandSender sender, String[] args) {
      if (args.length == 2) {
         if (sender instanceof Player) {
            Player player = (Player)sender;
            this.giveTool(player, args[1], 1);
         } else {
            sender.sendMessage(this.getLang("player-only"));
         }
      } else {
         if (args.length >= 3) {
            String targetName = args[1];
            String toolName = args[2];
            int amount = 1;
            if (args.length < 4 || !args[3].equalsIgnoreCase("uses") && !args[3].equalsIgnoreCase("time") && !args[3].equalsIgnoreCase("infinite")) {
               if (args.length >= 4) {
                  try {
                     amount = Integer.parseInt(args[3]);
                     if (amount < 1) {
                        amount = 1;
                     }

                     if (amount > 64) {
                        amount = 64;
                     }
                  } catch (NumberFormatException var11) {
                     sender.sendMessage(this.getLang("invalid-amount"));
                     return;
                  }

                  Player target;
                  if ((target = Bukkit.getPlayer(targetName)) == null) {
                     sender.sendMessage(this.getLang("player-not-found", "player", targetName));
                     return;
                  }

                  this.giveToolToPlayer(sender, target, toolName, amount);
                  return;
               }

               Player target;
               if ((target = Bukkit.getPlayer(targetName)) == null) {
                  sender.sendMessage(this.getLang("player-not-found", "player", targetName));
                  return;
               }

               this.giveToolToPlayer(sender, target, toolName, 1);
               return;
            }

            String mode = args[3].toLowerCase();
            String value = args.length >= 5 ? args[4] : null;
            if (args.length >= 6) {
               try {
                  amount = Integer.parseInt(args[5]);
                  if (amount < 1) {
                     amount = 1;
                  }

                  if (amount > 64) {
                     amount = 64;
                  }
               } catch (NumberFormatException var10) {
                  sender.sendMessage(this.getLang("invalid-amount"));
                  return;
               }
            }

            Player target;
            if ((target = Bukkit.getPlayer(targetName)) == null) {
               sender.sendMessage(this.getLang("player-not-found", "player", targetName));
               return;
            }

            this.giveToolWithCustomDurability(sender, target, toolName, mode, value, amount);
            return;
         }

         sender.sendMessage(this.getLang("command-usage"));
      }

   }

   private void giveToolWithCustomDurability(CommandSender sender, Player target, String toolName, String mode, String value, int amount) {
      File toolFile = new File(this.toolsFolder, toolName + ".yml");
      if (!toolFile.exists()) {
         sender.sendMessage(this.getLang("tool-not-found"));
      } else {
         YamlConfiguration toolConfig = YamlConfiguration.loadConfiguration(toolFile);
         int defaultMaxUses = toolConfig.getInt("durability.uses", 100);
         String defaultTime = toolConfig.getString("durability.time", "3d");
         int customUses = defaultMaxUses;
         long customTime = this.parseDuration(defaultTime);
         String modeDisplay = "";
         String valueText = "";
         switch (mode.toLowerCase()) {
            case "uses":
               modeDisplay = this.getLang("mode-uses");
               if (value != null && !value.isEmpty()) {
                  try {
                     customUses = Integer.parseInt(value);
                     if (customUses < 1) {
                        customUses = 1;
                     }

                     if (customUses > 10000) {
                        customUses = 10000;
                     }
                  } catch (NumberFormatException var21) {
                     sender.sendMessage(this.getLang("invalid-amount"));
                     return;
                  }
               }

               valueText = String.valueOf(customUses);
               break;
            case "time":
               modeDisplay = this.getLang("mode-time");
               if (value != null && !value.isEmpty()) {
                  customTime = this.parseDuration(value);
                  if (customTime <= 0L) {
                     customTime = this.parseDuration(defaultTime);
                  }
               }

               valueText = this.formatTime(customTime);
               break;
            case "infinite":
               modeDisplay = this.getLang("mode-infinite");
               valueText = "∞";
               break;
            default:
               sender.sendMessage(this.getLang("invalid-mode", "mode", mode));
               return;
         }

         ItemStack toolItem = null;
         String displayName = "";
         switch (toolName.toLowerCase()) {
            case "drill":
               toolItem = this.drill.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.drill.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ᴘɪᴄᴋᴀxᴇ");
               break;
            case "treechopper":
               toolItem = this.treeChopper.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.treeChopper.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ᴀxᴇ");
               break;
            case "shovel":
               toolItem = this.shovel.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.shovel.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ꜱʜᴏᴠᴇʟ");
               break;
            case "hoe":
               toolItem = this.hoe.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.hoe.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ʜᴏᴇ");
               break;
            case "enderpearl":
               toolItem = this.enderPearl.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.enderPearl.getConfig().getString("name", "&#A20AD6ɪɴꜰɪɴɪᴛᴇ ᴇɴᴅᴇʀ ᴘᴇᴀʀʟ");
               break;
            case "rocket":
               toolItem = this.rocket.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.rocket.getConfig().getString("name", "&#A20AD6ɪɴꜰɪɴɪᴛʏ ʀᴏᴄᴋᴇᴛ");
               break;
            case "waterbucket":
               toolItem = this.waterBucket.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.waterBucket.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴡᴀᴛᴇʀ ʙᴜᴄᴋᴇᴛ");
               break;
            case "lavabucket":
               toolItem = this.lavaBucket.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.lavaBucket.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ʟᴀᴠᴀ ʙᴜᴄᴋᴇᴛ");
               break;
            case "jump":
               toolItem = this.jump.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.jump.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴊᴜᴍᴘ");
               break;
            case "chunkdrill":
               toolItem = this.chunkDrill.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.chunkDrill.getConfig().getString("name", "&#A20AD6ᴄʜᴜɴᴋ ᴅʀɪʟʟ");
               break;
            case "totem":
               toolItem = this.totem.getItemWithCustomDurability(target, mode.toLowerCase(), customUses, customTime);
               displayName = this.totem.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴛᴏᴛᴇᴍ");
               break;
            default:
               sender.sendMessage(this.getLang("tool-not-found"));
               return;
         }

         if (toolItem != null) {
            toolItem.setAmount(amount);
            target.getInventory().addItem(new ItemStack[]{toolItem});
            translatedName = HexColorCode.translateAllColorCodes(displayName);
            String senderMessage = this.getLang("tool-given-custom", "tool_name", translatedName, "mode", modeDisplay, "value", valueText, "player", target.getName());
            String targetMessage = this.getLang("tool-received-custom", "tool_name", translatedName, "mode", modeDisplay, "value", valueText);
            sender.sendMessage(senderMessage);
            target.sendMessage(targetMessage);
         }

      }
   }

   private void giveTool(Player player, String toolName, int amount) {
      this.giveToolToPlayer(player, player, toolName, amount);
   }

   private void giveToolToPlayer(CommandSender sender, Player target, String toolName, int amount) {
      File toolFile = new File(this.toolsFolder, toolName + ".yml");
      if (!toolFile.exists()) {
         sender.sendMessage(this.getLang("tool-not-found"));
      } else {
         ItemStack toolItem = null;
         String displayName = "";
         switch (toolName.toLowerCase()) {
            case "drill":
               toolItem = this.drill.getItemForPlayer(target);
               displayName = this.drill.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ᴘɪᴄᴋᴀxᴇ");
               break;
            case "treechopper":
               toolItem = this.treeChopper.getItemForPlayer(target);
               displayName = this.treeChopper.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ᴀxᴇ");
               break;
            case "shovel":
               toolItem = this.shovel.getItemForPlayer(target);
               displayName = this.shovel.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ꜱʜᴏᴠᴇʟ");
               break;
            case "hoe":
               toolItem = this.hoe.getItemForPlayer(target);
               displayName = this.hoe.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʏꜱᴛ ʜᴏᴇ");
               break;
            case "enderpearl":
               toolItem = this.enderPearl.getItemForPlayer(target);
               displayName = this.enderPearl.getConfig().getString("name", "&#A20AD6ɪɴꜰɪɴɪᴛᴇ ᴇɴᴅᴇʀ ᴘᴇᴀʀʟ");
               break;
            case "rocket":
               toolItem = this.rocket.getItemForPlayer(target);
               displayName = this.rocket.getConfig().getString("name", "&#A20AD6ɪɴꜰɪɴɪᴛʏ ʀᴏᴄᴋᴇᴛ");
               break;
            case "waterbucket":
               toolItem = this.waterBucket.getItemForPlayer(target);
               displayName = this.waterBucket.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴡᴀᴛᴇʀ ʙᴜᴄᴋᴇᴛ");
               break;
            case "lavabucket":
               toolItem = this.lavaBucket.getItemForPlayer(target);
               displayName = this.lavaBucket.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ʟᴀᴠᴀ ʙᴜᴄᴋᴇᴛ");
               break;
            case "jump":
               toolItem = this.jump.getItemForPlayer(target);
               displayName = this.jump.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴊᴜᴍᴘ");
               break;
            case "chunkdrill":
               toolItem = this.chunkDrill.getItemForPlayer(target);
               displayName = this.chunkDrill.getConfig().getString("name", "&#A20AD6ᴄʜᴜɴᴋ ᴅʀɪʟʟ");
               break;
            case "totem":
               toolItem = this.totem.getItemForPlayer(target);
               displayName = this.totem.getConfig().getString("name", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ᴛᴏᴛᴇᴍ");
               break;
            default:
               sender.sendMessage(this.getLang("tool-not-found"));
               return;
         }

         if (toolItem != null) {
            toolItem.setAmount(amount);
            target.getInventory().addItem(new ItemStack[]{toolItem});
            translatedName = HexColorCode.translateAllColorCodes(displayName);
            if (sender == target) {
               sender.sendMessage(this.getLang("tool-received", "tool_name", translatedName, "amount", String.valueOf(amount)));
            } else {
               sender.sendMessage(this.getLang("tool-given-to-other", "tool_name", translatedName, "amount", String.valueOf(amount), "player", target.getName()));
               target.sendMessage(this.getLang("tool-received-from-other", "tool_name", translatedName, "amount", String.valueOf(amount), "sender", sender.getName()));
            }
         }

      }
   }

   private void reloadTools(CommandSender sender) {
      String BRIGHT_CYAN = "\u001b[96m";
      String BRIGHT_RED = "\u001b[91m";
      String RESET = "\u001b[0m";
      this.getLogger().info("\u001b[96mReloading configuration...\u001b[0m");
      this.loadLangFile();
      this.loadGUIConfig();
      this.reloadToolInstances();
      this.toolsGUI.reloadGUI();
      this.plotService = new PlotService(this);
      this.blacklistedBlocks = null;
      if (this.expiryChecker != null) {
         this.expiryChecker.stop();
         this.expiryChecker = new ToolExpiryChecker(this);
         this.expiryChecker.start();
      }

      this.getLogger().info("\u001b[91m╠ Reloaded lang.yml!\u001b[0m");
      this.getLogger().info("\u001b[91m╠ Reloaded gui.yml!\u001b[0m");
      this.getLogger().info("\u001b[91m╠ Reloaded Tools!\u001b[0m");
      this.getLogger().info("\u001b[96m╚ Successful reload!\u001b[0m");
      sender.sendMessage(this.getLang("plugin-reloaded"));
      if (sender instanceof Player player) {
         String soundName = this.getGuiConfig().getString("reload-sound", "entity.player.levelup");

         try {
            player.playSound(player.getLocation(), soundName, 1.0F, 1.0F);
         } catch (Exception var10) {
            try {
               player.playSound(player.getLocation(), "entity.player.levelup", 1.0F, 1.0F);
            } catch (Exception var9) {
            }
         }
      }

   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      ArrayList<String> completions = new ArrayList();
      if (!sender.hasPermission("tools.admin")) {
         return completions;
      } else {
         if (args.length == 1) {
            completions.add("give");
            completions.add("reload");
            completions.add("items");
            completions.add("help");
            String input = args[0].toLowerCase();
            completions.removeIf((completion) -> !completion.toLowerCase().startsWith(input));
         } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
               for(Player player : Bukkit.getOnlinePlayers()) {
                  completions.add(player.getName());
               }
            }

            String input = args[1].toLowerCase();
            completions.removeIf((completion) -> !completion.toLowerCase().startsWith(input));
         } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
               completions.add("drill");
               completions.add("treechopper");
               completions.add("shovel");
               completions.add("hoe");
               completions.add("enderpearl");
               completions.add("rocket");
               completions.add("waterbucket");
               completions.add("lavabucket");
               completions.add("jump");
               completions.add("chunkdrill");
               completions.add("totem");
            }

            String input = args[2].toLowerCase();
            completions.removeIf((completion) -> !completion.toLowerCase().startsWith(input));
         } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
               completions.add("uses");
               completions.add("time");
               completions.add("infinite");
            }

            String input = args[3].toLowerCase();
            completions.removeIf((completion) -> !completion.toLowerCase().startsWith(input));
         } else if (args.length == 5) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
               String mode = args[3].toLowerCase();
               if (mode.equals("uses")) {
                  completions.add("1");
                  completions.add("10");
                  completions.add("50");
                  completions.add("100");
                  completions.add("500");
                  completions.add("1000");
               } else if (mode.equals("time")) {
                  completions.add("1d");
                  completions.add("3d");
                  completions.add("7d");
                  completions.add("30d");
                  completions.add("1h");
                  completions.add("12h");
                  completions.add("24h");
               } else if (mode.equals("infinite")) {
               }
            }

            String input = args[4].toLowerCase();
            completions.removeIf((completion) -> !completion.toLowerCase().startsWith(input));
         } else if (args.length == 6) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give")) {
               completions.add("1");
               completions.add("5");
               completions.add("10");
               completions.add("16");
               completions.add("32");
               completions.add("64");
            }

            String input = args[5].toLowerCase();
            completions.removeIf((completion) -> !completion.toLowerCase().startsWith(input));
         }

         return completions;
      }
   }

   public File getToolsFolder() {
      return this.toolsFolder;
   }

   public Drill getDrill() {
      return this.drill;
   }

   public TreeChopper getTreeChopper() {
      return this.treeChopper;
   }

   public Shovel getShovel() {
      return this.shovel;
   }

   public Hoe getHoe() {
      return this.hoe;
   }

   public Rocket getRocket() {
      return this.rocket;
   }

   public WaterBucket getWaterBucket() {
      return this.waterBucket;
   }

   public LavaBucket getLavaBucket() {
      return this.lavaBucket;
   }

   public Jump getJump() {
      return this.jump;
   }

   public ChunkDrill getChunkDrill() {
      return this.chunkDrill;
   }

   public Totem getTotem() {
      return this.totem;
   }

   public FileConfiguration getLangConfig() {
      return this.langConfig;
   }

   public FileConfiguration getGuiConfig() {
      return this.guiConfig;
   }

   public ToolsGUI getToolsGUI() {
      return this.toolsGUI;
   }

   public EnderPearl getEnderPearl() {
      return this.enderPearl;
   }

   public ToolExpiryChecker getExpiryChecker() {
      return this.expiryChecker;
   }
}
