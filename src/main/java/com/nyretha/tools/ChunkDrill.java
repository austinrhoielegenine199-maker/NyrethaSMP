package com.nyretha.tools;

/** Integration scaffold for the ChunkDrill custom tool; original behavior will be wired during main-class integration. */
public class ChunkDrill {
    protected final Tools plugin;
    public ChunkDrill(Tools plugin) { this.plugin = plugin; }
    public Tools getPlugin() { return plugin; }
}
