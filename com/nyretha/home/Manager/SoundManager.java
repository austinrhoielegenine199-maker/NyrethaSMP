package com.nyretha.home.Manager;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundManager {
    private SoundManager() {}
    public static void play(Player player, Sound sound) {
        if (player != null && sound != null) player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
}
