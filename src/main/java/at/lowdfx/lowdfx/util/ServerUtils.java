package at.lowdfx.lowdfx.util;

import at.lowdfx.lowdfx.LowdFX;
import org.bukkit.event.Listener;

/**
 * Utility class for server-related operations.
 */
public final class ServerUtils {
    private ServerUtils() {}

    public static void registerEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            LowdFX.PLUGIN.getServer().getPluginManager().registerEvents(listener, LowdFX.PLUGIN);
        }
    }
}
