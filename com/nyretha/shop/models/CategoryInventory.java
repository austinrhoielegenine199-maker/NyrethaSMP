package com.nyretha.shop.models;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CategoryInventory {
    private final String categoryName;
    private final List<ShopItem> items = new ArrayList<>();
    private int totalPages = 1;
    private int rows = 3;
    private int previousPageSlot = 19;
    private int nextPageSlot = 26;
    private ItemStack previousPageItem;
    private ItemStack nextPageItem;

    public CategoryInventory(String categoryName) { this.categoryName = categoryName; }
    public void addItem(ShopItem item) { if (item != null) items.add(item); }
    public void setTotalPages(int pages) { totalPages = Math.max(1, pages); }
    public int getTotalPages() { return totalPages; }
    public void setRows(int rows) { this.rows = Math.max(1, Math.min(6, rows)); }
    public int getRows() { return rows; }
    public void setPreviousPageSlot(int slot) { previousPageSlot = slot; }
    public int getPreviousPageSlot() { return previousPageSlot; }
    public void setNextPageSlot(int slot) { nextPageSlot = slot; }
    public int getNextPageSlot() { return nextPageSlot; }
    public void setPreviousPageItem(ItemStack item) { previousPageItem = item; }
    public ItemStack getPreviousPageItem() { return previousPageItem; }
    public void setNextPageItem(ItemStack item) { nextPageItem = item; }
    public ItemStack getNextPageItem() { return nextPageItem; }

    public void populateInventory(Inventory inventory, int page) {
        inventory.clear();
        int maxSlots = rows * 9;
        for (ShopItem item : items) {
            if (item.getPage() == page) {
                int slot = item.getSlot();
                if (slot >= 0 && slot < maxSlots) inventory.setItem(slot, item.getItemStack());
            }
        }
        if (page > 1 && previousPageItem != null && previousPageSlot >= 0 && previousPageSlot < maxSlots)
            inventory.setItem(previousPageSlot, previousPageItem);
        if (page < totalPages && nextPageItem != null && nextPageSlot >= 0 && nextPageSlot < maxSlots)
            inventory.setItem(nextPageSlot, nextPageItem);
    }
    public String getCategoryName() { return categoryName; }
    public List<ShopItem> getItems() { return new ArrayList<>(items); }
    public int getItemCount() { return items.size(); }
    public boolean isEmpty() { return items.isEmpty(); }
    public void clearItems() { items.clear(); }
    public boolean containsItem(ShopItem item) { return items.contains(item); }
    public boolean removeItem(ShopItem item) { return items.remove(item); }
    public ShopItem getItemAtIndex(int index) { return index >= 0 && index < items.size() ? items.get(index) : null; }
    public boolean isPremium() { return false; }
    public void setPremium(boolean premium) { }
    public String getPermission() { return ""; }
    public void setPermission(String permission) { }
}