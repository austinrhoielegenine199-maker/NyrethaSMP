package com.nyretha.teams;

import com.nyretha.teams.Manager.DataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class TeamChatListener implements Listener {
    private final Team plugin;
    public TeamChatListener(Team plugin) { this.plugin = plugin; }
    @EventHandler public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isTeamChatEnabled(player)) return;
        event.setCancelled(true);
        DataManager team = plugin.getTeamManager().getPlayerTeam(player.getName());
        if (team != null) for (String member : team.getMembers()) {
            Player target = plugin.getServer().getPlayerExact(member);
            if (target != null) target.sendMessage(HexUtils.colorize("&a[Team] &f" + player.getName() + ": " + event.getMessage()));
        }
    }
}
