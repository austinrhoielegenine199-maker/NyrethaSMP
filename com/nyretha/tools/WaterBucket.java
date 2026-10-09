package com.nyretha.tools;

/** Integration scaffold for the WaterBucket custom tool; original behavior will be wired during main-class integration. */
public class WaterBucket {
    protected final Tools plugin;
    public WaterBucket(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
