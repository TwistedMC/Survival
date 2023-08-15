package com.twistedmcstudios.survival.events;

import com.twistedmcstudios.core.Core;
import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.boosters.Booster;
import com.twistedmcstudios.core.cache.RankCache;
import com.twistedmcstudios.core.common.Module;
import com.twistedmcstudios.core.common.update.UpdateEvent;
import com.twistedmcstudios.core.common.update.UpdateType;
import com.twistedmcstudios.core.common.util.TickUnit;
import com.twistedmcstudios.core.common.util.UtilSound;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.database.ServerDatabase;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JoinManager extends Module {

    private JoinManager() {
        super("Join Manager");
    }

    private static final JoinManager instance = new JoinManager();

    public static JoinManager getInstance() {
        return instance;
    }

    @EventHandler
    public void PlayerLeave(PlayerQuitEvent e) {
        Account player = new Account(e.getPlayer());

        e.setQuitMessage(null);
        e.getPlayer().getActivePotionEffects().clear();

        if (player.getManager().getPreference("game_join_alert", 1) && !player.isStaff()) {
            Bukkit.broadcastMessage(c.dgray + "[" + c.red + "-" + c.dgray + "]" + " " + player.getManager().getRankName());
        }

        RankCache.invalidatePlayerRankCache(player.getUUID());
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent e) {

        e.setJoinMessage(null);

        Bukkit.getScheduler().runTaskLaterAsynchronously(Core.getPlugin(), () -> {

        }, TickUnit.toTicks(3, TimeUnit.SECONDS));


        Bukkit.getScheduler().runTaskLaterAsynchronously(Core.getPlugin(), () -> {
            Account player = new Account(e.getPlayer());

            if (ServerDatabase.getInstance().executeQuery("SELECT last_login FROM `accounts` WHERE uuid = '" + player.getUUID() + "'", "last_login") == null) {
                Bukkit.broadcastMessage(ChatColor.of("#3498DB") + "Welcome " + player.getManager().getRankName() + ChatColor.of("#3498DB") + " to the server!");
            }

            if (player.getManager().getPreference("game_join_alert", 1)
                    && ServerDatabase.getInstance().executeQuery("SELECT last_login FROM `accounts` WHERE uuid = '" + player.getUUID() + "'", "last_login") != null
                    && !player.isStaff()) {
                Bukkit.broadcastMessage(c.dgray + "[" + c.green + "+" + c.dgray + "]" + " " + player.getManager().getRankName());
            }
        }, TickUnit.toTicks(1, TimeUnit.SECONDS));
    }

    @EventHandler
    public void onPlayerJoinBooster(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Booster.getInstance().isGlobalBoosterActive()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(Core.getPlugin(), () -> {
                Account account = new Account(player);
                player.sendMessage("");
                player.sendMessage(
                        c.green + "➲ " + c.white + "A " + c.aqua + Booster.getInstance().getGlobalBoosterMultiplier() + "x " + c.aqua + Booster.getInstance().getGlobalBoosterType().getName() + c.aqua + " Boost " + c.white + "is currently active by " + c.aqua + Bukkit.getOfflinePlayer(UUID.fromString(Booster.getInstance().getGlobalBoosterActivator())).getName() + c.white + "!");
                player.sendMessage("");
                account.playSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 10f, 1.0f);
            }, TickUnit.toTicks(10, TimeUnit.SECONDS));
        }
    }

    @EventHandler
    public void onSpecialOffers(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        /*Bukkit.getScheduler().runTaskLaterAsynchronously(Core.getPlugin(), () -> {
            Account account = new Account(player);
            player.sendMessage("");
            player.sendMessage(
                    c.green + "➲ " + c.white + c.bold + "SPECIAL OFFER! " + c.dgreen + "3,500 Gems" + c.white + " for " + c.aqua + "16.99 USD" + c.white + "!");
            player.sendMessage(
                    c.aqua + "https://store.twistedmc.net/category/gems");
            player.sendMessage("");
            UtilSound.jingleChimeSound(player);
        }, TickUnit.toTicks(3, TimeUnit.SECONDS));*/

        Bukkit.getScheduler().runTaskLaterAsynchronously(Core.getPlugin(), () -> {
            Account account = new Account(player);
            player.sendMessage("");
            player.sendMessage("");
            player.sendMessage(c.purple + "\uD83D\uDFCC " + c.white + c.bold + "SURVIVAL v0.0.1 " + c.green + "Mob Upgrades" + c.white + ", " + ChatColor.of("#6effca") + "Updated Life System" + c.white + " and more!");
            player.sendMessage("");
            player.sendMessage(c.gray + "Major features include enhanced mob behavior, updated lives system with unique effects, and much more!");
            player.sendMessage(c.white + "Read more here: " + c.aqua + "https://twistedmc.net/post/10-survival-(v0.0.1)---update-log/");
            player.sendMessage("");
            player.sendMessage("");

            UtilSound.jingleSound(player);
        }, TickUnit.toTicks(3, TimeUnit.SECONDS));

    }

}
