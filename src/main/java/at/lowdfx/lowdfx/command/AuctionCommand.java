package at.lowdfx.lowdfx.command;

import at.lowdfx.lowdfx.LowdFX;
import at.lowdfx.lowdfx.command.util.CommandHelp;
import at.lowdfx.lowdfx.kit.Items;
import at.lowdfx.lowdfx.managers.AuctionManager;
import at.lowdfx.lowdfx.util.Configuration;
import at.lowdfx.lowdfx.util.EconomyHandler;
import at.lowdfx.lowdfx.util.MessagedException;
import at.lowdfx.lowdfx.util.Perms;
import at.lowdfx.lowdfx.util.Utilities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import at.lowdfx.lowdfx.util.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class AuctionCommand {

    static {
        CommandHelp.register("ah",
                MiniMessage.miniMessage().deserialize("/help ah"),
                MiniMessage.miniMessage().deserialize(
                        "<gray>Auktionshaus - Kaufe und verkaufe Items.<newline>" +
                                "<yellow>· /ah</yellow> <gray>- Öffnet das Auktionshaus<newline>" +
                                "<yellow>· /ah sell <preis></yellow> <gray>- Verkaufe Item in der Hand<newline>" +
                                "<yellow>· /ah cancel</yellow> <gray>- Zeigt deine Auktionen zum Stornieren<newline>" +
                                "<yellow>· /ah my</yellow> <gray>- Zeigt deine aktiven Auktionen</gray>"),
                null,
                Perms.Perm.AUCTION.getPermission(),
                null);
    }

    public static LiteralCommandNode<CommandSourceStack> command() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("ah")
                .requires(source -> Perms.check(source, Perms.Perm.AUCTION) && source.getExecutor() instanceof Player)
                // /ah - Öffnet das Auktionshaus
                .executes(context -> {
                    if (!(context.getSource().getExecutor() instanceof Player player))
                        return 1;

                    if (!Configuration.AUCTION_ENABLED) {
                        player.sendMessage(LowdFX.serverMessage(Component.text("Das Auktionshaus ist deaktiviert!", NamedTextColor.RED)));
                        Utilities.negativeSound(player);
                        return 1;
                    }

                    openAuctionGui(player);
                    return 1;
                })
                // /ah sell <preis>
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("sell")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("price", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    if (!(context.getSource().getExecutor() instanceof Player player))
                                        return 1;

                                    int price = context.getArgument("price", Integer.class);
                                    ItemStack item = player.getInventory().getItemInMainHand();

                                    if (item.isEmpty()) {
                                        player.sendMessage(LowdFX.serverMessage(Component.text("Du musst ein Item in der Hand halten!", NamedTextColor.RED)));
                                        Utilities.negativeSound(player);
                                        return 1;
                                    }

                                    try {
                                        AuctionManager.Auction auction = AuctionManager.createAuction(player.getUniqueId(), item.clone(), price);
                                        player.getInventory().setItemInMainHand(null);
                                        player.sendMessage(LowdFX.serverMessage(Component.text("Auktion erstellt für " + EconomyHandler.format(price) + "!", NamedTextColor.GREEN)));
                                        Utilities.positiveSound(player);
                                    } catch (MessagedException e) {
                                        player.sendMessage(LowdFX.serverMessage(Component.text(e.getMessage(), NamedTextColor.RED)));
                                        Utilities.negativeSound(player);
                                    }
                                    return 1;
                                })
                        )
                        // Hilfe bei /ah sell ohne Preis
                        .executes(context -> {
                            if (context.getSource().getExecutor() instanceof Player player) {
                                showHelp(player);
                            }
                            return 1;
                        })
                )
                // /ah my - Zeigt eigene Auktionen
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("my")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player))
                                return 1;

                            openMyAuctionsGui(player);
                            return 1;
                        })
                )
                // /ah cancel - Storniert eine Auktion
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("cancel")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player))
                                return 1;

                            openMyAuctionsGui(player);
                            return 1;
                        })
                )
                .build();
    }

    private static void showHelp(Player player) {
        player.sendMessage(LowdFX.serverMessage(Component.text("Auktionshaus Befehle:", NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("/ah", NamedTextColor.GOLD).append(Component.text(" - Öffnet das Auktionshaus", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/ah sell <preis>", NamedTextColor.GOLD).append(Component.text(" - Verkaufe Item in der Hand", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/ah my", NamedTextColor.GOLD).append(Component.text(" - Zeigt deine Auktionen", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/ah cancel", NamedTextColor.GOLD).append(Component.text(" - Storniert eine Auktion", NamedTextColor.GRAY)));
    }

    private static void openAuctionGui(Player player) {
        List<AuctionManager.Auction> auctions = AuctionManager.getAllAuctions();
        List<Item> items = new ArrayList<>();

        for (AuctionManager.Auction auction : auctions) {
            items.add(new AuctionItem(auction, player, false));
        }

        if (items.isEmpty()) {
            ItemStack noAuctionItem = new ItemBuilder(Material.BARRIER)
                    .name(Component.text("Keine Auktionen", NamedTextColor.RED))
                    .lore(List.of(Component.text("Es sind aktuell keine Auktionen verfügbar.", NamedTextColor.GRAY)))
                    .build();
            items.add(new SimpleItem(noAuctionItem));
        }

        PagedGui<Item> gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < # > # # #")
                .addIngredient('#', Items.BLACK_BACKGROUND)
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new PageBackItem())
                .addIngredient('>', new PageForwardItem())
                .setContent(items)
                .build();

        Window.single()
                .setGui(gui)
                .setTitle("Auktionshaus")
                .open(player);
    }

    private static void openMyAuctionsGui(Player player) {
        List<AuctionManager.Auction> auctions = AuctionManager.getPlayerAuctions(player.getUniqueId());
        List<Item> items = new ArrayList<>();

        for (AuctionManager.Auction auction : auctions) {
            items.add(new AuctionItem(auction, player, true));
        }

        if (items.isEmpty()) {
            ItemStack noAuctionItem = new ItemBuilder(Material.BARRIER)
                    .name(Component.text("Keine Auktionen", NamedTextColor.RED))
                    .lore(List.of(Component.text("Du hast keine aktiven Auktionen.", NamedTextColor.GRAY)))
                    .build();
            items.add(new SimpleItem(noAuctionItem));
        }

        PagedGui<Item> gui = PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < # > # # #")
                .addIngredient('#', Items.BLACK_BACKGROUND)
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new PageBackItem())
                .addIngredient('>', new PageForwardItem())
                .setContent(items)
                .build();

        Window.single()
                .setGui(gui)
                .setTitle("Deine Auktionen")
                .open(player);
    }

    // ==================== Items ====================

    private static class AuctionItem extends AbstractItem {
        private final AuctionManager.Auction auction;
        private final Player viewer;
        private final boolean isOwner;

        public AuctionItem(AuctionManager.Auction auction, Player viewer, boolean isOwner) {
            this.auction = auction;
            this.viewer = viewer;
            this.isOwner = isOwner;
        }

        @Override
        public ItemProvider getItemProvider() {
            ItemStack item = auction.item();
            ItemMeta meta = item.getItemMeta();

            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.lore());
            }
            lore.add(Component.empty());
            lore.add(Component.text("Preis: " + EconomyHandler.format(auction.price()), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Verkäufer: " + Bukkit.getOfflinePlayer(auction.seller()).getName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

            long remaining = auction.remainingSeconds();
            String timeText = formatTime(remaining);
            lore.add(Component.text("Verbleibend: " + timeText, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            if (isOwner) {
                lore.add(Component.text("Klicke zum Stornieren", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            } else if (!auction.seller().equals(viewer.getUniqueId())) {
                lore.add(Component.text("Klicke zum Kaufen", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);

            return new xyz.xenondevs.invui.item.builder.ItemBuilder(item);
        }

        @Override
        public void handleClick(ClickType clickType, Player player, org.bukkit.event.inventory.InventoryClickEvent event) {
            if (isOwner) {
                // Stornieren
                try {
                    AuctionManager.cancelAuction(player, auction.id());
                    player.sendMessage(LowdFX.serverMessage(Component.text("Auktion storniert! Item wurde zurückgegeben.", NamedTextColor.GREEN)));
                    Utilities.positiveSound(player);
                    player.closeInventory();
                    openMyAuctionsGui(player);
                } catch (MessagedException e) {
                    player.sendMessage(LowdFX.serverMessage(Component.text(e.getMessage(), NamedTextColor.RED)));
                    Utilities.negativeSound(player);
                }
            } else if (!auction.seller().equals(player.getUniqueId())) {
                // Kaufen
                try {
                    AuctionManager.buyAuction(player, auction.id());
                    player.sendMessage(LowdFX.serverMessage(Component.text("Gekauft für " + EconomyHandler.format(auction.price()) + "!", NamedTextColor.GREEN)));
                    Utilities.positiveSound(player);
                    player.closeInventory();
                    openAuctionGui(player);
                } catch (MessagedException e) {
                    player.sendMessage(LowdFX.serverMessage(Component.text(e.getMessage(), NamedTextColor.RED)));
                    Utilities.negativeSound(player);
                }
            }
        }

        private String formatTime(long seconds) {
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m";
            if (seconds < 86400) return (seconds / 3600) + "h";
            return (seconds / 86400) + "d";
        }
    }

    private static class PageBackItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            ItemStack arrow = new ItemBuilder(Material.ARROW)
                    .name(Component.text("Zurück", NamedTextColor.WHITE))
                    .build();
            return new xyz.xenondevs.invui.item.builder.ItemBuilder(arrow);
        }

        @Override
        public void handleClick(ClickType clickType, Player player, org.bukkit.event.inventory.InventoryClickEvent event) {
            if (event.getInventory().getHolder() instanceof PagedGui<?> gui) {
                gui.goBack();
            }
        }
    }

    private static class PageForwardItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            ItemStack arrow = new ItemBuilder(Material.ARROW)
                    .name(Component.text("Weiter", NamedTextColor.WHITE))
                    .build();
            return new xyz.xenondevs.invui.item.builder.ItemBuilder(arrow);
        }

        @Override
        public void handleClick(ClickType clickType, Player player, org.bukkit.event.inventory.InventoryClickEvent event) {
            if (event.getInventory().getHolder() instanceof PagedGui<?> gui) {
                gui.goForward();
            }
        }
    }
}
