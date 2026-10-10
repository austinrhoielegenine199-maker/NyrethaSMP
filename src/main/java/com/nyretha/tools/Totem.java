package com.nyretha.tools;

/** Integration scaffold for the Totem custom tool; original behavior will be wired during main-class integration. */
public class Totem {
    protected final Tools plugin;
    public Totem(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
