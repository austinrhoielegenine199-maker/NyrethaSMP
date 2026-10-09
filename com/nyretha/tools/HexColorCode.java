package com.nyretha.tools;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Legacy hex color helper for ampersand and &#RRGGBB color codes. */
public final class HexColorCode {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private HexColorCode() {}
    public static String translate(String input) {
        if (input == null || input.isEmpty()) return input;
        Matcher matcher = HEX.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }
}
