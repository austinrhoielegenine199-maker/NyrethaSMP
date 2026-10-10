package com.nyretha.flakes.Utlis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class HexUtils {
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

   public static String colorize(String message) {
      if (message == null) {
         return "";
      } else {
         try {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer buffer = new StringBuffer();

            while(matcher.find()) {
               String hexColor = matcher.group(1);

               try {
                  ChatColor color = ChatColor.of("#" + hexColor);
                  matcher.appendReplacement(buffer, color.toString());
               } catch (IllegalArgumentException var5) {
                  matcher.appendReplacement(buffer, matcher.group(0));
               }
            }

            matcher.appendTail(buffer);
            return ChatColor.translateAlternateColorCodes('&', buffer.toString());
         } catch (Exception var6) {
            return ChatColor.translateAlternateColorCodes('&', message);
         }
      }
   }
}