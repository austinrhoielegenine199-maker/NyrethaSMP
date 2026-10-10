package com.nyretha.home;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import com.nyretha.teams.Team;
import com.nyretha.teams.Manager.DataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Standalone homes plugin module, organized to match Nyretha's other core modules. */
public final class Home extends JavaPlugin implements Listener {
    private final Map<UUID, Long> teleporting = new HashMap<>();
    private File homesFile;
    private YamlConfiguration data;
    private YamlConfiguration guiMessages;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResource("lang.yml", false);
        saveResource("gui/main.gui.yml", false);
        saveResource("gui/confirm.gui.yml", false);
        homesFile = new File(getDataFolder(), "homes.yml");
        data = YamlConfiguration.loadConfiguration(homesFile);
        guiMessages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui/main.gui.yml"));
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("NyrethaHomes enabled.");
    }
    @Override public void onDisable() { saveHomes(); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("home") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("home.reload")) { sender.sendMessage(c("&cYou don't have permission for this!")); return true; }
            reloadConfig(); sender.sendMessage(c("&aHomes has been reloaded!")); return true;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage(c("&cOnly players can use this command!")); return true; }
        if (cmd.equals("home") && args.length == 0) { openHomes(p); return true; }
        int n = 1;
        if (args.length > 0) try { n = Integer.parseInt(args[0]); } catch (NumberFormatException e) { p.sendMessage(c("&cInvalid number!")); return true; }
        if (n < 1 || n > 5) { p.sendMessage(c("&cInvalid number!")); return true; }
        String key = p.getUniqueId() + "." + n;
        if (cmd.equals("sethome")) {
            int limit = getConfig().getInt("homes.default-max-homes", 3);
            if (n > limit && !p.hasPermission("home.limit." + n)) { p.sendMessage(c("&cYou have reached the maximum number of homes!")); return true; }
            data.set(key, p.getLocation().serialize()); saveHomes(); p.sendMessage(c("&fHome &a" + n + " &fhas been set")); return true;
        }
        if (cmd.equals("delhome")) {
            if (!data.contains(key)) { p.sendMessage(c("&fHome &c" + n + " &fwas not found!")); return true; }
            data.set(key, null); saveHomes(); p.sendMessage(c("&fHome &c" + n + " &fhas been deleted")); return true;
        }
        if (cmd.equals("home")) {
            if (!data.contains(key)) { p.sendMessage(c("&fHome &c" + n + " &fwas not found!")); return true; }
            teleport(p, readLocation(key), n); return true;
        }
        return false;
    }

    private void openHomes(Player p) {
        Inventory inv = Bukkit.createInventory(p, 36, c("&8Homes"));
        for (int n = 1; n <= 5; n++) {
            String key = p.getUniqueId() + "." + n;
            ItemStack item = new ItemStack(data.contains(key) ? Material.LIGHT_BLUE_BED : Material.LIGHT_GRAY_BED);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(c(data.contains(key) ? "&bHome " + n : "&7No home set"));
            item.setItemMeta(meta); inv.setItem(10 + n, item);
        }
        inv.setItem(10, buildTeamHomeItem(p));
        p.openInventory(inv);
    }

    @EventHandler public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Homes")) return;
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw == 10) {
            Team teams = getTeamsPlugin();
            if (teams == null) { p.sendMessage(guiMessage("team-home.messages.plugin-missing", "&cTeam homes are unavailable.")); return; }
            DataManager team = teams.getTeamManager().getPlayerTeam(p.getName());
            if (team == null) { p.sendMessage(guiMessage("team-home.messages.no-team", "&cYou don't have a team!")); return; }
            if (team.getHome() == null) { p.sendMessage(guiMessage("team-home.messages.no-home", "&7Your team has no home set.")); return; }
            Bukkit.dispatchCommand(p, "team home");
            return;
        }
        if (raw < 11 || raw > 15) return;
        int n = raw - 10; String key = p.getUniqueId() + "." + n;
        if (e.isRightClick() && data.contains(key)) {
            data.set(key, null); saveHomes();
            p.sendMessage(c("&aHome " + n + " deleted."));
            openHomes(p);
            return;
        }
        if (data.contains(key)) teleport(p, readLocation(key), n);
        else { data.set(key, p.getLocation().serialize()); saveHomes(); p.sendMessage(c("&fHome &a" + n + " &fhas been set")); openHomes(p); }
    }

    private ItemStack buildTeamHomeItem(Player player) {
        Team teams = getTeamsPlugin();
        DataManager team = teams == null ? null : teams.getTeamManager().getPlayerTeam(player.getName());
        boolean hasTeam = team != null;
        boolean hasHome = hasTeam && team.getHome() != null;
        String state = !hasTeam ? "no-team" : hasHome ? "set" : "unset";
        String base = "team-home.states." + state + ".";
        Material material;
        try { material = Material.valueOf(guiMessages.getString(base + "material", !hasTeam ? "RED_BANNER" : hasHome ? "BLUE_BANNER" : "GRAY_BANNER")); }
        catch (IllegalArgumentException ex) { material = !hasTeam ? Material.RED_BANNER : hasHome ? Material.BLUE_BANNER : Material.GRAY_BANNER; }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(c(guiMessages.getString(base + "name", !hasTeam ? "&cYou don't have a team!" : hasHome ? "&9Team Home" : "&7Team has no home")));
            meta.setLore(guiMessages.getStringList(base + "lore").stream().map(this::c).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private Team getTeamsPlugin() {
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("NyrethaTeams");
        return plugin instanceof Team team ? team : null;
    }

    private String guiMessage(String path, String fallback) {
        return c(guiMessages == null ? fallback : guiMessages.getString(path, fallback));
    }

    private Location readLocation(String key) {
        if (data.getConfigurationSection(key) == null) return null;
        return Location.deserialize(data.getConfigurationSection(key).getValues(false));
    }

    private void teleport(Player p, Location target, int n) {
        if (target == null || target.getWorld() == null) { p.sendMessage(c("&cHome location is invalid.")); return; }
        if (teleporting.containsKey(p.getUniqueId())) { p.sendMessage(c("&cYou are already teleporting!")); return; }
        long delay = Math.max(0, getConfig().getLong("homes.teleport-delay", 5));
        teleporting.put(p.getUniqueId(), System.currentTimeMillis());
        Location start = p.getLocation().clone();
        p.sendMessage(c("&fTeleport in &b" + delay + " &fseconds"));
        new BukkitRunnable() {
            @Override public void run() {
                teleporting.remove(p.getUniqueId());
                if (!p.isOnline()) return;
                if (p.getLocation().distanceSquared(start) > 0.09) { p.sendMessage(c("&cTeleport cancelled!")); return; }
                p.teleport(target); p.sendMessage(c("&aYou have been teleported to home " + n + "!"));
            }
        }.runTaskLater(this, delay * 20L);
    }

    private void saveHomes() {
        if (data == null || homesFile == null) return;
        try { data.save(homesFile); } catch (Exception ex) { getLogger().warning("Could not save homes.yml: " + ex.getMessage()); }
    }
    private String c(String text) { return ChatColor.translateAlternateColorCodes('&', text); }
}
