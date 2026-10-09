package com.nyretha.teams.Manager;

import com.nyretha.teams.Team;
import java.io.File;
import java.util.*;

public final class TeamManager {
    private final Team plugin;
    private final Map<String, DataManager> teams = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> invites = new HashMap<>();
    public TeamManager(Team plugin) { this.plugin = plugin; }
    public void loadAllTeams() {
        File dir = new File(plugin.getDataFolder(), "Teams");
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) { DataManager team = new DataManager(plugin, f); teams.put(team.getName(), team); }
    }
    public Collection<DataManager> getTeams() { return Collections.unmodifiableCollection(teams.values()); }
    public DataManager getTeam(String name) { return teams.get(name); }
    public DataManager getPlayerTeam(String player) {
        return teams.values().stream().filter(t -> t.hasMember(player)).findFirst().orElse(null);
    }
    public boolean create(String name, String leader) {
        if (teams.containsKey(name) || getPlayerTeam(leader) != null) return false;
        DataManager t = new DataManager(plugin, name, leader); teams.put(name, t); return true;
    }
    public void remove(String name) { DataManager t = teams.remove(name); if (t != null) t.deleteFile(); }
    public void invite(String player, String team) { invites.put(player.toLowerCase(Locale.ROOT), team); }
    public String getInvite(String player) { return invites.get(player.toLowerCase(Locale.ROOT)); }
    public void clearInvite(String player) { invites.remove(player.toLowerCase(Locale.ROOT)); }
}
