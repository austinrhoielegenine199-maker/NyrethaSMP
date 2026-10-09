package com.nyretha.tools;

/** Integration scaffold for the Drill custom tool; original behavior will be wired during main-class integration. */
public class Drill {
    protected final Tools plugin;
    public Drill(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
