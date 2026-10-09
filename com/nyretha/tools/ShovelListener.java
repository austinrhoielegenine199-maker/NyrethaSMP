package com.nyretha.tools;

import org.bukkit.event.Listener;

/** Integration scaffold for the ShovelListener event handler; behavior will be ported during main-class integration. */
public class ShovelListener implements Listener {
    private final Tools plugin;
    public ShovelListener(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
