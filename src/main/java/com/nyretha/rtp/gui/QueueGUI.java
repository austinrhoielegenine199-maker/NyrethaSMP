package com.nyretha.rtp.gui;

import com.nyretha.rtp.RTP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** Queue status menu for players waiting for an RTP operation. */
public class QueueGUI {
    private final RTP plugin;

    public QueueGUI(RTP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int position, int queueSize) {
        if (player == null) return;
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&8RTP Queue"));
        ItemStack status = new ItemStack(Material.CLOCK);
        ItemMeta meta = status.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Queue Status");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Your position: " + Math.max(1, position),
                    ChatColor.GRAY + "Players queued: " + Math.max(0, queueSize),
                    ChatColor.YELLOW + "Please wait..."
            ));
            status.setItemMeta(meta);
        }
        inventory.setItem(13, status);
        inventory.setItem(22, new ItemStack(Material.BARRIER));
        player.openInventory(inventory);
    }

    public RTP getPlugin() {
        return plugin;
    }
}
