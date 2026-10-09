package com.nyretha.teams.Manager;

import com.nyretha.teams.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DataManager {
    private final Team plugin;
    private final String name;
    private String leader;
    private final Set<String> members = new LinkedHashSet<>();
    private final File file;
    private final YamlConfiguration data;
    public DataManager(Team plugin, String name, String leader) {
        this.plugin = plugin; this.name = name; this.leader = leader;
        this.file = new File(new File(plugin.getDataFolder(), "Teams"), name + ".yml");
        this.file.getParentFile().mkdirs();
        this.data = YamlConfiguration.loadConfiguration(file);
        if (leader != null && !leader.isBlank()) members.add(leader);
        save();
    }
    public DataManager(Team plugin, File file) {
        this.plugin = plugin; this.file = file; this.name = file.getName().replaceFirst("\\.yml$", "");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.leader = data.getString("leader", "");
        members.addAll(data.getStringList("members"));
    }
    public String getName() { return name; }
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; save(); }
    public Set<String> getMembers() { return new LinkedHashSet<>(members); }
    public boolean hasMember(String player) { return members.stream().anyMatch(n -> n.equalsIgnoreCase(player)); }
    public boolean addMember(String player) { boolean changed = members.add(player); if (changed) save(); return changed; }
    public boolean removeMember(String player) { boolean changed = members.removeIf(n -> n.equalsIgnoreCase(player)); if (changed) save(); return changed; }
    public boolean isLeader(String player) { return leader != null && leader.equalsIgnoreCase(player); }
    public void setHome(Location location) {
        data.set("home.world", location.getWorld().getName()); data.set("home.x", location.getX());
        data.set("home.y", location.getY()); data.set("home.z", location.getZ());
        data.set("home.yaw", location.getYaw()); data.set("home.pitch", location.getPitch()); save();
    }
    public Location getHome() {
        if (!data.contains("home.world")) return null;
        var world = Bukkit.getWorld(data.getString("home.world", ""));
        return world == null ? null : new Location(world, data.getDouble("home.x"), data.getDouble("home.y"),
            data.getDouble("home.z"), (float)data.getDouble("home.yaw"), (float)data.getDouble("home.pitch"));
    }
    public void save() {
        data.set("name", name); data.set("leader", leader); data.set("members", members.stream().toList());
        try { data.save(file); } catch (IOException ex) { plugin.getLogger().warning("Could not save team " + name + ": " + ex.getMessage()); }
    }
    public void deleteFile() { if (file.exists() && !file.delete()) plugin.getLogger().warning("Could not delete team file " + file); }
}
