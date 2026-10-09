package com.nyretha.homes.integration;

import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bridges the Homes GUI to the optional Teams plugin without a compile-time dependency.
 * Register from the Homes JavaPlugin with: new TeamHomeButtonListener(plugin).register();
 */
public final class TeamHomeButtonListener implements Listener {
    private static final int TEAM_HOME_SLOT = 10;
    private final JavaPlugin plugin;

    public TeamHomeButtonListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Keep the open Homes inventory in sync when a team/home changes while it is open.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory top = player.getOpenInventory().getTopInventory();
                if (isHomesInventory(player.getOpenInventory().getTitle())) {
                    updateButton(player, top);
                }
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onHomesOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (isHomesInventory(event.getView().getTitle())) {
            updateButton(player, event.getInventory());
        }
    }

    @EventHandler
    public void onHomesClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isHomesInventory(event.getView().getTitle())) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getRawSlot() != TEAM_HOME_SLOT) return;

        event.setCancelled(true);
        TeamState state = getTeamState(player);
        if (!state.hasTeam) {
            player.sendMessage(color("&cYou have no team!"));
            return;
        }
        if (state.home == null || state.home.getWorld() == null) {
            player.sendMessage(color("&7Your team has not set a home yet."));
            return;
        }
        player.closeInventory();
        player.teleportAsync(state.home).thenAccept(success -> {
            if (success) {
                player.sendMessage(color("&bTeleported to your team home."));
            } else {
                player.sendMessage(color("&cCould not teleport to your team home."));
            }
        });
    }

    private boolean isHomesInventory(String title) {
        String plain = ChatColor.stripColor(title == null ? "" : title).toLowerCase(Locale.ROOT);
        return plain.contains("homes") || plain.contains("ʜᴏᴍᴇꜱ");
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
            name = "&#0044FCᴛᴇᴀᴍ ʜᴏᴍᴇ";
            lore = "&fClick to teleport to your team home.";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(java.util.Collections.singletonList(color(lore)));
            item.setItemMeta(meta);
        }
        inventory.setItem(TEAM_HOME_SLOT, item);
    }

    private TeamState getTeamState(Player player) {
        Plugin teams = Bukkit.getPluginManager().getPlugin("Teams");
        if (teams == null || !teams.isEnabled()) return new TeamState(false, null);
        try {
            Object manager = call(teams, "getTeamManager");
            Object team = call(manager, "getPlayerTeam", player.getName());
            if (team == null) return new TeamState(false, null);
            Object location = call(team, "getHome");
            return new TeamState(true, location instanceof Location ? (Location) location : null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Compatibility with builds that expose the same data through Team#getPlayerTeams.
            try {
                Object playerTeams = call(teams, "getPlayerTeams");
                Object team = ((java.util.Map<?, ?>) playerTeams).get(player.getName());
                if (team == null) return new TeamState(false, null);
                Object location = call(team, "getHome");
                return new TeamState(true, location instanceof Location ? (Location) location : null);
            } catch (ReflectiveOperationException | RuntimeException ignoredAgain) {
                return new TeamState(false, null);
            }
        }
    }

    private Object call(Object target, String methodName, Object... args) throws ReflectiveOperationException {
        if (target == null) throw new NoSuchMethodException(methodName);
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) continue;
            try {
                return method.invoke(target, args);
            } catch (IllegalArgumentException ignored) {
                // Try another overload with the same name.
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private String color(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(text);
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
        TeamState(boolean hasTeam, Location home) {
            this.hasTeam = hasTeam;
            this.home = home;
        }
    }
}
