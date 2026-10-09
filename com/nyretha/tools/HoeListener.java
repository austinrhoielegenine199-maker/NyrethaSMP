package com.nyretha.tools;

import org.bukkit.event.Listener;

/** Integration scaffold for the HoeListener event handler; behavior will be ported during main-class integration. */
public class HoeListener implements Listener {
    private final Tools plugin;
    public HoeListener(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
