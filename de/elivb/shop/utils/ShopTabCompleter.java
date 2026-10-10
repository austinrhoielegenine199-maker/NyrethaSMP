package de.elivb.shop.utils;

import de.elivb.shop.Shop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

public class ShopTabCompleter implements TabCompleter {
   private static final List<String> COMMANDS = Arrays.asList("reload", "addhanditem", "removeitem", "addshop", "removeshop");
   private static final List<String> EMPTY_LIST = new ArrayList();
   private final Shop shop;

   public ShopTabCompleter(Shop shop) { this.shop = shop; }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (!command.getName().equalsIgnoreCase("shop")) return EMPTY_LIST;
      if (args.length == 1) {
         ArrayList<String> completions = new ArrayList();
         List<String> availableCommands = new ArrayList();
         if (sender.hasPermission("shop.reload")) availableCommands.add("reload");
         if (sender.hasPermission("shop.add")) availableCommands.add("addhanditem");
         if (sender.hasPermission("shop.remove")) availableCommands.add("removeitem");
         if (sender.hasPermission("shop.addshop")) availableCommands.add("addshop");
         if (sender.hasPermission("shop.removeshop")) availableCommands.add("removeshop");
         StringUtil.copyPartialMatches(args[0], availableCommands, completions);
         return completions;
      }
      if (args.length >= 2 && args[0].equalsIgnoreCase("addhanditem")) {
         if (!sender.hasPermission("shop.add")) return EMPTY_LIST;
         if (args.length == 2) {
            List<String> names = this.shop.getCategoryNames();
            ArrayList<String> completions = new ArrayList();
            StringUtil.copyPartialMatches(args[1], names, completions);
            return completions;
         }
         if (args.length == 3) return Arrays.asList("<price>");
         if (args.length == 4) return Arrays.asList("<slot>");
         if (args.length == 5) return Arrays.asList("<page>");
      }
      if (args.length >= 2 && args[0].equalsIgnoreCase("removeitem")) {
         if (!sender.hasPermission("shop.remove")) return EMPTY_LIST;
         if (args.length == 2) {
            List<String> names = this.shop.getCategoryNames();
            ArrayList<String> completions = new ArrayList();
            StringUtil.copyPartialMatches(args[1], names, completions);
            return completions;
         }
         if (args.length == 3) return Arrays.asList("<slot>");
         if (args.length == 4) return Arrays.asList("<page>");
      }
      if (args.length >= 2 && args[0].equalsIgnoreCase("addshop")) {
         if (!sender.hasPermission("shop.addshop")) return EMPTY_LIST;
         if (args.length == 2) return Arrays.asList("<name>");
         if (args.length == 3) return Arrays.asList("<slot>");
         if (args.length == 4) {
            List<String> materials = Arrays.stream(Material.values()).map(Enum::name).collect(Collectors.toList());
            ArrayList<String> completions = new ArrayList();
            StringUtil.copyPartialMatches(args[3].toUpperCase(), materials, completions);
            return completions;
         }
         if (args.length == 5) return Arrays.asList("<displayname>");
         if (args.length == 6) {
            List<String> names = this.shop.getCategoryNames();
            ArrayList<String> completions = new ArrayList();
            StringUtil.copyPartialMatches(args[5], names, completions);
            return completions;
         }
      }
      if (args.length >= 2 && args[0].equalsIgnoreCase("removeshop")) {
         if (!sender.hasPermission("shop.removeshop")) return EMPTY_LIST;
         if (args.length == 2) return Arrays.asList("<slot>");
      }
      return EMPTY_LIST;
   }
}