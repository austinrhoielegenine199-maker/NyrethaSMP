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

public final class TeamGUI {
    private final Team plugin;
    public TeamGUI(Team plugin) { this.plugin = plugin; }
    public void open(Player player) {
        DataManager team = plugin.getTeamManager().getPlayerTeam(player.getName());
        Inventory inv = Bukkit.createInventory(new TeamGUIHolder(), 27, HexUtils.colorize("&8Team Menu"));
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(HexUtils.colorize(team == null ? "&cNot in a team" : "&a" + team.getName()));
        meta.setLore(team == null ? List.of(HexUtils.colorize("&7Use /team create <name>")) :
            List.of(HexUtils.colorize("&7Leader: &f" + team.getLeader()), HexUtils.colorize("&7Members: &f" + team.getMembers().size())));
        info.setItemMeta(meta); inv.setItem(13, info);
        player.openInventory(inv);
    }
}