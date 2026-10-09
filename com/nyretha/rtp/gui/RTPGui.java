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

/** Basic RTP destination menu; configurable layout is loaded during plugin integration. */
public class RTPGui {
    private final RTP plugin;

    public RTPGui(RTP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (player == null) return;
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&8Random Teleport"));
        inventory.setItem(11, item(Material.GRASS_BLOCK, "&aOverworld", "&7Random teleport in the overworld"));
        inventory.setItem(13, item(Material.NETHERRACK, "&cNether", "&7Random teleport in the nether"));
        inventory.setItem(15, item(Material.END_STONE, "&eThe End", "&7Random teleport in the End"));
        player.openInventory(inventory);
    }

    private ItemStack item(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', lore)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public RTP getPlugin() {
        return plugin;
    }
}
