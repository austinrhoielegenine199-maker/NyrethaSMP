package com.nyretha.homes;

import com.nyretha.homes.integration.TeamHomeButtonListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Homes entry point with a working five-home GUI and the optional Teams integration.
 * Home locations are stored in this plugin's config.yml under players.<uuid>.homes.<index>.
 */
public final class Home extends JavaPlugin implements Listener {
    private static final int[] BED_SLOTS = {12, 13, 14, 15, 16};
    private static final int[] DYE_SLOTS = {21, 22, 23, 24, 25};
    private static final String HOMES_TITLE = color("&8ʜᴏᴍᴇꜱ");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        new TeamHomeButtonListener(this).register();
        getLogger().info("NyrethaHomes enabled.");
    }

    public void openHomesGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, HOMES_TITLE);
        inv.setItem(19, item(Material.BLUE_DYE, "&bʀᴇɴᴀᴍᴇ ᴛᴇᴀᴍ ʜᴏᴍᴇ", "&fClick to rename your team home"));
        for (int i = 0; i < BED_SLOTS.length; i++) {
            Location home = getHome(player, i);
            if (home == null) {
                inv.setItem(BED_SLOTS[i], item(Material.LIGHT_GRAY_BED, "&7ɴᴏ ʜᴏᴍᴇ ꜱᴇᴛ",
                        "&fLeft-click to save your location"));
                inv.setItem(DYE_SLOTS[i], item(Material.GRAY_DYE, "&7ɴᴏ ʜᴏᴍᴇ ꜱᴇᴛ",
                        "&fClick to save your location"));
            } else {
                inv.setItem(BED_SLOTS[i], item(Material.LIGHT_BLUE_BED, "&#0044FCʜᴏᴍᴇ " + (i + 1),
                        "&fLeft-click to teleport"));
                inv.setItem(DYE_SLOTS[i], item(Material.BLUE_DYE, "&#0044FCʜᴏᴍᴇ " + (i + 1),
                        "&fClick to delete"));
            }
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onHomesClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (!HOMES_TITLE.equals(event.getView().getTitle())) return;

        int rawSlot = event.getRawSlot();
        int index = slotIndex(rawSlot, BED_SLOTS);
        boolean dyeButton = false;
        if (index < 0) {
            index = slotIndex(rawSlot, DYE_SLOTS);
            dyeButton = index >= 0;
        }
        if (index < 0) return;
        event.setCancelled(true);

        Location home = getHome(player, index);
        if (dyeButton) {
            if (home == null) {
                saveHome(player, index, player.getLocation());
                player.sendMessage(color("&aHome " + (index + 1) + " saved."));
            } else {
                getConfig().set(homePath(player, index), null);
                saveConfig();
                player.sendMessage(color("&cHome " + (index + 1) + " deleted."));
            }
            openHomesGUI(player);
            return;
        }

        if (home == null) {
            saveHome(player, index, player.getLocation());
            player.sendMessage(color("&aHome " + (index + 1) + " saved."));
            openHomesGUI(player);
            return;
        }
        if (home.getWorld() == null) {
            player.sendMessage(color("&cThat home's world is unavailable."));
            return;
        }
        player.closeInventory();
        final int homeIndex = index;
        player.teleportAsync(home).thenAccept(success -> {
            if (success) player.sendMessage(color("&aTeleported to home " + (homeIndex + 1) + "."));
            else player.sendMessage(color("&cCould not teleport to that home."));
        });
    }

    private int slotIndex(int slot, int[] slots) {
        for (int i = 0; i < slots.length; i++) if (slots[i] == slot) return i;
        return -1;
    }

    private String homePath(Player player, int index) {
        return "players." + player.getUniqueId() + ".homes." + index;
    }

    private Location getHome(Player player, int index) {
        String path = homePath(player, index);
        if (!getConfig().contains(path + ".world")) return null;
        World world = Bukkit.getWorld(getConfig().getString(path + ".world", ""));
        if (world == null) return null;
        return new Location(world,
                getConfig().getDouble(path + ".x"),
                getConfig().getDouble(path + ".y"),
                getConfig().getDouble(path + ".z"),
                (float) getConfig().getDouble(path + ".yaw"),
                (float) getConfig().getDouble(path + ".pitch"));
    }

    private void saveHome(Player player, int index, Location loc) {
        String path = homePath(player, index);
        getConfig().set(path + ".world", loc.getWorld() == null ? "" : loc.getWorld().getName());
        getConfig().set(path + ".x", loc.getX());
        getConfig().set(path + ".y", loc.getY());
        getConfig().set(path + ".z", loc.getZ());
        getConfig().set(path + ".yaw", loc.getYaw());
        getConfig().set(path + ".pitch", loc.getPitch());
        saveConfig();
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> lines = new ArrayList<>();
            for (String line : lore) lines.add(color(line));
            meta.setLore(lines);
            stack.setItemMeta(meta);
        }
        return stack;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("home") || command.getName().equalsIgnoreCase("homes")) {
            openHomesGUI(player);
            return true;
        }
        return false;
    }
}
