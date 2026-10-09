package com.nyretha.home;

import com.nyretha.home.listener.TeamHomeButtonListener;
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

/** Nyretha Homes entry point. Stores home locations by player UUID in config.yml. */
public final class Home extends JavaPlugin implements Listener {
    private static final int[] BED_SLOTS = {12, 13, 14, 15, 16};
    private static final int[] DYE_SLOTS = {21, 22, 23, 24, 25};
    private static final String GUI_TITLE = color("&8ʜᴏᴍᴇꜱ");

    @Override public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        new TeamHomeButtonListener(this).register();
        getLogger().info("NyrethaHomes enabled.");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        openHomesGUI((Player)sender);
        return true;
    }

    public void openHomesGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
        for (int i = 0; i < BED_SLOTS.length; i++) {
            Location home = getHome(player, i);
            inv.setItem(BED_SLOTS[i], item(home == null ? Material.LIGHT_GRAY_BED : Material.LIGHT_BLUE_BED,
                home == null ? "&7ɴᴏ ʜᴏᴍᴇ ꜱᴇᴛ" : "&#0044FCʜᴏᴍᴇ " + (i + 1),
                home == null ? "&fClick to save your location" : "&fClick to teleport"));
            inv.setItem(DYE_SLOTS[i], item(home == null ? Material.GRAY_DYE : Material.BLUE_DYE,
                home == null ? "&7ɴᴏ ʜᴏᴍᴇ ꜱᴇᴛ" : "&#0044FCʜᴏᴍᴇ " + (i + 1),
                home == null ? "&fClick to save your location" : "&fClick to delete"));
        }
        inv.setItem(19, item(Material.BLUE_DYE, "&bʀᴇɴᴀᴍᴇ ᴛᴇᴀᴍ ʜᴏᴍᴇ", "&fClick to rename your team home"));
        player.openInventory(inv);
    }

    @EventHandler public void onHomeClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player) || !GUI_TITLE.equals(e.getView().getTitle()) ||
            e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot = e.getRawSlot();
        int index = -1;
        for (int i = 0; i < BED_SLOTS.length; i++) if (BED_SLOTS[i] == slot || DYE_SLOTS[i] == slot) index = i;
        if (index < 0) return;
        e.setCancelled(true);
        Player p = (Player)e.getWhoClicked();
        if (DYE_SLOTS[index] == slot && getHome(p, index) != null) {
            getConfig().set(path(p, index), null); saveConfig(); openHomesGUI(p); return;
        }
        if (getHome(p, index) == null) {
            getConfig().set(path(p, index), p.getWorld().getName() + ";" + p.getLocation().getX() + ";" +
                p.getLocation().getY() + ";" + p.getLocation().getZ() + ";" + p.getLocation().getYaw() + ";" + p.getLocation().getPitch());
            saveConfig(); p.sendMessage(color("&aHome " + (index + 1) + " saved.")); openHomesGUI(p);
        } else {
            p.teleport(getHome(p, index)); p.closeInventory();
        }
    }

    public Location getHome(Player player, int index) {
        String raw = getConfig().getString(path(player, index));
        if (raw == null) return null;
        try {
            String[] a = raw.split(";");
            World w = Bukkit.getWorld(a[0]); if (w == null) return null;
            return new Location(w, Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                Float.parseFloat(a[4]), Float.parseFloat(a[5]));
        } catch (RuntimeException ex) { getLogger().warning("Invalid saved home for " + player.getName()); return null; }
    }
    private String path(Player p, int i) { return "players." + p.getUniqueId() + ".homes." + i; }
    private static ItemStack item(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta();
        if (meta != null) { meta.setDisplayName(color(name)); meta.setLore(java.util.Collections.singletonList(color(lore))); stack.setItemMeta(meta); }
        return stack;
    }
    private static String color(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(s);
        StringBuffer b = new StringBuffer();
        while (m.find()) { String h=m.group(1); m.appendReplacement(b, ChatColor.COLOR_CHAR+"x"+ChatColor.COLOR_CHAR+h.charAt(0)+ChatColor.COLOR_CHAR+h.charAt(1)+ChatColor.COLOR_CHAR+h.charAt(2)+ChatColor.COLOR_CHAR+h.charAt(3)+ChatColor.COLOR_CHAR+h.charAt(4)+ChatColor.COLOR_CHAR+h.charAt(5)); }
        m.appendTail(b); return ChatColor.translateAlternateColorCodes('&', b.toString());
    }
}