package com.nyretha.tools;

import org.bukkit.event.Listener;

/** Integration scaffold for WaterBucketListener; event behavior is pending porting. */
public class WaterBucketListener implements Listener {
    private final Tools plugin;
    public WaterBucketListener(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
