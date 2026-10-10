package com.nyretha.tools;

import org.bukkit.event.Listener;

/** Integration scaffold for the TotemListener event handler; behavior will be ported during main-class integration. */
public class TotemListener implements Listener {
    private final Tools plugin;
    public TotemListener(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
