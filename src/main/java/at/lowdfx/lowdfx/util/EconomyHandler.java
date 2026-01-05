package at.lowdfx.lowdfx.util;

import at.lowdfx.lowdfx.LowdFX;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Handler für das Währungssystem.
 * Unterstützt Vault (Economy-Plugins) und Diamanten als Fallback.
 */
public final class EconomyHandler {
    private static Economy vaultEconomy = null;
    private static boolean useVault = false;

    private EconomyHandler() {}

    /**
     * Initialisiert den EconomyHandler.
     * Sollte in onEnable() aufgerufen werden.
     */
    public static void init() {
        // Prüfe Config-Einstellung
        String currencyType = Configuration.ECONOMY_CURRENCY;

        if ("vault".equalsIgnoreCase(currencyType)) {
            if (setupVault()) {
                useVault = true;
                LowdFX.LOG.info("Vault Economy erfolgreich aktiviert!");
            } else {
                useVault = false;
                LowdFX.LOG.warn("Vault nicht gefunden! Fallback auf Diamanten-Währung.");
            }
        } else {
            useVault = false;
            LowdFX.LOG.info("Diamanten-Währung aktiviert.");
        }
    }

    private static boolean setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        vaultEconomy = rsp.getProvider();
        return vaultEconomy != null;
    }

    /**
     * Prüft ob Vault verwendet wird.
     */
    public static boolean isUsingVault() {
        return useVault && vaultEconomy != null;
    }

    /**
     * Gibt den Währungsnamen zurück.
     */
    public static String getCurrencyName(int amount) {
        if (isUsingVault()) {
            return amount == 1 ? vaultEconomy.currencyNameSingular() : vaultEconomy.currencyNamePlural();
        }
        return amount == 1 ? Configuration.ECONOMY_CURRENCY_SINGULAR : Configuration.ECONOMY_CURRENCY_NAME;
    }

    /**
     * Prüft ob der Spieler genug Geld/Diamanten hat.
     */
    public static boolean hasEnough(Player player, int amount) {
        if (isUsingVault()) {
            return vaultEconomy.has(player, amount);
        }
        // Diamanten zählen
        return countDiamonds(player) >= amount;
    }

    /**
     * Gibt den Kontostand des Spielers zurück.
     */
    public static double getBalance(Player player) {
        if (isUsingVault()) {
            return vaultEconomy.getBalance(player);
        }
        return countDiamonds(player);
    }

    /**
     * Zieht Geld/Diamanten vom Spieler ab.
     * @return true wenn erfolgreich
     */
    public static boolean withdraw(Player player, int amount) {
        if (isUsingVault()) {
            return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
        }
        return removeDiamonds(player, amount);
    }

    /**
     * Gibt dem Spieler Geld/Diamanten.
     * @return true wenn erfolgreich
     */
    public static boolean deposit(Player player, int amount) {
        if (isUsingVault()) {
            return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
        }
        return addDiamonds(player, amount);
    }

    /**
     * Transferiert Geld/Diamanten von einem Spieler zum anderen.
     * @return true wenn erfolgreich
     */
    public static boolean transfer(Player from, Player to, int amount) {
        if (!hasEnough(from, amount)) {
            return false;
        }
        if (withdraw(from, amount)) {
            return deposit(to, amount);
        }
        return false;
    }

    // ==================== Diamanten-Hilfsmethoden ====================

    private static int countDiamonds(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static boolean removeDiamonds(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.DIAMOND) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }
        return remaining == 0;
    }

    private static boolean addDiamonds(Player player, int amount) {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, amount);
        var leftover = player.getInventory().addItem(diamonds);
        if (!leftover.isEmpty()) {
            // Falls Inventar voll, droppe die übrigen Diamanten
            leftover.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
        }
        return true;
    }

    /**
     * Formatiert einen Betrag mit Währungsname.
     */
    public static String format(int amount) {
        return amount + " " + getCurrencyName(amount);
    }
}
