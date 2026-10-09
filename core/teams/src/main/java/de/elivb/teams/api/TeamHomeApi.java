package de.elivb.teams.api;

import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Small public-facing adapter for other plugins that need to read or use Team Home.
 * It intentionally avoids exposing internal team storage classes.
 */
public final class TeamHomeApi {
    private final JavaPlugin plugin;

    public TeamHomeApi(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasTeam(Player player) {
        return getTeam(player) != null;
    }

    public Location getTeamHome(Player player) {
        Object team = getTeam(player);
        if (team == null) return null;
        try {
            Object value = invoke(team, "getHome");
            return value instanceof Location ? (Location) value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public boolean hasTeamHome(Player player) {
        Location home = getTeamHome(player);
        return home != null && home.getWorld() != null;
    }

    public boolean teleportToTeamHome(Player player) {
        if (!hasTeamHome(player)) return false;
        try {
            Object homeManager = invoke(plugin, "getTeamHome");
            Object result = invoke(homeManager, "teleportToHome", player);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private Object getTeam(Player player) {
        try {
            Object manager = invoke(plugin, "getTeamManager");
            return invoke(manager, "getPlayerTeam", player.getName());
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object invoke(Object target, String name, Object... args) throws ReflectiveOperationException {
        if (target == null) return null;
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            try {
                return method.invoke(target, args);
            } catch (IllegalArgumentException ignored) {
                // Try another overload.
            }
        }
        throw new NoSuchMethodException(name);
    }
}
