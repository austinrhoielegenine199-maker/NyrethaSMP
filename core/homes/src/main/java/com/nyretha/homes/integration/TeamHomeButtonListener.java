package com.nyretha.homes.integration;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Homes-side integration for the optional Teams plugin.
 * Register this once from Homes.onEnable(): new TeamHomeButtonListener(this).register();
 * Team plugin is accessed reflectively so Homes can still load without Teams installed.
 */
public final class TeamHomeButtonListener implements Listener {
    private static final int TEAM_HOME_SLOT = 10;
    private static final int CANCEL_SLOT = 10;
    private static final int INFO_SLOT = 13;
    private static final int CONFIRM_SLOT = 16;
    private static final String CONFIRM_TITLE = color("&8ᴛᴇᴀᴍ ʜᴏᴍᴇ");
    private final JavaPlugin plugin;
    private final Map<UUID, String> pendingRename = new java.util.HashMap<>();

    public TeamHomeButtonListener(JavaPlugin plugin) { this.plugin = plugin; }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isHomesInventory(player.getOpenInventory().getTitle())) {
                    updateButton(player, player.getOpenInventory().getTopInventory());
                }
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onHomesOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isHomesInventory(event.getView().getTitle())) {
            updateButton(player, event.getInventory());
        }
    }

    @EventHandler
    public void onHomesClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        String title = event.getView().getTitle();
        int slot = event.getRawSlot();

        if (isConfirmInventory(title)) {
            event.setCancelled(true);
            if (slot == CANCEL_SLOT) {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> openHomesAgain(player));
            } else if (slot == CONFIRM_SLOT) {
                TeamState state = getTeamState(player);
                if (!state.hasTeam || state.home == null || state.home.getWorld() == null) {
                    player.closeInventory();
                    player.sendMessage(color("&cYour team home is no longer available."));
                    return;
                }
                player.closeInventory();
                // Use Teams' own manager so its permission checks, cooldown and teleport delay remain active.
                try {
                    Object homeManager = call(state.teamsPlugin, "getTeamHome");
                    Object result = call(homeManager, "teleportToHome", player);
                    if (result instanceof Boolean && !((Boolean) result)) {
                        player.sendMessage(color("&cCould not teleport to your team home."));
                    }
                } catch (ReflectiveOperationException ex) {
                    player.sendMessage(color("&cTeams plugin does not expose its team-home manager."));
                }
            }
            return;
        }

        if (!isHomesInventory(title) || (slot != TEAM_HOME_SLOT && slot != 19)) return;
        event.setCancelled(true);
        TeamState state = getTeamState(player);
        if (!state.hasTeam) {
            player.sendMessage(color("&cYou have no team!"));
            return;
        }
        if (slot == 19) {
            if (state.home == null || state.home.getWorld() == null) {
                player.sendMessage(color("&7Your team has not set a home yet."));
                return;
            }
            beginRename(player, state);
            return;
        }
        if (state.home == null || state.home.getWorld() == null) {
            player.sendMessage(color("&7Your team has not set a home yet."));
            return;
        }
        if (event.getClick().isRightClick()) {
            beginRename(player, state);
        } else if (event.getClick().isLeftClick() || event.getClick() == ClickType.UNKNOWN) {
            openConfirmGUI(player, state);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { pendingRename.remove(event.getPlayer().getUniqueId()); }

    private void openConfirmGUI(Player player, TeamState state) {
        Inventory gui = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        gui.setItem(CANCEL_SLOT, item(Material.RED_STAINED_GLASS_PANE, "&cCancel", "&7Return to Homes"));
        String homeName = getStoredHomeName(state, player);
        gui.setItem(INFO_SLOT, item(Material.BLUE_BANNER, "&#0044FC" + homeName, "&7Team home", "&fConfirm to teleport"));
        gui.setItem(CONFIRM_SLOT, item(Material.GREEN_STAINED_GLASS_PANE, "&aConfirm", "&7Teleport to your team home"));
        player.openInventory(gui);
    }

    private void beginRename(Player player, TeamState state) {
        String inputApi = plugin.getConfig().getString("team-home.rename-input", "ChatAPI");
        String oldName = getStoredHomeName(state, player);
        pendingRename.put(player.getUniqueId(), state.teamName);
        BiConsumer<Player, String> callback = (renamer, input) -> {
            String newName = input == null ? "" : input.trim();
            String teamName = pendingRename.remove(renamer.getUniqueId());
            if (teamName == null || newName.isEmpty()) {
                renamer.sendMessage(color("&cTeam home name was not changed."));
                return;
            }
            if (newName.length() > 32) {
                renamer.sendMessage(color("&cTeam home name must be 32 characters or fewer."));
                return;
            }
            Plugin teams = Bukkit.getPluginManager().getPlugin("Teams");
            if (teams == null || !teams.isEnabled()) return;
            try {
                FileConfiguration cfg = (FileConfiguration) call(teams, "getConfig");
                cfg.set("team-home.display-names." + teamName, newName);
                call(teams, "saveConfig");
                renamer.sendMessage(color("&aTeam home renamed to &b" + newName + "&a."));
                if (isHomesInventory(renamer.getOpenInventory().getTitle())) {
                    updateButton(renamer, renamer.getOpenInventory().getTopInventory());
                }
            } catch (ReflectiveOperationException ex) {
                renamer.sendMessage(color("&cCould not save the team home name."));
            }
        };

        try {
            if ("SignAPI".equalsIgnoreCase(inputApi)) {
                Object signManager = call(plugin, "getSignManager");
                call(signManager, "openSignEditor", player, oldName, callback);
            } else {
                Object chatManager = call(plugin, "getChatManager");
                call(chatManager, "openChatEditor", player, "Enter a new name for your team home:", callback);
            }
        } catch (ReflectiveOperationException ex) {
            pendingRename.remove(player.getUniqueId());
            player.sendMessage(color("&cRename input is unavailable. Check team-home.rename-input in config.yml."));
        }
    }

    private void updateButton(Player player, Inventory inventory) {
        if (inventory == null || inventory.getSize() <= TEAM_HOME_SLOT) return;
        TeamState state = getTeamState(player);
        Material material;
        String name;
        String lore;
        if (!state.hasTeam) {
            material = Material.RED_BANNER;
            name = "&cʏᴏᴜ ʜᴀᴠᴇ ɴᴏ ᴛᴇᴀᴍ!";
            lore = "&7Join or create a team to use Team Home.";
        } else if (state.home == null || state.home.getWorld() == null) {
            material = Material.GRAY_BANNER;
            name = "&7ᴛᴇᴀᴍ ʜᴏᴍᴇ";
            lore = "&7Your team has not set a home yet.";
        } else {
            material = Material.BLUE_BANNER;
            name = "&#0044FC" + getStoredHomeName(state, player);
            lore = "&fLeft-click to teleport; right-click to rename.";
        }
        inventory.setItem(TEAM_HOME_SLOT, item(material, name, lore));
    }

    private TeamState getTeamState(Player player) {
        Plugin teams = Bukkit.getPluginManager().getPlugin("Teams");
        if (teams == null || !teams.isEnabled()) return TeamState.none();
        try {
            Object manager = call(teams, "getTeamManager");
            Object team = call(manager, "getPlayerTeam", player.getName());
            if (team == null) return TeamState.none();
            Object location = call(team, "getHome");
            Object teamName = call(team, "getName");
            return new TeamState(true, location instanceof Location ? (Location) location : null,
                    String.valueOf(teamName), teams);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return TeamState.none();
        }
    }

    private String getStoredHomeName(TeamState state, Player player) {
        if (state.teamName == null || state.teamsPlugin == null) return "ᴛᴇᴀᴍ ʜᴏᴍᴇ";
        try {
            FileConfiguration cfg = (FileConfiguration) call(state.teamsPlugin, "getConfig");
            return cfg.getString("team-home.display-names." + state.teamName, "ᴛᴇᴀᴍ ʜᴏᴍᴇ");
        } catch (ReflectiveOperationException ex) {
            return "ᴛᴇᴀᴍ ʜᴏᴍᴇ";
        }
    }

    private void openHomesAgain(Player player) {
        try {
            call(plugin, "openHomesGUI", player);
        } catch (ReflectiveOperationException ignored) { }
    }

    private boolean isHomesInventory(String title) {
        String plain = ChatColor.stripColor(title == null ? "" : title).toLowerCase(Locale.ROOT);
        return plain.contains("homes") || plain.contains("ʜᴏᴍᴇꜱ");
    }

    private boolean isConfirmInventory(String title) { return CONFIRM_TITLE.equals(title); }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (String line : lore) lines.add(color(line));
            meta.setLore(lines);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Object call(Object target, String methodName, Object... args) throws ReflectiveOperationException {
        if (target == null) throw new NoSuchMethodException(methodName);
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) continue;
            try { return method.invoke(target, args); }
            catch (IllegalArgumentException ignored) { }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static String color(String text) {
        Matcher matcher = Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x§" + hex.charAt(0) + "§" + hex.charAt(1) + "§" + hex.charAt(2)
                    + "§" + hex.charAt(3) + "§" + hex.charAt(4) + "§" + hex.charAt(5);
            matcher.appendReplacement(out, replacement);
        }
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    private static final class TeamState {
        final boolean hasTeam;
        final Location home;
        final String teamName;
        final Plugin teamsPlugin;
        TeamState(boolean hasTeam, Location home, String teamName, Plugin teamsPlugin) {
            this.hasTeam = hasTeam; this.home = home; this.teamName = teamName; this.teamsPlugin = teamsPlugin;
        }
        static TeamState none() { return new TeamState(false, null, null, null); }
    }
}
