package com.isekai.nationsplus.data;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Objects;

public record ClaimChunk(String world, int x, int z) {

    public static ClaimChunk fromChunk(Chunk chunk) {
        return new ClaimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static ClaimChunk fromLocation(Location loc) {
        return new ClaimChunk(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public static ClaimChunk fromString(String s) {
        String[] parts = s.split(",");
        if (parts.length != 3) return null;
        return new ClaimChunk(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    @Override
    public String toString() {
        return world + "," + x + "," + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimChunk c)) return false;
        return x == c.x && z == c.z && Objects.equals(world, c.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }
}
