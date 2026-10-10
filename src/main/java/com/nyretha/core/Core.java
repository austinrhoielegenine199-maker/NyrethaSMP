package com.nyretha.core;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Single Bukkit entry point for NyrethaSMP.
 *
 * Module startup must be performed through Core-owned module adapters; Bukkit
 * does not load multiple JavaPlugin main classes from a single plugin.yml.
 */
public final class Core extends JavaPlugin {
    private static Core instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("NyrethaSMP Core starting.");
        getLogger().warning("Legacy modules still need their standalone JavaPlugin lifecycle converted to Core module adapters before their features can be enabled from this single JAR.");
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static Core getInstance() {
        return instance;
    }
}
