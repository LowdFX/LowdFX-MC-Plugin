package at.lowdfx.lowdfx.util;

import at.lowdfx.lowdfx.managers.teleport.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Eine vereinfachte, serialisierbare Darstellung einer Location.
 */
public record SimpleLocation(String world, Cord cord, float yaw, float pitch) {

    public void teleportSafe(Entity entity) {
        TeleportManager.teleportSafe(entity, asLocation());
    }

    /**
     * Konvertiert diese SimpleLocation zu einer Bukkit Location.
     */
    public @NotNull Location asLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            w = Objects.requireNonNull(Bukkit.createWorld(WorldCreator.name(world)));
        }
        Location loc = new Location(w, cord.x(), cord.y(), cord.z());
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        return loc;
    }

    /**
     * Erstellt eine SimpleLocation aus einer Bukkit Location.
     */
    public static @NotNull SimpleLocation ofLocation(@NotNull Location location) {
        return new SimpleLocation(
                location.getWorld().getName(),
                Cord.of(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch()
        );
    }
}
