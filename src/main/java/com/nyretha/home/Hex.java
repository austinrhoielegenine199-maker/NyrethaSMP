package com.nyretha.home;

import org.bukkit.ChatColor;

/** Small formatting helper shared by the Homes module. */
public final class Hex {
    private Hex() {}
    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
