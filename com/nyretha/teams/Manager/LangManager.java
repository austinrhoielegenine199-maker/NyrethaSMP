package com.nyretha.teams.Manager;

import com.nyretha.teams.HexUtils;
import com.nyretha.teams.Team;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public final class LangManager {
    private final Team plugin;
    private YamlConfiguration messages;
    public LangManager(Team plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) plugin.saveResource("lang.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
    }
    public String getMessage(String path) {
        return HexUtils.colorize(messages.getString(path, path));
    }
    public String getMessage(String path, String... replacements) {
        String value = messages.getString(path, path);
        for (int i = 0; i + 1 < replacements.length; i += 2)
            value = value.replace(replacements[i], replacements[i + 1]);
        return HexUtils.colorize(value);
    }
}
