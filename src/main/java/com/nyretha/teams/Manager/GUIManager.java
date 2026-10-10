package com.nyretha.teams.Manager;

import com.nyretha.teams.GUI.MemberManagerGUI;
import com.nyretha.teams.GUI.TeamGUI;
import com.nyretha.teams.Team;
import org.bukkit.entity.Player;

public final class GUIManager {
    private final TeamGUI teamGUI;
    private final MemberManagerGUI memberGUI;
    public GUIManager(Team plugin) { teamGUI = new TeamGUI(plugin); memberGUI = new MemberManagerGUI(plugin); }
    public void openTeam(Player player) { teamGUI.open(player); }
    public void openMembers(Player player) { memberGUI.open(player); }
}