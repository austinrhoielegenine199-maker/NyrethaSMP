package com.nyretha.tools;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Optional protection integration hook; the main module wires supported region APIs. */
public class WorldGuardService {
    private final Tools plugin;
    public WorldGuardService(Tools plugin) { this.plugin = plugin; }
    public boolean canUse(Player player, Location location) { return player != null && location != null; }
    public Tools getPlugin() { return plugin; }
}
