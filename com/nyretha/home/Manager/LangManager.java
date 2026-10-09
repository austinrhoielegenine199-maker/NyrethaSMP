package com.nyretha.home.Manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import com.nyretha.home.Home;

/** Loads player-facing messages from lang.yml. */
public final class LangManager {
    private final YamlConfiguration messages;
    public LangManager(Home plugin) {
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) plugin.saveResource("lang.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
    }
    public String get(String key) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(key, key));
    }
}
