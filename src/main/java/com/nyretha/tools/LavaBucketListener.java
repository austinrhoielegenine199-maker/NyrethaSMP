package com.nyretha.tools;

import org.bukkit.event.Listener;

/** Integration scaffold for the LavaBucketListener event handler; behavior will be ported during main-class integration. */
public class LavaBucketListener implements Listener {
    private final Tools plugin;
    public LavaBucketListener(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
