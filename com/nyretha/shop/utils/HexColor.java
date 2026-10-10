package com.nyretha.shop.utils;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

/** Translates legacy, hex, and simple gradient color codes used by Shop menus. */
public final class HexColor {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern GRADIENT_PATTERN =
        Pattern.compile("&#gradient\\(([0-9a-fA-F]{6}):([0-9a-fA-F]{6})\\)(.+?)&#end-gradient");

    private HexColor() {}

    public static String translateHexCodes(String text) {
        if (text == null) return "";
        Matcher hexMatcher = HEX_PATTERN.matcher(ChatColor.translateAlternateColorCodes('&', text));
        StringBuffer buffer = new StringBuffer();
        while (hexMatcher.find()) {
            String replacement;
            try {
                replacement = ChatColor.of("#" + hexMatcher.group(1)).toString();
            } catch (IllegalArgumentException ex) {
                replacement = hexMatcher.group(0);
            }
            hexMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        hexMatcher.appendTail(buffer);
        return translateGradients(buffer.toString());
    }

    private static String translateGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = applyGradient(matcher.group(3), matcher.group(1), matcher.group(2));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String applyGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return text;
        Color start = Color.decode("#" + startHex);
        Color end = Color.decode("#" + endHex);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / Math.max(1, text.length() - 1);
            int red = Math.round(start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = Math.round(start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = Math.round(start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            result.append(ChatColor.of(String.format("#%02x%02x%02x", red, green, blue)))
                  .append(text.charAt(i));
        }
        return result.toString();
    }
}
