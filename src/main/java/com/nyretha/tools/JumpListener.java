package com.nyretha.tools;

import org.bukkit.event.Listener;

/** Integration scaffold for the JumpListener event handler; behavior will be ported during main-class integration. */
public class JumpListener implements Listener {
    private final Tools plugin;
    public JumpListener(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
