package com.nyretha.teams;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public final class HexUtils {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private HexUtils() {}
    public static String colorize(String text) {
        if (text == null) return "";
        Matcher m = HEX.matcher(text);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            try { m.appendReplacement(out, ChatColor.of("#" + m.group(1)).toString()); }
            catch (IllegalArgumentException ex) { m.appendReplacement(out, m.group()); }
        }
        m.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }
    public static String removeColor(String text) {
        return text == null ? "" : text.replaceAll("(?i)&#[0-9a-f]{6}|[&§][0-9a-fk-or]", "");
    }
}
