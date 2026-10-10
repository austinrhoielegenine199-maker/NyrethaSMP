package com.nyretha.home.Manager;

import java.io.File;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import com.nyretha.home.Home;

/** Persistent storage for player homes, keyed by UUID and home name. */
public final class DataManager {
    private final File file;
    private final YamlConfiguration data;
    public DataManager(Home plugin) {
        file = new File(plugin.getDataFolder(), "homes.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }
    private String key(Player player, String name) { return player.getUniqueId() + "." + name.toLowerCase(); }
    public void setHome(Player player, String name, Location location) { data.set(key(player, name), location); save(); }
    public Location getHome(Player player, String name) { return data.getLocation(key(player, name)); }
    public boolean hasHome(Player player, String name) { return data.isLocation(key(player, name)); }
    public void deleteHome(Player player, String name) { data.set(key(player, name), null); save(); }
    public void save() { try { data.save(file); } catch (IOException ex) { throw new IllegalStateException("Unable to save homes.yml", ex); } }
}
