package com.nyretha.team.manager;

import com.nyretha.team.Team;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Public API for Homes and other optional integrations. */
public final class TeamHomeApi {
    private final Team plugin;
    public TeamHomeApi(Team plugin) { this.plugin = plugin; }
    public boolean hasTeam(Player player) { return plugin.getPlayerTeamName(player) != null; }
    public Location getTeamHome(Player player) { return plugin.getTeamHome(player); }
    public boolean hasTeamHome(Player player) { Location l=getTeamHome(player); return l!=null && l.getWorld()!=null; }
    public boolean teleportToTeamHome(Player player) { Location l=getTeamHome(player); return l!=null && player.teleport(l); }
}