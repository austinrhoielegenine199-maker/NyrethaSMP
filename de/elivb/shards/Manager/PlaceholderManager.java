package de.elivb.shards.Manager;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderManager {
   private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\w+)%");

   public static String replacePlaceholders(String text, Player player, int gems) {
      if (text == null) {
         return null;
      } else {
         HashMap<String, String> placeholders = new HashMap();
         placeholders.put("player", player != null ? player.getName() : "");
         placeholders.put("displayname", player != null ? player.getDisplayName() : "");
         placeholders.put("gems", String.valueOf(gems));
         placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
         placeholders.put("max_players", String.valueOf(Bukkit.getMaxPlayers()));
         if (player != null) {
            placeholders.put("health", String.valueOf((int)player.getHealth()));
            placeholders.put("max_health", String.valueOf((int)player.getMaxHealth()));
            placeholders.put("level", String.valueOf(player.getLevel()));
            placeholders.put("world", player.getWorld().getName());
            placeholders.put("ping", String.valueOf(player.getPing()));
         }

         Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
         StringBuffer result = new StringBuffer();

         while(matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String replacement = placeholders.getOrDefault(placeholder, matcher.group());
            matcher.appendReplacement(result, replacement);
         }

         matcher.appendTail(result);
         return result.toString();
      }
   }

   public static String replacePlaceholders(String text, Map<String, String> customPlaceholders) {
      if (text == null) {
         return null;
      } else {
         Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
         StringBuffer result = new StringBuffer();

         while(matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String replacement = customPlaceholders.getOrDefault(placeholder, matcher.group());
            matcher.appendReplacement(result, replacement);
         }

         matcher.appendTail(result);
         return result.toString();
      }
   }

   public static String replaceGemPlaceholders(String text, Player player, int gems, String targetPlayer, int gemsSend, int gemsReceived, int gemsTaken, int gemsGiven, int gemsSet) {
      if (text == null) {
         return null;
      } else {
         HashMap<String, String> placeholders = new HashMap();
         placeholders.put("player", player != null ? player.getName() : "");
         placeholders.put("target_player", targetPlayer != null ? targetPlayer : "");
         placeholders.put("shards", String.valueOf(gems));
         placeholders.put("shards_send", String.valueOf(gemsSend));
         placeholders.put("shards_received", String.valueOf(gemsReceived));
         placeholders.put("shards_taken", String.valueOf(gemsTaken));
         placeholders.put("shards_given", String.valueOf(gemsGiven));
         placeholders.put("shards_set", String.valueOf(gemsSet));
         placeholders.put("displayname", player != null ? player.getDisplayName() : "");
         placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
         placeholders.put("max_players", String.valueOf(Bukkit.getMaxPlayers()));
         if (player != null) {
            placeholders.put("health", String.valueOf((int)player.getHealth()));
            placeholders.put("max_health", String.valueOf((int)player.getMaxHealth()));
            placeholders.put("level", String.valueOf(player.getLevel()));
            placeholders.put("world", player.getWorld().getName());
            placeholders.put("ping", String.valueOf(player.getPing()));
         }

         Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
         StringBuffer result = new StringBuffer();

         while(matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String replacement = placeholders.getOrDefault(placeholder, matcher.group());
            matcher.appendReplacement(result, replacement);
         }

         matcher.appendTail(result);
         return result.toString();
      }
   }

   public static String replaceGemPlaceholders(String text, Player player, int gems, String targetPlayer, int gemsSend, int gemsReceived) {
      return replaceGemPlaceholders(text, player, gems, targetPlayer, gemsSend, gemsReceived, 0, 0, 0);
   }

   public static String replaceGemPlaceholders(String text, Player player, int gems, String targetPlayer, int gemsSend, int gemsReceived, int gemsTaken) {
      return replaceGemPlaceholders(text, player, gems, targetPlayer, gemsSend, gemsReceived, gemsTaken, 0, 0);
   }

   public static String replaceGemPlaceholders(String text, Player player, int gems, String targetPlayer, int gemsSend, int gemsReceived, int gemsTaken, int gemsGiven) {
      return replaceGemPlaceholders(text, player, gems, targetPlayer, gemsSend, gemsReceived, gemsTaken, gemsGiven, 0);
   }
}