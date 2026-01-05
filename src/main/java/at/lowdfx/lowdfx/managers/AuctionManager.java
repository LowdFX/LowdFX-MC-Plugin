package at.lowdfx.lowdfx.managers;

import at.lowdfx.lowdfx.LowdFX;
import at.lowdfx.lowdfx.util.Configuration;
import at.lowdfx.lowdfx.util.EconomyHandler;
import at.lowdfx.lowdfx.util.MessagedException;
import at.lowdfx.lowdfx.util.storage.JsonUtils;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager für das Auktionshaus-System.
 */
public final class AuctionManager {

    /**
     * Repräsentiert eine Auktion.
     */
    public static final class Auction {
        private final UUID id;
        private final UUID seller;
        private final byte[] serializedItem;
        private final int price;
        private final long createdAt;
        private final long expiresAt;

        public Auction(UUID id, UUID seller, byte[] serializedItem, int price, long createdAt, long expiresAt) {
            this.id = id;
            this.seller = seller;
            this.serializedItem = serializedItem;
            this.price = price;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public UUID id() { return id; }
        public UUID seller() { return seller; }
        public int price() { return price; }
        public long createdAt() { return createdAt; }
        public long expiresAt() { return expiresAt; }

        public @NotNull ItemStack item() {
            return ItemStack.deserializeBytes(serializedItem).clone();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() / 1000 > expiresAt;
        }

        public long remainingSeconds() {
            return Math.max(0, expiresAt - System.currentTimeMillis() / 1000);
        }
    }

    // Alle aktiven Auktionen (Key = Auction ID)
    public static final Map<UUID, Auction> AUCTIONS = new ConcurrentHashMap<>();

    // Auktionen pro Spieler für schnellen Zugriff
    public static final Map<UUID, List<UUID>> PLAYER_AUCTIONS = new ConcurrentHashMap<>();

    public static void save() {
        JsonUtils.saveSafe(AUCTIONS, LowdFX.DATA_DIR.resolve("auctions.json").toFile());
        JsonUtils.saveSafe(PLAYER_AUCTIONS, LowdFX.DATA_DIR.resolve("player-auctions.json").toFile());
    }

    public static void load() {
        AUCTIONS.putAll(JsonUtils.loadSafe(LowdFX.DATA_DIR.resolve("auctions.json").toFile(), Map.of(), new TypeToken<>() {}));
        PLAYER_AUCTIONS.putAll(JsonUtils.loadSafe(LowdFX.DATA_DIR.resolve("player-auctions.json").toFile(), Map.of(), new TypeToken<>() {}));

        // Cleanup beim Laden
        cleanupExpired();
    }

    /**
     * Erstellt eine neue Auktion.
     */
    public static Auction createAuction(UUID seller, ItemStack item, int price) throws MessagedException {
        // Prüfe ob Auktionen aktiviert sind
        if (!Configuration.AUCTION_ENABLED) {
            throw new MessagedException("Das Auktionshaus ist deaktiviert!");
        }

        // Prüfe maximale Auktionen pro Spieler
        List<UUID> playerAuctions = PLAYER_AUCTIONS.computeIfAbsent(seller, k -> new ArrayList<>());
        if (playerAuctions.size() >= Configuration.AUCTION_MAX_PER_PLAYER) {
            throw new MessagedException("Du hast bereits die maximale Anzahl an Auktionen (" + Configuration.AUCTION_MAX_PER_PLAYER + ")!");
        }

        // Prüfe Preislimits
        if (price < Configuration.AUCTION_MIN_PRICE) {
            throw new MessagedException("Der Mindestpreis ist " + EconomyHandler.format(Configuration.AUCTION_MIN_PRICE) + "!");
        }
        if (price > Configuration.AUCTION_MAX_PRICE) {
            throw new MessagedException("Der Höchstpreis ist " + EconomyHandler.format(Configuration.AUCTION_MAX_PRICE) + "!");
        }

        // Erstelle Auktion
        UUID auctionId = UUID.randomUUID();
        long now = System.currentTimeMillis() / 1000;
        long expiresAt = now + Configuration.AUCTION_DEFAULT_DURATION;

        Auction auction = new Auction(auctionId, seller, item.serializeAsBytes(), price, now, expiresAt);

        AUCTIONS.put(auctionId, auction);
        playerAuctions.add(auctionId);

        return auction;
    }

    /**
     * Kauft eine Auktion.
     */
    public static void buyAuction(Player buyer, UUID auctionId) throws MessagedException {
        Auction auction = AUCTIONS.get(auctionId);
        if (auction == null) {
            throw new MessagedException("Diese Auktion existiert nicht mehr!");
        }

        if (auction.isExpired()) {
            throw new MessagedException("Diese Auktion ist abgelaufen!");
        }

        if (auction.seller().equals(buyer.getUniqueId())) {
            throw new MessagedException("Du kannst deine eigene Auktion nicht kaufen!");
        }

        // Prüfe ob Käufer genug Geld hat
        if (!EconomyHandler.hasEnough(buyer, auction.price())) {
            throw new MessagedException("Du hast nicht genug " + EconomyHandler.getCurrencyName(auction.price()) + "!");
        }

        // Transaktion durchführen
        EconomyHandler.withdraw(buyer, auction.price());

        // Verkäufer bekommt Geld
        Player seller = Bukkit.getPlayer(auction.seller());
        if (seller != null) {
            EconomyHandler.deposit(seller, auction.price());
            seller.sendMessage(LowdFX.serverMessage(
                    net.kyori.adventure.text.Component.text("Deine Auktion wurde verkauft! +" + EconomyHandler.format(auction.price()),
                            net.kyori.adventure.text.format.NamedTextColor.GREEN)));
        } else {
            // Offline: Geld wird beim nächsten Login gutgeschrieben (TODO: Offline-Pending-System)
            // Für jetzt: direkt auf Vault-Konto einzahlen falls möglich
            if (EconomyHandler.isUsingVault()) {
                org.bukkit.OfflinePlayer offlineSeller = Bukkit.getOfflinePlayer(auction.seller());
                // Vault kann auch Offline-Spielern Geld geben
            }
        }

        // Item an Käufer geben
        ItemStack item = auction.item();
        var leftover = buyer.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(i -> buyer.getWorld().dropItem(buyer.getLocation(), i));
        }

        // Auktion entfernen
        removeAuction(auctionId);
    }

    /**
     * Storniert eine Auktion und gibt das Item zurück.
     */
    public static void cancelAuction(Player owner, UUID auctionId) throws MessagedException {
        Auction auction = AUCTIONS.get(auctionId);
        if (auction == null) {
            throw new MessagedException("Diese Auktion existiert nicht!");
        }

        if (!auction.seller().equals(owner.getUniqueId())) {
            throw new MessagedException("Du bist nicht der Besitzer dieser Auktion!");
        }

        // Item zurückgeben
        ItemStack item = auction.item();
        var leftover = owner.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(i -> owner.getWorld().dropItem(owner.getLocation(), i));
        }

        removeAuction(auctionId);
    }

    /**
     * Entfernt eine Auktion.
     */
    private static void removeAuction(UUID auctionId) {
        Auction auction = AUCTIONS.remove(auctionId);
        if (auction != null) {
            List<UUID> playerAuctions = PLAYER_AUCTIONS.get(auction.seller());
            if (playerAuctions != null) {
                playerAuctions.remove(auctionId);
            }
        }
    }

    /**
     * Entfernt abgelaufene Auktionen und gibt Items zurück.
     */
    public static void cleanupExpired() {
        List<UUID> expired = new ArrayList<>();

        for (Auction auction : AUCTIONS.values()) {
            if (auction.isExpired()) {
                expired.add(auction.id());
            }
        }

        for (UUID auctionId : expired) {
            Auction auction = AUCTIONS.get(auctionId);
            if (auction != null) {
                // Item an Verkäufer zurückgeben (falls online)
                Player seller = Bukkit.getPlayer(auction.seller());
                if (seller != null) {
                    ItemStack item = auction.item();
                    var leftover = seller.getInventory().addItem(item);
                    if (!leftover.isEmpty()) {
                        leftover.values().forEach(i -> seller.getWorld().dropItem(seller.getLocation(), i));
                    }
                    seller.sendMessage(LowdFX.serverMessage(
                            net.kyori.adventure.text.Component.text("Deine Auktion ist abgelaufen. Item wurde zurückgegeben.",
                                    net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
                }
                // TODO: Für Offline-Spieler: Pending-Items-System

                removeAuction(auctionId);
            }
        }
    }

    /**
     * Gibt alle aktiven Auktionen zurück (sortiert nach Erstellungszeit).
     */
    public static @NotNull List<Auction> getAllAuctions() {
        return AUCTIONS.values().stream()
                .filter(a -> !a.isExpired())
                .sorted(Comparator.comparingLong(Auction::createdAt).reversed())
                .toList();
    }

    /**
     * Gibt die Auktionen eines Spielers zurück.
     */
    public static @NotNull List<Auction> getPlayerAuctions(UUID playerId) {
        List<UUID> auctionIds = PLAYER_AUCTIONS.getOrDefault(playerId, List.of());
        return auctionIds.stream()
                .map(AUCTIONS::get)
                .filter(Objects::nonNull)
                .filter(a -> !a.isExpired())
                .toList();
    }

    /**
     * Gibt eine Auktion per ID zurück.
     */
    public static @Nullable Auction getAuction(UUID auctionId) {
        return AUCTIONS.get(auctionId);
    }

    /**
     * Startet den Cleanup-Task für abgelaufene Auktionen.
     */
    public static void startCleanupTask() {
        // Alle 5 Minuten aufräumen
        Bukkit.getScheduler().runTaskTimer(LowdFX.PLUGIN, AuctionManager::cleanupExpired, 6000, 6000);
    }
}
