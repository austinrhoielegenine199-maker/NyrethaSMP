package com.nyretha.flakes.Utlis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

/** Color formatting helper used by the Flakes plugin. */
public final class HexUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private HexUtils() {}

    public static String colorize(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            try {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(
                    ChatColor.of("#" + matcher.group(1)).toString()));
            } catch (IllegalArgumentException ex) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
