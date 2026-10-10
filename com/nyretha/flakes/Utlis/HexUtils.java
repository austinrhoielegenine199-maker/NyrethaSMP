package com.nyretha.flakes.Utlis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public final class HexUtils {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private HexUtils() {}

    public static String colorize(String message) {
        if (message == null) return "";
        Matcher matcher = HEX.matcher(message);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String replacement;
            try { replacement = ChatColor.of("#" + matcher.group(1)).toString(); }
            catch (IllegalArgumentException ex) { replacement = matcher.group(); }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }
}