package com.nyretha.teams;

import com.nyretha.teams.Manager.DataManager;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class PvPListener implements Listener {
    private final Team plugin;
    public PvPListener(Team plugin) { this.plugin = plugin; }
    @EventHandler public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = event.getDamager() instanceof Player p ? p : null;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) attacker = p;
        if (attacker == null) return;
        DataManager a = plugin.getTeamManager().getPlayerTeam(attacker.getName());
        DataManager v = plugin.getTeamManager().getPlayerTeam(victim.getName());
        if (a != null && v != null && a.getName().equalsIgnoreCase(v.getName()) && !a.getTeamPvpEnabled())
            event.setCancelled(true);
    }
}
