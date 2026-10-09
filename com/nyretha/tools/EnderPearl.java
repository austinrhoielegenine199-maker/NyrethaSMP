package com.nyretha.tools;

/** Integration scaffold for the EnderPearl custom tool; original behavior will be wired during main-class integration. */
public class EnderPearl {
    protected final Tools plugin;
    public EnderPearl(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
