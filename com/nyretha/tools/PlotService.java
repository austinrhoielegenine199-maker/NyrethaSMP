package com.nyretha.tools;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Optional PlotSquared integration hook. */
public class PlotService {
    private final Tools plugin;
    public PlotService(Tools plugin) { this.plugin = plugin; }
    public boolean canBuild(Player player, Location location) { return player != null && location != null; }
    public Tools getPlugin() { return plugin; }
}
