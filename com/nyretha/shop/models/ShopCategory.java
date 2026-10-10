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

public class ShopCategory {
   private String displayName;
   private Material material;
   private int slot;
   private String action;
   private String command;
   private List<String> lore = new ArrayList();
   private Map<Enchantment, Integer> enchantments = new HashMap();

   public ShopCategory(String displayName, Material material, int slot, String action) {
      this.displayName = HexColor.translateHexCodes(displayName);
      this.material = material;
      this.slot = slot;
      this.action = action;
      this.command = null;
   }

   public void setCommand(String command) { this.command = command; }
   public String getCommand() { return this.command; }
   public void addLoreLine(String line) { this.lore.add(HexColor.translateHexCodes(line)); }

   public void setLore(List<String> lore) {
      this.lore = new ArrayList();
      for(String line : lore) this.lore.add(HexColor.translateHexCodes(line));
   }

   public void addEnchantment(Enchantment enchantment, int level) { this.enchantments.put(enchantment, level); }

   public ItemStack getDisplayItem() {
      ItemStack item = new ItemStack(this.material);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(this.displayName);
      if (this.lore != null && !this.lore.isEmpty()) meta.setLore(this.lore);
      if (!this.enchantments.isEmpty()) {
         for(Map.Entry<Enchantment, Integer> entry : this.enchantments.entrySet()) meta.addEnchant(entry.getKey(), entry.getValue(), true);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
      }
      item.setItemMeta(meta);
      return item;
   }

   public String getDisplayName() { return this.displayName; }
   public int getSlot() { return this.slot; }
   public String getAction() { return this.action; }
   public void addLine(String s) { }
}