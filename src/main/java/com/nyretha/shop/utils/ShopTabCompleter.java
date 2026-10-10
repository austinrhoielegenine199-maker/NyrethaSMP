package com.nyretha.shop.utils;

import com.nyretha.shop.Shop;
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
    private static final List<String> EMPTY = new ArrayList<>();
    private final Shop shop;
    public ShopTabCompleter(Shop shop) { this.shop = shop; }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("shop")) return EMPTY;
        if (args.length == 1) {
            List<String> choices = new ArrayList<>();
            if (sender.hasPermission("shop.reload")) choices.add("reload");
            if (sender.hasPermission("shop.add")) choices.add("addhanditem");
            if (sender.hasPermission("shop.remove")) choices.add("removeitem");
            if (sender.hasPermission("shop.addshop")) choices.add("addshop");
            if (sender.hasPermission("shop.removeshop")) choices.add("removeshop");
            List<String> matches = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], choices, matches);
            return matches;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("addhanditem") || args[0].equalsIgnoreCase("removeitem"))) {
            List<String> matches = new ArrayList<>();
            StringUtil.copyPartialMatches(args[1], shop.getCategoryNames(), matches);
            return matches;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("addhanditem")) return Arrays.asList("<price>");
        if (args.length == 4 && args[0].equalsIgnoreCase("addhanditem")) return Arrays.asList("<slot>");
        if (args.length == 5 && args[0].equalsIgnoreCase("addhanditem")) return Arrays.asList("<page>");
        if (args.length == 2 && args[0].equalsIgnoreCase("addshop")) return Arrays.asList("<name>");
        if (args.length == 3 && args[0].equalsIgnoreCase("addshop")) return Arrays.asList("<slot>");
        if (args.length == 4 && args[0].equalsIgnoreCase("addshop")) return Arrays.stream(Material.values()).map(Enum::name).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("removeshop")) return Arrays.asList("<slot>");
        return EMPTY;
    }
}