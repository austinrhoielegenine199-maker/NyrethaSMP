package com.nyretha.shop.utils;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class HexColor {
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
   private static final Pattern GRADIENT_PATTERN = Pattern.compile("&#gradient\\(([0-9a-fA-F]{6}):([0-9a-fA-F]{6})\\)(.+?)&#end-gradient");

   public static String translateHexCodes(String text) {
      if (text == null) return "";
      text = ChatColor.translateAlternateColorCodes('&', text);
      Matcher hexMatcher = HEX_PATTERN.matcher(text);
      StringBuffer buffer = new StringBuffer();
      while(hexMatcher.find()) {
         String hexCode = hexMatcher.group(1);
         String replacement = ChatColor.of("#" + hexCode).toString();
         hexMatcher.appendReplacement(buffer, replacement);
      }
      hexMatcher.appendTail(buffer);
      text = translateGradients(buffer.toString());
      return text;
   }

   private static String translateGradients(String text) {
      Matcher gradientMatcher = GRADIENT_PATTERN.matcher(text);
      StringBuffer buffer = new StringBuffer();
      while(gradientMatcher.find()) {
         String gradientText = applyGradient(gradientMatcher.group(3), gradientMatcher.group(1), gradientMatcher.group(2));
         gradientMatcher.appendReplacement(buffer, gradientText);
      }
      gradientMatcher.appendTail(buffer);
      return buffer.toString();
   }

   private static String applyGradient(String text, String startHex, String endHex) {
      if (text.length() == 0) return text;
      Color startColor = Color.decode("#" + startHex);
      Color endColor = Color.decode("#" + endHex);
      StringBuilder result = new StringBuilder();
      int length = text.length();
      for(int i = 0; i < length; ++i) {
         float ratio = (float)i / (float)Math.max(1, length - 1);
         Color interpolated = interpolateColor(startColor, endColor, ratio);
         String hex = String.format("#%02x%02x%02x", interpolated.getRed(), interpolated.getGreen(), interpolated.getBlue());
         result.append(ChatColor.of(hex)).append(text.charAt(i));
      }
      return result.toString();
   }

   private static Color interpolateColor(Color start, Color end, float ratio) {
      int red = (int)(start.getRed() + ratio * (end.getRed() - start.getRed()));
      int green = (int)(start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
      int blue = (int)(start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
      red = Math.max(0, Math.min(255, red));
      green = Math.max(0, Math.min(255, green));
      blue = Math.max(0, Math.min(255, blue));
      return new Color(red, green, blue);
   }
}