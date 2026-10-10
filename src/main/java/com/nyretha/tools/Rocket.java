package com.nyretha.tools;

/** Integration scaffold for the Rocket custom tool; original behavior will be wired during main-class integration. */
public class Rocket {
    protected final Tools plugin;
    public Rocket(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
