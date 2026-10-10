package com.nyretha.rtp.Manager;

import com.nyretha.rtp.RTP;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zone boundaries and optional WorldGuard integration hook for RTP.
 * WorldGuard checks can be connected here during main-class integration.
 */
public class RTPZoneManager {
    private final RTP plugin;
    private final Map<String, Zone> zones = new ConcurrentHashMap<>();

    public RTPZoneManager(RTP plugin) {
        this.plugin = plugin;
    }

    public void setZone(String name, World world, int minX, int maxX, int minZ, int maxZ) {
        if (name == null || world == null) return;
        zones.put(name.toLowerCase(), new Zone(world.getName(), minX, maxX, minZ, maxZ));
    }

    public void removeZone(String name) {
        if (name != null) zones.remove(name.toLowerCase());
    }

    public boolean isInsideAnyZone(Location location) {
        if (location == null || location.getWorld() == null) return false;
        for (Zone zone : zones.values()) {
            if (zone.contains(location)) return true;
        }
        return false;
    }

    public boolean isInsideZone(String name, Location location) {
        if (name == null || location == null) return false;
        Zone zone = zones.get(name.toLowerCase());
        return zone != null && zone.contains(location);
    }

    public boolean canRtp(Player player, Location location) {
        return player != null && location != null && location.getWorld() != null
                && !isInsideAnyZone(location);
    }

    public RTP getPlugin() {
        return plugin;
    }

    private static final class Zone {
        private final String world;
        private final int minX, maxX, minZ, maxZ;

        private Zone(String world, int minX, int maxX, int minZ, int maxZ) {
            this.world = world;
            this.minX = Math.min(minX, maxX);
            this.maxX = Math.max(minX, maxX);
            this.minZ = Math.min(minZ, maxZ);
            this.maxZ = Math.max(minZ, maxZ);
        }

        private boolean contains(Location location) {
            return location.getWorld() != null && world.equals(location.getWorld().getName())
                    && location.getBlockX() >= minX && location.getBlockX() <= maxX
                    && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
        }
    }
}
