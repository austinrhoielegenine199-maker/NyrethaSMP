package com.nyretha.team;

import com.nyretha.team.manager.TeamHomeApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Nyretha Teams entry point. Keeps team membership and shared home data in config.yml.
 * This implementation is a compact native module and does not include licensing code.
 */
public final class Team extends JavaPlugin {
    private TeamHomeApi homeApi;
    @Override public void onEnable() { saveDefaultConfig(); homeApi = new TeamHomeApi(this); getLogger().info("NyrethaTeams enabled."); }
    public TeamHomeApi getTeamHomeApi() { return homeApi; }

    public String getPlayerTeamName(Player player) { return getConfig().getString("members." + player.getUniqueId()); }
    public Location getTeamHome(Player player) {
        String team = getPlayerTeamName(player); if (team == null) return null;
        String raw = getConfig().getString("teams." + team + ".home"); if (raw == null) return null;
        try {
            String[] a=raw.split(";"); org.bukkit.World w=Bukkit.getWorld(a[0]); if(w==null)return null;
            return new Location(w,Double.parseDouble(a[1]),Double.parseDouble(a[2]),Double.parseDouble(a[3]),Float.parseFloat(a[4]),Float.parseFloat(a[5]));
        } catch (RuntimeException ex) { return null; }
    }
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        Player p=(Player)sender;
        if (!command.getName().equalsIgnoreCase("team")) return false;
        if(args.length==0){ p.sendMessage(color("&e/team create <name> &7| &e/team invite <player> &7| &e/team sethome")); return true; }
        switch(args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "create":
                if(args.length<2){p.sendMessage(color("&cUsage: /team create <name>"));return true;}
                if(getPlayerTeamName(p)!=null){p.sendMessage(color("&cYou are already in a team."));return true;}
                String name=args[1].replaceAll("[^A-Za-z0-9_-]","");
                if(name.isEmpty()||getConfig().contains("teams."+name)){p.sendMessage(color("&cThat team name is invalid or taken."));return true;}
                getConfig().set("teams."+name+".owner",p.getUniqueId().toString());
                getConfig().set("teams."+name+".members",java.util.Collections.singletonList(p.getUniqueId().toString()));
                getConfig().set("members."+p.getUniqueId(),name); saveConfig(); p.sendMessage(color("&aTeam created: "+name)); return true;
            case "sethome":
                String team=getPlayerTeamName(p);
                if(team==null){p.sendMessage(color("&cYou are not in a team."));return true;}
                if(!p.getUniqueId().toString().equals(getConfig().getString("teams."+team+".owner"))){p.sendMessage(color("&cOnly the team owner can set the team home."));return true;}
                Location l=p.getLocation();
                getConfig().set("teams."+team+".home",l.getWorld().getName()+";"+l.getX()+";"+l.getY()+";"+l.getZ()+";"+l.getYaw()+";"+l.getPitch());
                saveConfig(); p.sendMessage(color("&aTeam home saved.")); return true;
            case "home":
                Location home=getTeamHome(p); if(home==null){p.sendMessage(color("&cYour team has no home set."));return true;}
                p.teleport(home); return true;
            default: p.sendMessage(color("&cUnknown team command.")); return true;
        }
    }
    private static String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
}