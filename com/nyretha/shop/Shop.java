package com.nyretha.shop;

import com.nyretha.shop.models.ShopCategory;
import com.nyretha.shop.utils.HexColor;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for the Nyretha Shop module.
 *
 * Categories are loaded from the plugin's config.yml:
 * categories.<key>.name, material, slot, action, command, lore.
 * A category's command is run as the player after they click its icon.
 */
public final class Shop extends JavaPlugin implements Listener, TabExecutor {
    private final Map<Integer, ShopCategory> categoriesBySlot = new HashMap<>();
    private String menuTitle = "&#A303F9Nyretha Shop";
    private int menuRows = 3;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadCategories();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(this);
            getCommand("shop").setTabCompleter(this);
        } else {
            getLogger().warning("The 'shop' command is missing from plugin.yml.");
        }
    }

    public void reloadShop() {
        reloadConfig();
        loadCategories();
    }

    private void loadCategories() {
        categoriesBySlot.clear();
        menuTitle = HexColor.translateHexCodes(getConfig().getString("menu.title", "&#A303F9Nyretha Shop"));
        menuRows = Math.max(1, Math.min(6, getConfig().getInt("menu.rows", 3)));
        ConfigurationSection section = getConfig().getConfigurationSection("categories");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "categories." + key + ".";
            Material material;
            try {
                material = Material.valueOf(getConfig().getString(path + "material", "CHEST").toUpperCase());
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Invalid material for shop category '" + key + "'. Skipping it.");
                continue;
            }

            int slot = getConfig().getInt(path + "slot", -1);
            if (slot < 0 || slot >= menuRows * 9) {
                getLogger().warning("Invalid slot for shop category '" + key + "'. Skipping it.");
                continue;
            }
            ShopCategory category = new ShopCategory(
                getConfig().getString(path + "name", key), material, slot,
                getConfig().getString(path + "action", key));
            category.setCommand(getConfig().getString(path + "command", ""));
            category.setLore(getConfig().getStringList(path + "lore"));
            categoriesBySlot.put(slot, category);
        }
    }

    public void openShop(Player player) {
        Inventory inventory = Bukkit.createInventory(null, menuRows * 9, menuTitle);
        for (Map.Entry<Integer, ShopCategory> entry : categoriesBySlot.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getDisplayItem());
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!menuTitle.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        ShopCategory category = categoriesBySlot.get(slot);
        if (category == null) return;
        Player player = (Player) event.getWhoClicked();
        String command = category.getCommand();
        if (command == null || command.trim().isEmpty()) return;
        command = command.replace("%player%", player.getName()).trim();
        if (command.startsWith("/")) command = command.substring(1);
        player.closeInventory();
        player.performCommand(command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("shop")) return false;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("shop.reload")) {
                sender.sendMessage(HexColor.translateHexCodes("&cYou don't have permission."));
                return true;
            }
            reloadShop();
            sender.sendMessage(HexColor.translateHexCodes("&aShop configuration reloaded."));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        openShop((Player) sender);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("shop.reload")) {
            String option = "reload";
            return option.startsWith(args[0].toLowerCase()) ? java.util.Collections.singletonList(option)
                : java.util.Collections.emptyList();
        }
        return java.util.Collections.emptyList();
    }
}
