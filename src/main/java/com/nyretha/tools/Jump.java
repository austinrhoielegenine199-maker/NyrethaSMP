package com.nyretha.tools;

/** Integration scaffold for the Jump custom tool; original behavior will be wired during main-class integration. */
public class Jump {
    protected final Tools plugin;
    public Jump(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
