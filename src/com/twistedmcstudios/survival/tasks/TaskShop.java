package com.twistedmcstudios.survival.tasks;

import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.account.stats.Stat;
import com.twistedmcstudios.core.common.gui.AbstractGUI;
import com.twistedmcstudios.core.common.gui.ScrollerInventory;
import com.twistedmcstudios.core.common.gui.item.cItemStack;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.ingamestore.*;
import com.twistedmcstudios.core.ingamestore.cache.StoreCache;
import com.twistedmcstudios.core.ingamestore.confirmpurchase.ConfirmPurchaseMenu;
import com.twistedmcstudios.core.ingamestore.gemstore.category.games.SurvivalGemStore;
import com.twistedmcstudios.core.modifiers.SurvivalModifiers;
import com.twistedmcstudios.core.reboot.RebootManager;
import com.twistedmcstudios.survival.scoreboard.PlayerManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TaskShop extends AbstractGUI {


    /*
    00 01 02 03 04 05 06 07 08
    09 10 11 12 13 14 15 16 17
    18 19 20 21 22 23 24 25 26
    27 28 29 30 31 32 33 34 35
    36 37 38 39 40 41 42 43 44
    45 46 47 48 49 50 51 52 53
    */

    public TaskShop(Player player) {
        super(6, "Points Shop");

        NumberFormat formatter = NumberFormat.getIntegerInstance();

        Account _player = new Account(player);

        if (RebootManager.timeRemaining <= 300) {
            player.sendMessage(c.red + "Instance rebooting soon! Shops are disabled.");
            _player.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 10, 0.7F);
            player.closeInventory();
            return;
        }

        ZoneId estZoneId = ZoneId.of("America/New_York");
        LocalDate dateToCheck = LocalDate.now(estZoneId);

        LocalDate startRange = LocalDate.of(dateToCheck.getYear(), 7, 17);
        LocalDate endRange = LocalDate.of(dateToCheck.getYear(), 8, 17);

        if (dateToCheck.isAfter(endRange) ) {
            player.sendMessage(c.red + "Season over! Join our Discord for updates: discord.gg/twistedmc");
            _player.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 10, 0.7F);
            player.closeInventory();
            return;
        }

        LocalDateTime targetDateTime = LocalDateTime.of(2023, 8, 17, 23, 59, 59);
        String remainingTime = PlayerManager.getTimeRemaining(targetDateTime);

        _player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10, 1.0f);

        List<StoreItem> storeItems = StoreCache.getStoreItems(StoreCategory.SURVIVAL_TASK_POINTS_SHOP);

        if (storeItems.size() >= 1) {
            int maxPerPage = 10;

            for (int i = 0; i < storeItems.size(); i++) {
                StoreItem storeItem = storeItems.get(i);

                String _storeItem;
                int _storeItemPrice = storeItem.getPrice();
                String _storeItemSalePrice = storeItem.getSale();
                int _storeItemNewPrice = InGameStore.subtractPercentage(_storeItemPrice, _storeItemSalePrice);
                String _storeItemB;

                if (storeItem.getSaleActive() == 1) {
                    _storeItem = c.gray + "Cost: " + c.red + c.strike + formatter.format(_storeItemPrice)
                            + c.reset
                            + " "
                            + ChatColor.of("#6effca") + formatter.format(_storeItemNewPrice) + " Points" +
                            " " + c.red + c.bold +
                            InGameStore.calculateDiscountPercentage(
                                    _storeItemPrice,
                                    _storeItemNewPrice
                            ) + "% OFF";
                    _storeItemPrice = _storeItemNewPrice;
                } else {
                    _storeItem = c.gray + "Cost: " + ChatColor.of("#6effca") + NumberFormat.getIntegerInstance().format(storeItem.getPrice()) + " Points";
                    _storeItemPrice = InGameStore.subtractPercentage(_storeItemPrice, _storeItemSalePrice);
                }

                if (storeItem.getItem().endsWith("_premium")) {
                    try {
                        if (_player.getManager().doesPlayerAlreadyHaveItem(storeItem.getItem())) {
                            _storeItemB = c.red + "Already purchased!";
                        } else if (_player.getManager().hasSeasonPass()) {
                            if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= _storeItemPrice) {
                                _storeItemB = c.yellow + "Click to purchase!";
                            } else {
                                _storeItemB = c.red + "Not enough Points in your wallet!";
                            }
                        } else {
                            _storeItemB = c.red + "Season Pass required!";
                        }
                    } catch (SQLException | ExecutionException e) {
                        player.closeInventory();
                        player.sendMessage(c.red + "Huh, an error occurred. Please try again later and contact an admin if this keeps happening.");
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        if (_player.getManager().doesPlayerAlreadyHaveItem(storeItem.getItem())) {
                            _storeItemB = c.red + "Already purchased!";
                        } else if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= _storeItemPrice) {
                            _storeItemB = c.yellow + "Click to purchase!";
                        } else {
                            _storeItemB = c.red + "Not enough Points in your wallet!";
                        }
                    } catch (SQLException | ExecutionException e) {
                        player.closeInventory();
                        player.sendMessage(c.red + "Huh, an error occurred. Please try again later and contact an admin if this keeps happening.");
                        throw new RuntimeException(e);
                    }
                }


                int final_storeItemPrice = _storeItemPrice;
                if (storeItem.getItem().startsWith("POTION")) {

                    String[] parts = storeItem.getItem().split("_", 2); // Limit to split into 2 parts

                    String[] subParts = parts[1].split("\\.");

                    String action = subParts[0];
                    String duration = subParts[1];
                    String level = subParts[2];
                    String color = subParts[3];

                    setItem(new cItemStack(Material.POTION).setDisplayName(storeItem.getDisplay())
                            .SetEffect(PotionEffectType.getByName(action), 20 * Integer.parseInt(duration), Integer.parseInt(level), getColorFromString(color.toLowerCase()))
                            .addLore(c.dgray + (storeItem.getItem().startsWith("PREMIUM") ? "Premium Reward" : "Free Reward"), "")
                            .addFancyLore(storeItem.getDescription(), c.gray)
                            .addLore("",
                                    _storeItem,
                                    "")
                            .addFancyLore("Important notice: All items can only be purchased once! " +
                                    "Choose wisely, and ensure you're making the best decision before confirming your purchase.", c.red)
                            .addLore("", _storeItemB).addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS), i % maxPerPage, (p) -> {

                        try {
                            if (_player.getManager().doesPlayerAlreadyHaveItem(storeItem.getItem())) {
                                player.sendMessage(c.red + "Already purchased this item!");
                            } else if (RebootManager.timeRemaining <= 300) {
                                player.sendMessage(c.red + "Instance rebooting soon! Shops are disabled.");
                                _player.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 10, 0.7F);
                                player.closeInventory();
                            } else if (storeItem.getItem().startsWith("PREMIUM")) {
                                if (_player.getManager().hasSeasonPass()) {
                                    if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= final_storeItemPrice) {
                                        ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(player, storeItem.getItem(), storeItem.getDisplay(), 1, final_storeItemPrice, StoreType.TASK_POINTS_SHOP);
                                        menu.openInventory(player);
                                    } else {
                                        _player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0.7F);
                                        player.closeInventory();
                                        player.sendMessage(c.red + "You do not have enough " + ChatColor.of("#6effca") + "Points" + c.red + " in your wallet!");
                                    }
                                } else {
                                    _player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10, 1.0f);
                                    player.closeInventory();
                                    player.sendMessage(c.green + "Purchase Season Pass: " + c.aqua + "https://store.twistedmc.net/category/offers");
                                }
                            } else {
                                if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= final_storeItemPrice) {
                                    ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(player, storeItem.getItem(), storeItem.getDisplay(), 1, final_storeItemPrice, StoreType.TASK_POINTS_SHOP);
                                    menu.openInventory(player);
                                } else {
                                    _player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0.7F);
                                    player.closeInventory();
                                    player.sendMessage(c.red + "You do not have enough " + ChatColor.of("#6effca") + "Points" + c.red + " in your wallet!");
                                }
                            }
                        } catch (ExecutionException e) {
                            player.closeInventory();
                            player.sendMessage(c.red + "Huh, an error occurred. Please try again later and contact an admin if this keeps happening.");
                            throw new RuntimeException(e);
                        }
                    });
                } else if (storeItem.getItem().startsWith("PREMIUMPOTION")) {

                    String[] parts = storeItem.getItem().split("_", 2); // Limit to split into 2 parts

                    String[] subParts = parts[1].split("\\.");

                    String action = subParts[0];
                    String duration = subParts[1];
                    String level = subParts[2];
                    String color = subParts[3];

                    setItem(new cItemStack(Material.POTION).setDisplayName(storeItem.getDisplay())
                            .SetEffect(PotionEffectType.getByName(action), 20 * Integer.parseInt(duration), Integer.parseInt(level), getColorFromString(color.toLowerCase()))
                            .addLore(c.dgray + (storeItem.getItem().startsWith("PREMIUM") ? "Premium Reward" : "Free Reward"), "")
                            .addFancyLore(storeItem.getDescription(), c.gray)
                            .addLore("",
                                    _storeItem,
                                    "")
                            .addFancyLore("Important notice: All items can only be purchased once! " +
                                    "Choose wisely, and ensure you're making the best decision before confirming your purchase.", c.red)
                            .addLore("", _storeItemB).addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS), i % maxPerPage, (p) -> {

                        try {
                            if (_player.getManager().doesPlayerAlreadyHaveItem(storeItem.getItem())) {
                                player.sendMessage(c.red + "Already purchased this item!");
                            } else if (RebootManager.timeRemaining <= 300) {
                                player.sendMessage(c.red + "Instance rebooting soon! Shops are disabled.");
                                _player.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 10, 0.7F);
                                player.closeInventory();
                            } else if (storeItem.getItem().startsWith("PREMIUM")) {
                                if (_player.getManager().hasSeasonPass()) {
                                    if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= final_storeItemPrice) {
                                        ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(player, storeItem.getItem(), storeItem.getDisplay(), 1, final_storeItemPrice, StoreType.TASK_POINTS_SHOP);
                                        menu.openInventory(player);
                                    } else {
                                        _player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0.7F);
                                        player.closeInventory();
                                        player.sendMessage(c.red + "You do not have enough " + ChatColor.of("#6effca") + "Points" + c.red + " in your wallet!");
                                    }
                                } else {
                                    _player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10, 1.0f);
                                    player.closeInventory();
                                    player.sendMessage(c.green + "Purchase Season Pass: " + c.aqua + "https://store.twistedmc.net/category/offers");
                                }
                            } else {
                                if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= final_storeItemPrice) {
                                    ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(player, storeItem.getItem(), storeItem.getDisplay(), 1, final_storeItemPrice, StoreType.TASK_POINTS_SHOP);
                                    menu.openInventory(player);
                                } else {
                                    _player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0.7F);
                                    player.closeInventory();
                                    player.sendMessage(c.red + "You do not have enough " + ChatColor.of("#6effca") + "Points" + c.red + " in your wallet!");
                                }
                            }
                        } catch (ExecutionException e) {
                            player.closeInventory();
                            player.sendMessage(c.red + "Huh, an error occurred. Please try again later and contact an admin if this keeps happening.");
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    setItem(new cItemStack(storeItem.getMaterial()).setDisplayName(storeItem.getDisplay())
                            .addLore(c.dgray + (storeItem.getItem().endsWith("_premium") ? "Premium Reward" : "Free Reward"), "")
                            .addFancyLore(storeItem.getDescription(), c.gray)
                            .addLore("",
                                    _storeItem,
                                    "")
                            .addFancyLore("Important notice: All items can only be purchased once! " +
                                    "Choose wisely, and ensure you're making the best decision before confirming your purchase.", c.red)
                            .addLore("", _storeItemB).addFlags(ItemFlag.HIDE_ATTRIBUTES), i % maxPerPage, (p) -> {

                        try {
                            if (_player.getManager().doesPlayerAlreadyHaveItem(storeItem.getItem())) {
                                player.sendMessage(c.red + "Already purchased this item!");
                            } else if (RebootManager.timeRemaining <= 300) {
                                player.sendMessage(c.red + "Instance rebooting soon! Shops are disabled.");
                                _player.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 10, 0.7F);
                                player.closeInventory();
                            } else if (storeItem.getItem().endsWith("_premium")) {
                                if (_player.getManager().hasSeasonPass()) {
                                    if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= final_storeItemPrice) {
                                        ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(player, storeItem.getItem(), storeItem.getDisplay(), 1, final_storeItemPrice, StoreType.TASK_POINTS_SHOP);
                                        menu.openInventory(player);
                                    } else {
                                        _player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0.7F);
                                        player.closeInventory();
                                        player.sendMessage(c.red + "You do not have enough " + ChatColor.of("#6effca") + "Points" + c.red + " in your wallet!");
                                    }
                                } else {
                                    _player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10, 1.0f);
                                    player.closeInventory();
                                    player.sendMessage(c.green + "Purchase Season Pass: " + c.aqua + "https://store.twistedmc.net/category/offers");
                                }
                            } else {
                                if (_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS) >= final_storeItemPrice) {
                                    ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(player, storeItem.getItem(), storeItem.getDisplay(), 1, final_storeItemPrice, StoreType.TASK_POINTS_SHOP);
                                    menu.openInventory(player);
                                } else {
                                    _player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0.7F);
                                    player.closeInventory();
                                    player.sendMessage(c.red + "You do not have enough " + ChatColor.of("#6effca") + "Points" + c.red + " in your wallet!");
                                }
                            }
                        } catch (ExecutionException e) {
                            player.closeInventory();
                            player.sendMessage(c.red + "Huh, an error occurred. Please try again later and contact an admin if this keeps happening.");
                            throw new RuntimeException(e);
                        }
                    });
                }




            }
        }

        // OTHER //

        setStaticItem(new cItemStack(Material.AMETHYST_CLUSTER).setDisplayName(c.purple + "Season Pass")
                .addFancyLore("Purchase the Season Pass to unlock access to our " +
                "Premium Rewards Section loaded with high-value items, exclusive cosmetics, and powerful enhancements.", c.white)
                .addLore("")
                .addLore(c.gray + "Season ends " + c.white + remainingTime)
                .addEnchant(Enchantment.DURABILITY, 1).addFlags(ItemFlag.HIDE_ENCHANTS),48, (p) -> {
            _player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10, 1.0f);
            player.closeInventory();
            player.sendMessage(c.green + "Purchase Season Pass: " + c.aqua + "https://store.twistedmc.net/category/offers");
        });

        setStaticItem(new cItemStack(Material.BARRIER).setDisplayName(c.red + "Close"),49, (p) -> {
            player.closeInventory();
        });

        setStaticItem(new cItemStack(Material.PRISMARINE_SHARD).setDisplayName(ChatColor.of("#6effca") + "Points Wallet: " + ChatColor.of("#c9ffeb") + NumberFormat.getIntegerInstance().format(_player.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS)))
                        .addLore(c.gray + "The " + ChatColor.of("#6effca") + "Points Wallet " + c.gray + "contains",
                                c.gray + "currency you can use in this ",
                                c.gray + "menu to purchase items. You can",
                                c.gray + "earn currency for your",
                                c.gray + "Points wallet by " + c.aqua + "completing tasks",
                                c.gray + "with /tasks.")
                        .addLore("")
                        .addLore(c.gray + "Season ends " + c.white + remainingTime)
                ,4, (p) -> {
                    _player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10, 1.0f);
                    player.closeInventory();
                    player.sendMessage(c.green + "Earn currency for your " + ChatColor.of("#6effca") + "Points Wallet" + c.green + " by " + c.aqua + "completing tasks" + c.green + " with /tasks.");
                });

        ScrollerInventory scrollerInventory = ScrollerInventory.getPlayerInstance(getInventoryTitle(), this, _player.getPlayer(), storeItems.size());
        scrollerInventory.openForPlayer(_player.getPlayer());

    }

    public int getNumberOfPages(int numberOfItems, int itemsPerPage) {
        return (int) Math.ceil(numberOfItems / (double) itemsPerPage);
    }

    public static Color getColorFromString(String colorString) {
        return switch (colorString.toLowerCase()) {
            case "aqua", "cyan" -> Color.AQUA;
            case "black" -> Color.BLACK;
            case "blue" -> Color.BLUE;
            case "fuchsia", "magenta" -> Color.FUCHSIA;
            case "gray", "grey" -> Color.GRAY;
            case "green" -> Color.GREEN;
            case "lime" -> Color.LIME;
            case "maroon" -> Color.MAROON;
            case "navy" -> Color.NAVY;
            case "olive" -> Color.OLIVE;
            case "orange" -> Color.ORANGE;
            case "purple" -> Color.PURPLE;
            case "red" -> Color.RED;
            case "silver" -> Color.SILVER;
            case "teal" -> Color.TEAL;
            case "white" -> Color.WHITE;
            case "yellow" -> Color.YELLOW;
            // Add more color mappings as needed
            default ->
                // If the input is not recognized, return null or a default color
                    null;
        };
    }



}
