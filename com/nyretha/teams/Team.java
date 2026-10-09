package com.nyretha.teams;

import com.nyretha.teams.Manager.DataManager;
import com.nyretha.teams.Manager.LangManager;
import com.nyretha.teams.Manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Team extends JavaPlugin implements Listener {
    private TeamManager teamManager;
    private LangManager langManager;
    private final Set<UUID> teamChat = new HashSet<>();

    @Override public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        saveResourceIfPresent("config.yml");
        saveResourceIfPresent("lang.yml");
        this.langManager = new LangManager(this);
        this.teamManager = new TeamManager(this);
        this.teamManager.loadAllTeams();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PvPListener(this), this);
        getServer().getPluginManager().registerEvents(new TeamChatListener(this), this);
        getLogger().info("NyrethaTeams enabled. Loaded " + teamManager.getTeams().size() + " teams.");
    }
    private void saveResourceIfPresent(String name) {
        File file = new File(getDataFolder(), name);
        if (file.exists()) return;
        try { saveResource(name, false); }
        catch (IllegalArgumentException ignored) {
            try { file.createNewFile(); } catch (java.io.IOException ex) { getLogger().warning(ex.getMessage()); }
        }
    }
    @Override public void onDisable() {
        if (teamManager != null) for (DataManager team : teamManager.getTeams()) team.save();
    }
    public TeamManager getTeamManager() { return teamManager; }
    public LangManager getLangManager() { return langManager; }
    public boolean isTeamChatEnabled(Player player) { return teamChat.contains(player.getUniqueId()); }
    public void setTeamChatEnabled(Player player, boolean enabled) {
        if (enabled) teamChat.add(player.getUniqueId()); else teamChat.remove(player.getUniqueId());
    }
    private void msg(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(langManager.getMessage(key, replacements));
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(HexUtils.colorize("&a/team create <name> &7| &a/team invite <player> &7| &a/team join <name>"));
            sender.sendMessage(HexUtils.colorize("&a/team leave &7| &a/team delete &7| &a/team info &7| &a/team sethome &7| &a/team home"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("join")) {
            if (!(sender instanceof Player p) || args.length < 2) return usage(sender);
            String invited = teamManager.getInvite(p.getName());
            DataManager target = teamManager.getTeam(args[1]);
            if (target == null || invited == null || !invited.equalsIgnoreCase(args[1])) { msg(sender, "errors.team-not-found"); return true; }
            if (teamManager.getPlayerTeam(p.getName()) != null) { msg(sender, "errors.already-in-team"); return true; }
            target.addMember(p.getName()); teamManager.clearInvite(p.getName());
            msg(p, "team.joined", "%team%", target.getName());
            for (String member : target.getMembers()) { Player online = Bukkit.getPlayerExact(member); if (online != null && !online.equals(p)) msg(online, "team.member-joined", "%player%", p.getName()); }
            return true;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("This command requires a player."); return true; }
        DataManager current = teamManager.getPlayerTeam(p.getName());
        switch (sub) {
            case "create" -> {
                if (args.length < 2) return usage(sender);
                String name = args[1];
                int min = getConfig().getInt("min-team-name-length", 3), max = getConfig().getInt("max-team-name-length", 9);
                if (name.length() < min || name.length() > max || getConfig().getStringList("blocked-team-names").stream().anyMatch(name.toLowerCase()::contains)) {
                    p.sendMessage(HexUtils.colorize("&cInvalid team name.")); return true;
                }
                if (teamManager.getPlayerTeam(p.getName()) != null) { msg(p, "errors.already-in-team"); return true; }
                if (!teamManager.create(name, p.getName())) { msg(p, "errors.team-already-exists"); return true; }
                msg(p, "team.created", "%team%", name);
            }
            case "invite" -> {
                if (current == null) { msg(p, "errors.not-in-team"); return true; }
                if (!current.isLeader(p.getName())) { msg(p, "errors.not-leader"); return true; }
                if (args.length < 2) return usage(sender);
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { msg(p, "errors.player-not-found"); return true; }
                if (teamManager.getPlayerTeam(target.getName()) != null) { msg(p, "errors.player-already-in-team"); return true; }
                teamManager.invite(target.getName(), current.getName());
                msg(p, "team.invite-sent", "%player%", target.getName());
                msg(target, "team.invite-received", "%player%", p.getName(), "%team%", current.getName());
            }
            case "leave" -> {
                if (current == null) { msg(p, "errors.not-in-team"); return true; }
                if (current.isLeader(p.getName())) { msg(p, "errors.leader-cant-leave"); return true; }
                current.removeMember(p.getName()); msg(p, "team.left");
            }
            case "delete" -> {
                if (current == null) { msg(p, "errors.not-in-team"); return true; }
                if (!current.isLeader(p.getName()) && !p.hasPermission("team.admin")) { msg(p, "errors.not-leader"); return true; }
                teamManager.remove(current.getName()); msg(p, "team.deleted", "%team%", current.getName());
            }
            case "info" -> {
                if (current == null) { msg(p, "errors.not-in-team"); return true; }
                p.sendMessage(HexUtils.colorize("&aTeam: &f" + current.getName() + " &7Leader: &f" + current.getLeader()));
                p.sendMessage(HexUtils.colorize("&aMembers: &f" + String.join(", ", current.getMembers())));
            }
            case "sethome" -> {
                if (current == null) { msg(p, "errors.not-in-team"); return true; }
                if (!current.isLeader(p.getName()) && !p.hasPermission("team.admin")) { msg(p, "errors.not-leader"); return true; }
                current.setHome(p.getLocation()); msg(p, "team.home-set");
            }
            case "home" -> {
                if (current == null) { msg(p, "errors.not-in-team"); return true; }
                if (!getConfig().getBoolean("team-home.enabled", true)) { p.sendMessage(HexUtils.colorize("&cTeam homes are disabled.")); return true; }
                Location home = current.getHome();
                if (home == null) { msg(p, "team.no-team-home"); return true; }
                p.teleport(home); msg(p, "team.teleport-completed");
            }
            case "gui", "menu" -> new com.nyretha.teams.GUI.TeamGUI(this).open(p);
            case "members" -> new com.nyretha.teams.GUI.MemberManagerGUI(this).open(p);
            case "chat" -> {
                if (current == null) { msg(p, "errors.not-in-team"); return true; }
                boolean enabled = !isTeamChatEnabled(p); setTeamChatEnabled(p, enabled);
                p.sendMessage(HexUtils.colorize(enabled ? "&aTeam chat enabled." : "&cTeam chat disabled."));
                if (args.length > 1 && enabled) {
                    String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    for (String member : current.getMembers()) { Player online = Bukkit.getPlayerExact(member); if (online != null) online.sendMessage(HexUtils.colorize("&a[Team] &f" + p.getName() + ": " + message)); }
                }
            }
            default -> usage(sender);
        }
        return true;
    }
    private boolean usage(CommandSender sender) {
        sender.sendMessage(HexUtils.colorize("&cUsage: /team create|invite|join|leave|delete|info|sethome|home|chat"));
        return true;
    }
}