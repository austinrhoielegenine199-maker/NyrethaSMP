package com.nyretha.shop.models;

import com.nyretha.shop.utils.HexColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** A configurable category entry shown in the main Shop inventory. */
public class ShopCategory {
    private final String displayName;
    private final Material material;
    private final int slot;
    private final String action;
    private String command;
    private List<String> lore = new ArrayList<>();
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();

    public ShopCategory(String displayName, Material material, int slot, String action) {
        this.displayName = HexColor.translateHexCodes(displayName);
        this.material = material;
        this.slot = slot;
        this.action = action;
    }

    public void setCommand(String command) { this.command = command; }
    public String getCommand() { return command; }
    public void addLoreLine(String line) { lore.add(HexColor.translateHexCodes(line)); }

    public void setLore(List<String> lore) {
        this.lore = new ArrayList<>();
        if (lore != null) for (String line : lore) this.lore.add(HexColor.translateHexCodes(line));
    }

    public void addEnchantment(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) enchantments.put(enchantment, level);
    }

    public ItemStack getDisplayItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(displayName);
        if (!lore.isEmpty()) meta.setLore(new ArrayList<>(lore));
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        if (!enchantments.isEmpty()) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getSlot() { return slot; }
    public String getAction() { return action; }
    public void addLine(String line) { addLoreLine(line); }
}
