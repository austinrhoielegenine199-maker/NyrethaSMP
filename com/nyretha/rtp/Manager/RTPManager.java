package com.nyretha.rtp.Manager;

import com.nyretha.rtp.RTP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Random-teleport selection and cooldown support.
 * The plugin entry point will wire this manager to configuration and events.
 */
public class RTPManager {
    private final RTP plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public RTPManager(RTP plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player, long cooldownMillis) {
        Long last = cooldowns.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last < cooldownMillis;
    }

    public long getRemainingCooldown(Player player, long cooldownMillis) {
        Long last = cooldowns.get(player.getUniqueId());
        if (last == null) return 0L;
        return Math.max(0L, cooldownMillis - (System.currentTimeMillis() - last));
    }

    public void markUsed(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public Location findRandomLocation(World world, int minRadius, int maxRadius, int maxAttempts) {
        if (world == null || maxRadius < minRadius || maxAttempts <= 0) return null;
        for (int i = 0; i < maxAttempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int radius = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius + 1));
            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            if (isSafe(candidate)) return candidate;
        }
        return null;
    }

    public boolean isSafe(Location location) {
        if (location == null || location.getWorld() == null) return false;
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) return false;
        return world.getBlockAt(x, y - 1, z).getType().isSolid()
                && world.getBlockAt(x, y, z).isPassable()
                && world.getBlockAt(x, y + 1, z).isPassable();
    }

    public RTP getPlugin() {
        return plugin;
    }

    public World getWorld(String name) {
        return name == null ? null : Bukkit.getWorld(name);
    }
}
