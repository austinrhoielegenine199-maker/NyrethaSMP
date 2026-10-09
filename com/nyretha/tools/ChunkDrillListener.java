package com.nyretha.tools;

import org.bukkit.event.Listener;

/** Integration scaffold for the ChunkDrillListener event handler; behavior will be ported during main-class integration. */
public class ChunkDrillListener implements Listener {
    private final Tools plugin;
    public ChunkDrillListener(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
