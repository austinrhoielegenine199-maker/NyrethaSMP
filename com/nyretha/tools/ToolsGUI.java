package com.nyretha.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.Listener;

/** GUI entry point scaffold; layout is defined in core/tools/gui.yml. */
public class ToolsGUI implements Listener {
    private final Tools plugin;
    public ToolsGUI(Tools plugin) { this.plugin = plugin; }
    public void open(Player player) {
        if (player == null) return;
        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("gui.rows", 3)));
        Inventory inventory = Bukkit.createInventory(null, rows * 9, "Tools");
        player.openInventory(inventory);
    }
    public Tools getPlugin() { return plugin; }
}
