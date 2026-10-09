package com.nyretha.home.GUI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Builds the Homes selection inventory. */
public final class MainGUI {
    public static final String TITLE = ChatColor.DARK_GRAY + "Homes";
    private MainGUI() {}
    public static Inventory create(Player player, int maxHomes) {
        Inventory inventory = Bukkit.createInventory(player, 36, TITLE);
        for (int slot = 11; slot <= 15; slot++) {
            int number = slot - 10;
            ItemStack item = new ItemStack(Material.LIGHT_GRAY_BED);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) { meta.setDisplayName(ChatColor.GRAY + "Home " + number); item.setItemMeta(meta); }
            inventory.setItem(slot, item);
        }
        return inventory;
    }
}
