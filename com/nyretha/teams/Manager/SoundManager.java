package com.nyretha.teams.Manager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
public final class SoundManager {
    private SoundManager() {}
    public static void play(Player player, String soundName) {
        if (player == null || soundName == null) return;
        try { player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1f, 1f); }
        catch (IllegalArgumentException ignored) {}
    }
}
