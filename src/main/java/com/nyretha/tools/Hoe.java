package com.nyretha.tools;

/** Integration scaffold for the Hoe custom tool; original behavior will be wired during main-class integration. */
public class Hoe {
    protected final Tools plugin;
    public Hoe(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
