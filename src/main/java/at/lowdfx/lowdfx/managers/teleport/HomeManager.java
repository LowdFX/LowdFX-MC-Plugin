package at.lowdfx.lowdfx.managers.teleport;

import at.lowdfx.lowdfx.LowdFX;
import at.lowdfx.lowdfx.util.SimpleLocation;
import com.google.gson.reflect.TypeToken;
import at.lowdfx.lowdfx.util.storage.JsonUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HomeManager {
    public static final Map<UUID, Map<String, SimpleLocation>> HOMES = new ConcurrentHashMap<>();

    public static void save() {
        JsonUtils.saveSafe(HOMES, LowdFX.DATA_DIR.resolve("homes.json").toFile());
    }

    public static void load() {
        HOMES.putAll(JsonUtils.loadSafe(LowdFX.DATA_DIR.resolve("homes.json").toFile(), Map.of(), new TypeToken<>() {}));
    }

    public static void add(UUID player) {
        if (HOMES.containsKey(player)) return;
        HOMES.put(player, new ConcurrentHashMap<>());
    }

    public static Map<String, SimpleLocation> get(UUID player) {
        return HOMES.get(player);
    }
}
