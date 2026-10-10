package com.nyretha.tools;

/** Expiration-checking service scaffold; scheduling is connected in the main plugin class. */
public class ToolExpiryChecker {
    private final Tools plugin;
    public ToolExpiryChecker(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
