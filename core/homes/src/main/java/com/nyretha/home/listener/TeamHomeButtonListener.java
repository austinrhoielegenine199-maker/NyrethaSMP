package com.nyretha.home.listener;

import com.nyretha.home.Home;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/** Displays Team Home states and handles click-to-teleport when NyrethaTeams is installed. */
public final class TeamHomeButtonListener implements Listener {
    private final Home homes;
    private static final String TITLE = color("&8ʜᴏᴍᴇꜱ");
    public TeamHomeButtonListener(Home homes) { this.homes=homes; }
    public void register() { Bukkit.getPluginManager().registerEvents(this, homes); }
    @EventHandler public void onOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player && TITLE.equals(e.getView().getTitle())) update(e.getInventory(), (Player)e.getPlayer());
    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player) || e.getClickedInventory()!=e.getView().getTopInventory() || !TITLE.equals(e.getView().getTitle()) || e.getRawSlot()!=10) return;
        e.setCancelled(true); Player p=(Player)e.getWhoClicked();
        Plugin teams=Bukkit.getPluginManager().getPlugin("NyrethaTeams");
        if(teams==null || !teams.isEnabled()){p.sendMessage(color("&cYou have no team!"));return;}
        try {
            Object api=teams.getClass().getMethod("getTeamHomeApi").invoke(teams);
            Object hasTeam=api.getClass().getMethod("hasTeam",Player.class).invoke(api,p);
            if(!Boolean.TRUE.equals(hasTeam)){p.sendMessage(color("&cYou have no team!"));return;}
            Object home=api.getClass().getMethod("getTeamHome",Player.class).invoke(api,p);
            if(home instanceof org.bukkit.Location){p.closeInventory();p.teleport((org.bukkit.Location)home);}
            else p.sendMessage(color("&7Your team has not set a home yet."));
        } catch (ReflectiveOperationException ex) { homes.getLogger().warning("Team Home integration failed: "+ex.getMessage()); }
    }
    private void update(Inventory inv, Player p) {
        Plugin teams=Bukkit.getPluginManager().getPlugin("NyrethaTeams");
        Material material=Material.RED_BANNER; String name="&cʏᴏᴜ ʜᴀᴠᴇ ɴᴏ ᴛᴇᴀᴍ!";
        if(teams!=null && teams.isEnabled()) try {
            Object api=teams.getClass().getMethod("getTeamHomeApi").invoke(teams);
            boolean has=Boolean.TRUE.equals(api.getClass().getMethod("hasTeam",Player.class).invoke(api,p));
            if(has) { Object home=api.getClass().getMethod("getTeamHome",Player.class).invoke(api,p);
                if(home instanceof org.bukkit.Location){material=Material.BLUE_BANNER;name="&#0044FCᴛᴇᴀᴍ ʜᴏᴍᴇ";}
                else {material=Material.GRAY_BANNER;name="&7ᴛᴇᴀᴍ ʜᴏᴍᴇ";}
            }
        } catch(ReflectiveOperationException ex) { homes.getLogger().fine("Teams plugin API unavailable."); }
        ItemStack item=new ItemStack(material); ItemMeta meta=item.getItemMeta();
        if(meta!=null){meta.setDisplayName(color(name));item.setItemMeta(meta);} inv.setItem(10,item);
    }
    private static String color(String s){return ChatColor.translateAlternateColorCodes('&',s.replace("&#0044FC","&9"));}
}