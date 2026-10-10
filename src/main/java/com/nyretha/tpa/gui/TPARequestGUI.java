package com.nyretha.tpa.gui;

import com.nyretha.tpa.Hex;
import com.nyretha.tpa.TPA;
import com.nyretha.tpa.Manager.TPARequest;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

final class TPARequestGUI implements Listener {
   private final TPA plugin;
   private final Player viewer;
   private final Player sender;
   private final Player target;
   private final TPARequest request;
   private final String key;
   private final boolean sendMode;
   private final boolean hereMode;
   private Inventory inventory;

   TPARequestGUI(TPA plugin, Player viewer, Player sender, Player target, TPARequest request, String key, boolean sendMode, boolean hereMode) {
      this.plugin=plugin; this.viewer=viewer; this.sender=sender; this.target=target; this.request=request; this.key=key; this.sendMode=sendMode; this.hereMode=hereMode;
   }
   void open() {
      String name=this.plugin.getGUIManager().getGUIName(key, "&8ᴛᴘᴀ");
      int rows=this.plugin.getGUIManager().getGUIRows(key,3);
      inventory=Bukkit.createInventory((InventoryHolder)null,Math.max(1,Math.min(6,rows))*9,Hex.translateAllColorCodes(name));
      putIcon("cancel-icon",Material.RED_STAINED_GLASS_PANE,10,"&cCancel");
      putIcon("location-icon",Material.GRASS_BLOCK,12,"&aLocation");
      putIcon("player-icon",Material.PLAYER_HEAD,13,"&aPlayer");
      putIcon("fly-icon",Material.FEATHER,14,"&aFlying");
      putIcon("confirm-icon",Material.LIME_STAINED_GLASS_PANE,16,"&aConfirm");
      plugin.getServer().getPluginManager().registerEvents(this,plugin);
      viewer.openInventory(inventory);
   }
   private void putIcon(String icon,Material fallback,int defaultSlot,String defaultName) {
      int slot=plugin.getGUIManager().getIconSlot(key,icon,defaultSlot);
      if(slot<0 || slot>=inventory.getSize()) return;
      String materialName=plugin.getGUIManager().getIconMaterial(key,icon,fallback.name());
      Material material=Material.matchMaterial(materialName==null?fallback.name():materialName);
      if(material==null) material=fallback;
      ItemStack item=new ItemStack(material);
      ItemMeta meta=item.getItemMeta();
      String name=plugin.getGUIManager().getIconDisplayName(key,icon,defaultName);
      if(name!=null) meta.setDisplayName(Hex.translateAllColorCodes(name.replace("%player%",target.getName())));
      List<String> lore=plugin.getGUIManager().getIconLore(key,icon);
      if(lore!=null&&!lore.isEmpty()) {
         List<String> lines=new ArrayList<>();
         for(String line:lore) lines.add(Hex.translateAllColorCodes(line.replace("%player%",target.getName()).replace("%world%",plugin.getConfigManager().getWorldNickname(sender.getWorld().getName())).replace("%is_flying%",sender.isFlying()?"Yes":"No")));
         meta.setLore(lines);
      }
      if(item.getType()==Material.PLAYER_HEAD && meta instanceof SkullMeta skull) {
         Player headPlayer=sendMode?target:sender;
         if(headPlayer!=null) skull.setOwningPlayer(headPlayer);
      }
      item.setItemMeta(meta);
      inventory.setItem(slot,item);
   }
   @EventHandler public void onClick(InventoryClickEvent event) {
      if(!event.getInventory().equals(inventory)) return;
      event.setCancelled(true);
      if(!(event.getWhoClicked() instanceof Player player)) return;
      int confirm=plugin.getGUIManager().getIconSlot(key,"confirm-icon",16);
      int cancel=plugin.getGUIManager().getIconSlot(key,"cancel-icon",10);
      plugin.getSoundManager().playButtonClickSound(player);
      if(event.getSlot()==confirm) {
         player.closeInventory();
         if(sendMode) plugin.sendTPARequestDirect(sender,target,hereMode);
         else plugin.handleTPAAcceptDirect(viewer,request);
      } else if(event.getSlot()==cancel) {
         player.closeInventory();
         if(sendMode) sender.sendMessage(plugin.getLanguageManager().getMessage("request-cancelled"));
         else plugin.handleTPADenyDirect(viewer,request);
      }
   }
   @EventHandler public void onClose(InventoryCloseEvent event) {
      if(event.getInventory().equals(inventory)) HandlerList.unregisterAll(this);
   }
}