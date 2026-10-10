package com.nyretha.tools;

/** Integration scaffold for the Shovel custom tool; original behavior will be wired during main-class integration. */
public class Shovel {
    protected final Tools plugin;
    public Shovel(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
