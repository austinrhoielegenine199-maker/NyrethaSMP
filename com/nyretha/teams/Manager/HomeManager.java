package com.nyretha.teams.Manager;

import com.nyretha.teams.Team;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class HomeManager {
    private final Team plugin;
    public HomeManager(Team plugin) { this.plugin = plugin; }
    public Location getHome(Player player) {
        DataManager team = plugin.getTeamManager().getPlayerTeam(player.getName());
        return team == null ? null : team.getHome();
    }
    public boolean setHome(Player player) {
        DataManager team = plugin.getTeamManager().getPlayerTeam(player.getName());
        if (team == null || (!team.isLeader(player.getName()) && !player.hasPermission("team.admin"))) return false;
        team.setHome(player.getLocation()); return true;
    }
}