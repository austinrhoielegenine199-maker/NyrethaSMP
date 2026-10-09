package com.nyretha.teams.GUI;

import com.nyretha.teams.HexUtils;
import com.nyretha.teams.Team;
import com.nyretha.teams.Manager.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public final class MemberManagerGUI {
    private final Team plugin;
    public MemberManagerGUI(Team plugin) { this.plugin = plugin; }
    public void open(Player player) {
        DataManager team = plugin.getTeamManager().getPlayerTeam(player.getName());
        Inventory inv = Bukkit.createInventory(new TeamGUIHolder(), 54, HexUtils.colorize("&8Team Members"));
        if (team != null) {
            int slot = 0;
            for (String member : team.getMembers()) {
                if (slot >= inv.getSize()) break;
                ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(HexUtils.colorize("&a" + member));
                meta.setLore(List.of(HexUtils.colorize(team.isLeader(member) ? "&6Leader" : "&7Member")));
                item.setItemMeta(meta); inv.setItem(slot++, item);
            }
        }
        player.openInventory(inv);
    }
}