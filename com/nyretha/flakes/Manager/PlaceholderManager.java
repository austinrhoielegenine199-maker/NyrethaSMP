package com.nyretha.flakes.Manager;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderManager {
    private static final Pattern PATTERN = Pattern.compile("%(\\w+)%");

    public static String replacePlaceholders(String text, Player player, int flakes) {
        if (text == null) return null;
        Map<String,String> values = new HashMap<>();
        values.put("player", player == null ? "" : player.getName());
        values.put("displayname", player == null ? "" : player.getDisplayName());
        values.put("flakes", String.valueOf(flakes));
        values.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        values.put("max_players", String.valueOf(Bukkit.getMaxPlayers()));
        if (player != null) {
            values.put("health", String.valueOf((int) player.getHealth()));
            values.put("max_health", String.valueOf((int) player.getMaxHealth()));
            values.put("level", String.valueOf(player.getLevel()));
            values.put("world", player.getWorld().getName());
            values.put("ping", String.valueOf(player.getPing()));
        }
        return replace(text, values);
    }

    public static String replacePlaceholders(String text, Map<String,String> values) { return replace(text, values); }

    private static String replace(String text, Map<String,String> values) {
        if (text == null) return null;
        Matcher matcher = PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1).toLowerCase(), matcher.group());
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}