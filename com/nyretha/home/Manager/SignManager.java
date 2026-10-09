package com.nyretha.home.Manager;

import org.bukkit.entity.Player;

/** Optional sign-input adapter; deliberately has no third-party SignAPI dependency. */
public final class SignManager {
    public boolean isAvailable() { return false; }
    public void requestInput(Player player) {
        if (player != null) player.sendMessage("Sign input is not configured on this server.");
    }
}
