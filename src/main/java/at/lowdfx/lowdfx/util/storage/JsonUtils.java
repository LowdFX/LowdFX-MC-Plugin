package at.lowdfx.lowdfx.util.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import at.lowdfx.lowdfx.LowdFX;

import java.io.*;
import java.util.Map;

/**
 * Utility class for JSON serialization and deserialization.
 */
public final class JsonUtils {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonUtils() {}

    /**
     * Saves an object to a JSON file safely.
     *
     * @param object The object to save
     * @param file   The target file
     */
    public static void saveSafe(Object object, File file) {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(object, writer);
            }
        } catch (IOException e) {
            LowdFX.LOG.error("Fehler beim Speichern der JSON-Datei: {}", file.getPath(), e);
        }
    }

    /**
     * Loads an object from a JSON file safely.
     *
     * @param file         The file to load from
     * @param defaultValue The default value if loading fails
     * @param typeToken    The type token for deserialization
     * @param <T>          The type of object to load
     * @return The loaded object or default value
     */
    public static <T> T loadSafe(File file, T defaultValue, TypeToken<T> typeToken) {
        if (!file.exists()) {
            return defaultValue;
        }
        try (Reader reader = new FileReader(file)) {
            T result = GSON.fromJson(reader, typeToken.getType());
            return result != null ? result : defaultValue;
        } catch (IOException e) {
            LowdFX.LOG.error("Fehler beim Laden der JSON-Datei: {}", file.getPath(), e);
            return defaultValue;
        }
    }

    /**
     * Saves a map to a JSON file safely.
     *
     * @param map  The map to save
     * @param file The target file
     */
    public static void saveMapSafe(Map<?, ?> map, File file) {
        saveSafe(map, file);
    }

    /**
     * Loads a map from a JSON file safely.
     *
     * @param file         The file to load from
     * @param defaultValue The default value if loading fails
     * @return The loaded map or default value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadMapSafe(File file, Map<String, Object> defaultValue) {
        if (!file.exists()) {
            return defaultValue;
        }
        try (Reader reader = new FileReader(file)) {
            Map<String, Object> result = GSON.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
            return result != null ? result : defaultValue;
        } catch (IOException e) {
            LowdFX.LOG.error("Fehler beim Laden der JSON-Map-Datei: {}", file.getPath(), e);
            return defaultValue;
        }
    }
}
