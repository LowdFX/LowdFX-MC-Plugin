package at.lowdfx.lowdfx.managers.teleport;


import at.lowdfx.lowdfx.LowdFX;
import at.lowdfx.lowdfx.util.SimpleLocation;
import com.google.gson.reflect.TypeToken;
import at.lowdfx.lowdfx.util.storage.JsonUtils;
import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WarpManager {
    public static final Map<String, SimpleLocation> WARPS = new ConcurrentHashMap<>();

    public static void save() {
        JsonUtils.saveSafe(WARPS, LowdFX.DATA_DIR.resolve("warps.json").toFile());
    }

    public static void load() {
        WARPS.putAll(JsonUtils.loadSafe(LowdFX.DATA_DIR.resolve("warps.json").toFile(), Map.of(), new TypeToken<>() {}));
    }
    public static void setSpawn(String name, Location location) {
        WARPS.put(name, SimpleLocation.ofLocation(location));
    }
}
