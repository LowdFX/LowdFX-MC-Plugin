package at.lowdfx.lowdfx.util;

import at.lowdfx.lowdfx.LowdFX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
    public static FileConfiguration CONFIG;

    public static Component CONNECTION_FIRST_JOIN;
    public static Component CONNECTION_JOIN;
    public static Component CONNECTION_QUIT;

    public static String BASIC_SERVER_NAME;
    public static boolean BASIC_STARTERKIT;
    public static boolean BASIC_CUSTOM_HELP;
    public static long BASIC_HOLOGRAM_REFRESH_INTERVAL;

    public static int DEFAULT_MAX_HOMES;
    public static Map<String, Integer> HOME_MAXHOMES = new HashMap<>();

    public static long WARNING_TEMPBAN_DURATION;

    public static boolean SAFE_TELEPORT_ENABLED;
    public static int TELEPORT_DELAY;
    public static int BACK_COOLDOWN;

    public static boolean DEATHLOG_GLOBAL;
    public static String DEATHLOG_WORLD;
    public static int DEATHLOG_MAX_ENTRIES;

    // Economy Einstellungen
    public static String ECONOMY_CURRENCY;
    public static String ECONOMY_CURRENCY_NAME;
    public static String ECONOMY_CURRENCY_SINGULAR;

    // Auction Einstellungen
    public static boolean AUCTION_ENABLED;
    public static int AUCTION_MAX_PER_PLAYER;
    public static long AUCTION_DEFAULT_DURATION;
    public static int AUCTION_MIN_PRICE;
    public static int AUCTION_MAX_PRICE;


    public static void init(@NotNull JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        CONFIG = plugin.getConfig();
        loadValues();
    }

    public static void reload() {
        LowdFX.PLUGIN.reloadConfig();
        CONFIG = LowdFX.PLUGIN.getConfig();
        loadValues();
    }

    private static void loadValues() {
        CONNECTION_FIRST_JOIN = MiniMessage.miniMessage().deserialize(CONFIG.getString("connection.first-join", ""));
        CONNECTION_JOIN = MiniMessage.miniMessage().deserialize(CONFIG.getString("connection.join", ""));
        CONNECTION_QUIT = MiniMessage.miniMessage().deserialize(CONFIG.getString("connection.quit", ""));

        BASIC_SERVER_NAME = CONFIG.getString("basic.server-name", "Server");
        BASIC_STARTERKIT = CONFIG.getBoolean("basic.starterkit", true);
        BASIC_CUSTOM_HELP = CONFIG.getBoolean("basic.customhelp", true);
        BASIC_HOLOGRAM_REFRESH_INTERVAL = CONFIG.getLong("basic.hologram-refresh-interval", 20);

        DEFAULT_MAX_HOMES = CONFIG.getInt("home.default-max-homes", 5);
        HOME_MAXHOMES.clear();
        ConfigurationSection homeSection = CONFIG.getConfigurationSection("home.max-homes");
        if (homeSection != null) {
            for (String group : homeSection.getKeys(false)) {
                HOME_MAXHOMES.put(group, homeSection.getInt(group, DEFAULT_MAX_HOMES));
            }
        }

        WARNING_TEMPBAN_DURATION = CONFIG.getLong("warning.tempban-duration", 1440) * 60000;

        SAFE_TELEPORT_ENABLED = CONFIG.getBoolean("teleport.safe-enabled", true);
        TELEPORT_DELAY = CONFIG.getInt("teleport.delay", 5);
        BACK_COOLDOWN = CONFIG.getInt("teleport.backCooldown", 43200);

        DEATHLOG_GLOBAL = CONFIG.getBoolean("deathlog.global", false);
        DEATHLOG_WORLD = CONFIG.getString("deathlog.world", "world");
        DEATHLOG_MAX_ENTRIES = CONFIG.getInt("deathlog.maxEntriesPerPlayer", 3);

        // Economy
        ECONOMY_CURRENCY = CONFIG.getString("economy.currency", "diamonds");
        ECONOMY_CURRENCY_NAME = CONFIG.getString("economy.currency-name", "Diamanten");
        ECONOMY_CURRENCY_SINGULAR = CONFIG.getString("economy.currency-name-singular", "Diamant");

        // Auction
        AUCTION_ENABLED = CONFIG.getBoolean("auction.enabled", true);
        AUCTION_MAX_PER_PLAYER = CONFIG.getInt("auction.max-auctions-per-player", 5);
        AUCTION_DEFAULT_DURATION = CONFIG.getLong("auction.default-duration", 86400);
        AUCTION_MIN_PRICE = CONFIG.getInt("auction.min-price", 1);
        AUCTION_MAX_PRICE = CONFIG.getInt("auction.max-price", 1000000);
    }

    public static FileConfiguration get() {
        return CONFIG;
    }
}
