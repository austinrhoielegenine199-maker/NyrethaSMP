package com.nyretha;

import org.bukkit.plugin.java.JavaPlugin;

/** Single Bukkit entry point for NyrethaSMP. */
public final class Core extends JavaPlugin {
    private static Core instance;

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("NyrethaSMP Core starting.");
        getLogger().warning("Module lifecycle migration is still in progress; legacy modules are not yet enabled by Core.");
    }
    @Override public void onDisable() { instance = null; }
    public static Core getInstance() { return instance; }
}
