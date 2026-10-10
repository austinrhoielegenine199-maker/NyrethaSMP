package com.nyretha.shop.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CategoryInventory {
   private String categoryName;
   private List<ShopItem> items = new ArrayList();
   private int totalPages = 1;
   private int rows = 3;
   private Map<String, Object> pageIcons = new HashMap();
   private int previousPageSlot = 19;
   private int nextPageSlot = 26;
   private ItemStack previousPageItem = null;
   private ItemStack nextPageItem = null;
   private boolean premium = false;
   private String permission = "";

   public CategoryInventory(String categoryName) { this.categoryName = categoryName; }
   public void addItem(ShopItem item) { this.items.add(item); }
   public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
   public int getTotalPages() { return this.totalPages; }
   public void setRows(int rows) { this.rows = rows; }
   public int getRows() { return this.rows; }
   public void setPreviousPageSlot(int slot) { this.previousPageSlot = slot; }
   public int getPreviousPageSlot() { return this.previousPageSlot; }
   public void setNextPageSlot(int slot) { this.nextPageSlot = slot; }
   public int getNextPageSlot() { return this.nextPageSlot; }
   public void setPreviousPageItem(ItemStack item) { this.previousPageItem = item; }
   public ItemStack getPreviousPageItem() { return this.previousPageItem; }
   public void setNextPageItem(ItemStack item) { this.nextPageItem = item; }
   public ItemStack getNextPageItem() { return this.nextPageItem; }

   public void populateInventory(Inventory inventory, int page) {
      inventory.clear();
      int maxSlots = this.rows * 9;
      for(ShopItem item : this.items) {
         if (item.getPage() == page) {
            int slot = item.getSlot();
            if (slot >= 0 && slot < maxSlots) inventory.setItem(slot, item.getItemStack());
         }
      }
      if (page > 1 && this.previousPageItem != null && this.previousPageSlot >= 0 && this.previousPageSlot < maxSlots) inventory.setItem(this.previousPageSlot, this.previousPageItem);
      if (page < this.totalPages && this.nextPageItem != null && this.nextPageSlot >= 0 && this.nextPageSlot < maxSlots) inventory.setItem(this.nextPageSlot, this.nextPageItem);
   }

   public ShopItem getItem(ItemStack itemStack) {
      if (itemStack != null && itemStack.hasItemMeta()) {
         for(ShopItem item : this.items) {
            ItemStack shopItemStack = item.getItemStack();
            if (shopItemStack.isSimilar(itemStack)) return item;
         }
         return null;
      }
      return null;
   }

   public String getCategoryName() { return this.categoryName; }
   public List<ShopItem> getItems() { return new ArrayList(this.items); }
   public int getItemCount() { return this.items.size(); }
   public boolean isEmpty() { return this.items.isEmpty(); }
   public void clearItems() { this.items.clear(); }
   public boolean containsItem(ShopItem shopItem) { return this.items.contains(shopItem); }
   public boolean removeItem(ShopItem shopItem) { return this.items.remove(shopItem); }
   public ShopItem getItemAtIndex(int index) { return index >= 0 && index < this.items.size() ? this.items.get(index) : null; }
   public void add(ShopItem shopItem) { if (shopItem != null) this.items.add(shopItem); }
   public boolean isPremium() { return this.premium; }
   public void setPremium(boolean premium) { this.premium = premium; }
   public String getPermission() { return this.permission; }
   public void setPermission(String permission) { this.permission = permission; }
}