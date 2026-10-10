package com.nyretha.shop.models;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** A configured item shown in a shop category. */
public final class ShopItem {
    private final ItemStack itemStack;
    private final double price;
    private final int slot;
    private int page = 1;

    public ShopItem(Material material, double price, int amount, String displayName, List<String> lore, int slot) {
        this.itemStack = new ItemStack(material == null ? Material.STONE : material, Math.max(1, amount));
        this.price = price;
        this.slot = slot;
        var meta = itemStack.getItemMeta();
        if (meta != null) {
            if (displayName != null) meta.setDisplayName(displayName);
            if (lore != null) meta.setLore(new ArrayList<>(lore));
            itemStack.setItemMeta(meta);
        }
    }
    public ItemStack getItemStack() { return itemStack.clone(); }
    public double getPrice() { return price; }
    public int getSlot() { return slot; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
}
