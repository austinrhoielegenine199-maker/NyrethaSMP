package com.nyretha.teams.Listener;

import com.nyretha.teams.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final Team plugin;
    public PlayerJoinListener(Team plugin) { this.plugin = plugin; }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        // Team membership is restored from each team's persistent YAML file.
    }
}