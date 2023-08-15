package com.twistedmcstudios.survival.soulstats;

import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.account.OfflineAccount;
import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.account.stats.PlayerStatistics;
import com.twistedmcstudios.core.account.stats.Stat;
import com.twistedmcstudios.core.common.Module;
import com.twistedmcstudios.core.common.recharge.Recharge;
import com.twistedmcstudios.core.common.update.UpdateType;
import com.twistedmcstudios.core.common.util.UtilScheduler;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.database.ServerDatabase;
import com.twistedmcstudios.core.ingamestore.confirmpurchase.PurchaseCompleteEvent;
import com.twistedmcstudios.core.punish.Punish;
import com.twistedmcstudios.survival.Survival;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.LibsDisguises;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Skull;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SoulAndStatsManager extends Module {

    private Map<UUID, PlayerStatistics> playerStatisticsMap = new HashMap<>();

    private SoulAndStatsManager() {
        super("Survival Manager");
    }

    private static final SoulAndStatsManager instance = new SoulAndStatsManager();

    public static SoulAndStatsManager getInstance() {
        return instance;
    }

    @Override
    public void Enable() {
        createTables();
        createDeaths();

        UtilScheduler.runEvery(UpdateType.SEC, this::Particles);
        UtilScheduler.runEvery(UpdateType.SEC, this::GiveEffects);
        UtilScheduler.runEvery(UpdateType.FAST, this::CheckExpiredPenanceTokens);

        UtilScheduler.runEvery(UpdateType.MIN_30, this::updatePlayerStatistics);

    }

    public static void createTables() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS survival_souls (" +
                "player_uuid VARCHAR(255) PRIMARY KEY," +
                "world VARCHAR(255)," +
                "x VARCHAR(255)," +
                "y VARCHAR(255)," +
                "z VARCHAR(255)," +
                "player_inventory BLOB," +
                "player_xp INT," +
                "death_time BIGINT," +
                "penance_expiration BIGINT," +
                "grace_period BIGINT DEFAULT 3600000," +
                "unlocked INT DEFAULT 0," +
                "claimed INT DEFAULT 1," +
                "claimed_uuid VARCHAR(255) NULL," +
                "free_home INT DEFAULT 1" +
                ")";

        try (Connection connection = ServerDatabase.getInstance().getConnection(); PreparedStatement stmt = connection.prepareStatement(createTableQuery)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createDeaths() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS survival_deaths (" +
                "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY UNIQUE KEY, " +
                "player_uuid VARCHAR(255)," +
                "world VARCHAR(255)," +
                "x VARCHAR(255)," +
                "y VARCHAR(255)," +
                "z VARCHAR(255)," +
                "player_inventory BLOB," +
                "player_xp INT," +
                "death_time BIGINT" +
                ")";

        try (Connection connection = ServerDatabase.getInstance().getConnection(); PreparedStatement stmt = connection.prepareStatement(createTableQuery)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setZombie(Player player) {
        DisguiseAPI.disguiseToAll(player, new MobDisguise(DisguiseType.ZOMBIE));
        DisguiseAPI.setViewDisguiseToggled(player, true);
    }

    private void setCompassTarget(Player player, Location location) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        compassMeta.setLodestoneTracked(false);
        compassMeta.setLodestone(location);
        compass.setItemMeta(compassMeta);
        player.getInventory().addItem(compass);
    }

    private void placeHead(Player player, Location location) {
        Block block = location.getBlock();
        block.setType(Material.PLAYER_HEAD);
        Skull skull = (Skull) block.getState();
        skull.setOwningPlayer(player);
        skull.update();

        Bukkit.getScheduler().scheduleSyncDelayedTask(Survival.getInstance(), skull::update, 20L);
    }


    private String getTimeRemaining(long deathTime, long gracePeriod) {
        long remainingTime = deathTime + gracePeriod - System.currentTimeMillis();
        if (remainingTime < 0) {
            return "0s";
        } else {
            long seconds = (remainingTime / 1000) % 60;
            long minutes = (remainingTime / (1000 * 60)) % 60;
            long hours = (remainingTime / (1000 * 60 * 60)) % 24;

            String time = "";
            if(hours > 0) {
                time += hours + "h ";
            }
            if(minutes > 0 || hours > 0) {
                time += minutes + "m ";
            }
            time += seconds + "s";
            return time;
        }
    }

    private String getTimeRemaining(String uuidString) {
        long deathTime = 0;
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT death_time FROM survival_souls WHERE player_uuid = ?")) {
            stmt.setString(1, uuidString);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    deathTime = rs.getLong("death_time");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long remainingMillis = deathTime + TimeUnit.HOURS.toMillis(1) - System.currentTimeMillis();
        return formatTime(remainingMillis);
    }

    private String get(String uuidString, String what) {
        String value = "";
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + what + " FROM survival_souls WHERE player_uuid = ?")) {
            stmt.setString(1, uuidString);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    value = rs.getString(what);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return value;
    }


    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        String time = "";
        if (hours > 0) {
            time += hours + "h ";
        }
        if (minutes > 0 || hours > 0) {
            time += minutes + "m ";
        }
        time += seconds + "s";

        return time;
    }


    private String formatTimePadding(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        String time = "";
        if (hours > 0) {
            time += String.format("%02dh ", hours);
        }
        if (minutes > 0 || hours > 0) {
            time += String.format("%02dm ", minutes);
        }
        time += String.format("%02ds", seconds);

        return time;
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Account account = new Account(player);

        ItemStack[] inventory = player.getInventory().getContents();
        byte[] inventoryData = new byte[0];
        try {
            inventoryData = itemStackArrayToByteArray(inventory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO survival_deaths (player_uuid, world, x, y, z, player_inventory, player_xp, death_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getLocation().getWorld().getName());
            stmt.setString(3, String.valueOf(player.getLocation().getX()));
            stmt.setString(4, String.valueOf(player.getLocation().getY()));
            stmt.setString(5, String.valueOf(player.getLocation().getZ()));
            stmt.setBytes(6, inventoryData);
            stmt.setInt(7, player.getLevel());
            stmt.setLong(8, System.currentTimeMillis());
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        account.getManager().incrementStat(Stat.SURVIVAL_DEATHS, 1, "Died", false, false);

        if (account.getManager().getStatInt(Stat.SURVIVAL_LIVES) > 0) {
            account.getManager().decrementStat(Stat.SURVIVAL_LIVES, 1, "Died");
        }

        int newLives = account.getManager().getStatInt(Stat.SURVIVAL_LIVES);

        if (newLives == 1) {
            if (isInDatabase(player)) {

                if (isClaimed(player.getUniqueId())) {
                    try (Connection conn = ServerDatabase.getInstance().getConnection();
                         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ?")) {
                        stmt.setString(1, player.getUniqueId().toString());
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                World world = Bukkit.getServer().getWorld(rs.getString("world"));
                                double x = Double.parseDouble(rs.getString("x"));
                                double y = Double.parseDouble(rs.getString("y"));
                                double z = Double.parseDouble(rs.getString("z"));
                                Location location = new Location(world, x, y, z);
                                setZombie(player);
                                setCompassTarget(player, location);

                                if (isClaimed(player.getUniqueId())) {
                                    player.sendMessage(c.dred + "☠ " + c.red + "Your items have been claimed, but you still need to retrieve your soul.");
                                    player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) x / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) z / 100 * 100) + c.yellow + " World: " + formatEnvironment(world.getEnvironment()));
                                } else {
                                    player.sendMessage(c.dred + "☠ " + c.red + "You have " + c.gold + getTimeRemaining(rs.getLong("death_time"), rs.getLong("grace_period")) + c.red + " to retrieve your soul before others can claim your items.");
                                    player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) x / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) z / 100 * 100) + c.yellow + " World: " + formatEnvironment(world.getEnvironment()));
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }


                    if (!Recharge.getInstance().use(player, "PenanceTokenNotify", 500, false, false)) {

                        TextComponent message = new TextComponent(TextComponent.fromLegacyText(c.yellow + "Having trouble retrieving your soul? "));

                        if (account.getManager().getStatInt(Stat.SURVIVAL_PENANCE_TOKEN) == 0) {
                            TextComponent clickMe = new TextComponent(TextComponent.fromLegacyText(c.gold + c.bold + "Click here"));
                            clickMe.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/buy penance_token"));

                            TextComponent restOfMessage = new TextComponent(
                                    TextComponent.fromLegacyText(
                                            c.yellow + " to use " + c.dgreen + "5 Gems" + c.yellow + " for a 24-hour ban instead. " +
                                                    "Your soul will be unlocked instantly and anyone will be able to retrieve your items.")
                            );

                            TextComponent whyGems = new TextComponent(TextComponent.fromLegacyText(c.aqua + "Why do I have to use Gems? "));

                            TextComponent explanation = new TextComponent(
                                    TextComponent.fromLegacyText(
                                            c.white + "You were given the option to choose a ban over finding your soul when you first died. " +
                                                    "This is a second chance, but it comes at a cost. " +
                                                    "The price is a way to balance the game and keep it fair for all players."));

                            message.addExtra(clickMe);
                            message.addExtra(restOfMessage);

                            player.spigot().sendMessage(message);
                            player.sendMessage("");
                            player.spigot().sendMessage(whyGems);
                            player.spigot().sendMessage(explanation);
                        } else {
                            TextComponent clickMe = new TextComponent(TextComponent.fromLegacyText(c.gold + c.bold + "Click here"));
                            clickMe.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/use penance_token"));

                            TextComponent restOfMessage = new TextComponent(
                                    TextComponent.fromLegacyText(
                                            c.yellow + " to use your Penance Token for a 24-hour ban instead. " +
                                                    "Your soul will be unlocked instantly and anyone will be able to retrieve your items."
                                    )
                            );

                            message.addExtra(clickMe);
                            message.addExtra(restOfMessage);

                            player.spigot().sendMessage(message);
                        }

                    }
                    return;
                }

                try (Connection conn = ServerDatabase.getInstance().getConnection();
                     PreparedStatement stmt = conn.prepareStatement("UPDATE survival_souls SET death_time = death_time - 600000 WHERE player_uuid = ? and claimed = '0'"); PreparedStatement stmt2 = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ?")) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.executeUpdate();

                    stmt2.setString(1, player.getUniqueId().toString());

                    try (ResultSet rs = stmt2.executeQuery()) {
                        if (rs.next()) {
                            World world = Bukkit.getServer().getWorld(rs.getString("world"));
                            double x = Double.parseDouble(rs.getString("x"));
                            double y = Double.parseDouble(rs.getString("y"));
                            double z = Double.parseDouble(rs.getString("z"));
                            Location location = new Location(world, x, y, z);
                            setZombie(player);
                            setCompassTarget(player, location);

                            if (isClaimed(player.getUniqueId())) {
                                player.sendMessage(c.dred + "☠ " + c.red + "Your items have been claimed, but you still need to retrieve your soul.");
                                player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) x / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) z / 100 * 100) + c.yellow + " World: " + formatEnvironment(world.getEnvironment()));
                            } else {
                                player.sendMessage(
                                        c.dred + "☠ " + c.red + "You died again! Your time to retrieve your soul before others can claim your items has decreased by " +
                                                c.red + c.bold + "10 minutes" + c.red + ".",
                                        c.red + "Time remaining: " + c.gold + c.bold + getTimeRemaining(player.getUniqueId().toString())
                                );
                                player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) x / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) z / 100 * 100) + c.yellow + " World: " + formatEnvironment(world.getEnvironment()));
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (!Recharge.getInstance().use(player, "PenanceTokenNotify", 500, false, false)) {

                    TextComponent message = new TextComponent(TextComponent.fromLegacyText(c.yellow + "Having trouble retrieving your soul? "));

                    if (account.getManager().getStatInt(Stat.SURVIVAL_PENANCE_TOKEN) == 0) {
                        TextComponent clickMe = new TextComponent(TextComponent.fromLegacyText(c.gold + c.bold + "Click me"));
                        clickMe.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/buy penance_token"));
                        clickMe.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(c.purple + "Buy Penance Token")));

                        TextComponent restOfMessage = new TextComponent(
                                TextComponent.fromLegacyText(
                                        c.yellow + " to use " + c.dgreen + "5 Gems" + c.yellow + " for a 24-hour ban instead. " +
                                                "Your soul will be unlocked instantly and anyone will be able to retrieve your items.")
                        );

                        TextComponent whyGems = new TextComponent(TextComponent.fromLegacyText(c.aqua + "Why do I have to use Gems? "));

                        TextComponent explanation = new TextComponent(
                                TextComponent.fromLegacyText(
                                        c.white + "You were given the option to choose a ban over finding your soul when you first died. " +
                                                "This is a second chance, but it comes at a cost. " +
                                                "The price is a way to balance the game and keep it fair for all players."));

                        message.addExtra(clickMe);
                        message.addExtra(restOfMessage);

                        player.spigot().sendMessage(message);
                        player.sendMessage("");
                        player.spigot().sendMessage(whyGems);
                        player.spigot().sendMessage(explanation);
                    } else {
                        TextComponent clickMe = new TextComponent(TextComponent.fromLegacyText(c.gold + c.bold + "Click here"));
                        clickMe.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/use penance_token"));
                        clickMe.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(c.purple + "Use Penance Token")));

                        TextComponent restOfMessage = new TextComponent(
                                TextComponent.fromLegacyText(
                                        c.yellow + " to use your Penance Token for a 24-hour ban instead. " +
                                                "Your soul will be unlocked instantly and anyone will be able to retrieve your items."
                                )
                        );

                        message.addExtra(clickMe);
                        message.addExtra(restOfMessage);

                        player.spigot().sendMessage(message);
                    }

                }
                return;
            }

            Location deathLocation = player.getLocation();

            // Get the nearest non-water block.
            Location placeLocation = getNearestNonWaterBlock(deathLocation);

            // If there's no suitable location, generate a temporary platform rising from the death location.
            if (placeLocation == null) {
                World world = deathLocation.getWorld();
                int x = deathLocation.getBlockX();
                int y = deathLocation.getBlockY();
                int z = deathLocation.getBlockZ();
                Block block;

                // Iterate upwards from the death location until reaching a block that's not water or air.
                do {
                    y++;
                    block = world.getBlockAt(x, y, z);
                } while (block.getType() == Material.WATER || block.getType() == Material.AIR);

                // The block below the non-water or non-air block becomes the placement location.
                placeLocation = world.getBlockAt(x, y - 1, z).getLocation();
            }


            placeHead(player, placeLocation);

            long gracePeriod;

            if (account.getManager().getRank().equals(Rank.VIP)) {
                gracePeriod = TimeUnit.HOURS.toMillis(2);
            } else if (account.getManager().getRank().equals(Rank.VIP_PLUS)) {
                gracePeriod = TimeUnit.HOURS.toMillis(3);
            } else if (account.getManager().getRank().equals(Rank.PLATINUM)) {
                gracePeriod = TimeUnit.HOURS.toMillis(4);
            } else if (account.getManager().getRank().equals(Rank.ULTIMATE)) {
                gracePeriod = TimeUnit.HOURS.toMillis(5);
            } else if (account.getManager().getRank().equals(Rank.MEDIA)) {
                gracePeriod = TimeUnit.HOURS.toMillis(8);
            } else if (account.isStaff()) {
                gracePeriod = TimeUnit.HOURS.toMillis(24);
            } else {
                gracePeriod = TimeUnit.HOURS.toMillis(1);
            }

            String uuidString = player.getUniqueId().toString();

            event.getDrops().clear();
            event.setDroppedExp(0);

            EntityDamageEvent.DamageCause cause = player.getLastDamageCause().getCause();

            if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.LAVA) {
                Punish.getInstance().SurvivalBan(Punish.getInstance().generateRandomID(), player.getUniqueId(), "Death in unclaimable circumstances (lava or void). Your soul became inaccessible.", Punish.getInstance().formatToMillis("1d"));
                deleteSoul(player.getUniqueId());
            } else {

                try (Connection conn = ServerDatabase.getInstance().getConnection();
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO survival_souls (player_uuid, world, x, y, z, player_inventory, player_xp, death_time, penance_expiration, grace_period, unlocked, claimed, free_home) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, uuidString);
                    stmt.setString(2, placeLocation.getWorld().getName());
                    stmt.setString(3, String.valueOf(placeLocation.getX()));
                    stmt.setString(4, String.valueOf(placeLocation.getY()));
                    stmt.setString(5, String.valueOf(placeLocation.getZ()));
                    stmt.setBytes(6, inventoryData);
                    stmt.setInt(7, player.getLevel());
                    stmt.setLong(8, System.currentTimeMillis());
                    stmt.setLong(9, System.currentTimeMillis() + 180000);
                    stmt.setLong(10, gracePeriod);
                    stmt.setInt(11, 0);
                    stmt.setInt(12, 0);
                    stmt.setInt(13, 1);
                    stmt.execute();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                account.getManager().incrementStat(Stat.SURVIVAL_PENANCE_TOKEN, 1, "Death (1 Free Penance Token)", false, false);

                player.sendMessage(c.dred + "☠ " + c.red + "You have died! You have " + c.gold + getTimeRemaining(player.getUniqueId().toString()) + c.red + " to retrieve your soul before others can claim your items.");
                player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) placeLocation.getX() / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) placeLocation.getZ() / 100 * 100) + c.yellow + " World: " + formatEnvironment(placeLocation.getWorld().getEnvironment()));

                TextComponent message = new TextComponent("Don't want to have fun? ");
                message.setColor(ChatColor.YELLOW);

                TextComponent clickMe = new TextComponent("Click me");
                clickMe.setColor(ChatColor.GOLD);
                clickMe.setBold(true);
                clickMe.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/use penance_token"));
                clickMe.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(c.purple + "Use Penance Token")));

                TextComponent restOfMessage = new TextComponent(" to be banned for 24 hours instead. Your soul will be unlocked instantly and anyone will be able to retrieve your items. This option expires in 3 minutes.");
                restOfMessage.setColor(ChatColor.YELLOW);

                message.addExtra(clickMe);
                message.addExtra(restOfMessage);

                player.spigot().sendMessage(message);
            }
        } else {
            TextComponent message = new TextComponent(c.dred + "☠ " + c.red + "You died and lost a life! ");
            TextComponent clickHere = new TextComponent(c.gold + c.bold + "Click me");
            clickHere.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/buy DeathLocView"));
            clickHere.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(c.purple + "Get Death Location")));
            message.addExtra(clickHere);
            message.addExtra(c.yellow + " to get your death location by using " + c.dgreen + "5 Gems" + c.yellow + ".");
            player.spigot().sendMessage(message);

        }

    }

    @EventHandler
    public void onPurchaseComplete(PurchaseCompleteEvent event) {
        if (event.getItem().equals("DeathLocView")) {
            Location deathLocation = getMostRecentDeathLocation(event.getPlayer());
            event.getPlayer().sendMessage(c.yellow + "Your most recent death location is: " + formatLocation(deathLocation));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (isInDatabase(event.getPlayer())) {
            Location location = new Location(Bukkit.getWorld(get(uuid.toString(), "world")), Double.parseDouble(get(uuid.toString(), "x")), Double.parseDouble(get(uuid.toString(), "y")), Double.parseDouble(get(uuid.toString(), "z")));
            setZombie(event.getPlayer());
            setCompassTarget(event.getPlayer(), location);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isInDatabase(player)) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                Block block = event.getClickedBlock();
                Material material = block.getType();
                if (block.getState() instanceof Container) {
                    event.setCancelled(true);
                } else if (!material.isEdible() && material != Material.COMPASS && material != Material.PLAYER_HEAD) {
                    event.setCancelled(true);
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                Material itemMaterial = player.getInventory().getItemInMainHand().getType();
                if (!itemMaterial.isEdible() && itemMaterial != Material.COMPASS) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isInDatabase(player)) {
                Item item = event.getItem();
                Material material = item.getItemStack().getType();
                if (!material.isEdible()) {
                    event.setCancelled(true);
                }
            }
        }
    }



    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String uuidString = player.getUniqueId().toString();

        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ?")) {
            stmt.setString(1, uuidString);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {

                    if (isUnlocked(player.getUniqueId())) {
                        deleteSoul(player.getUniqueId());
                        return;
                    }

                    World world = Bukkit.getServer().getWorld(rs.getString("world"));
                    double x = Double.parseDouble(rs.getString("x"));
                    double y = Double.parseDouble(rs.getString("y"));
                    double z = Double.parseDouble(rs.getString("z"));
                    Location location = new Location(world, x, y, z);
                    setZombie(player);
                    setCompassTarget(player, location);

                    if (isClaimed(UUID.fromString(uuidString))) {
                        player.sendMessage(c.dred + "☠ " + c.red + "Your items have been claimed, but you still need to retrieve your soul.");
                        player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) x / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) z / 100 * 100) + c.yellow + " World: " + formatEnvironment(world.getEnvironment()));
                    } else if (isUnlocked(UUID.fromString(uuidString))) {
                        player.sendMessage(c.dred + "☠ " + c.red + "Your soul is now unlocked, but your items are still unclaimed.");
                        player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) x / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) z / 100 * 100) + c.yellow + " World: " + formatEnvironment(world.getEnvironment()));
                    } else {
                        player.sendMessage(c.dred + "☠ " + c.red + "You have " + c.gold + getTimeRemaining(rs.getLong("death_time"), rs.getLong("grace_period")) + c.red + " to retrieve your soul before others can claim your items.");
                        player.sendMessage(c.yellow + "Your soul is in the general area of " + c.gold + "X: " + ((int) x / 100 * 100) + c.yellow + ", " + c.gold + "Z: " + ((int) z / 100 * 100) + c.yellow + " World: " + formatEnvironment(world.getEnvironment()));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @EventHandler
    public void onCompassRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.COMPASS
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            try (Connection conn = ServerDatabase.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ?")) {
                stmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    World world = Bukkit.getServer().getWorld(rs.getString("world"));
                    double x = Double.parseDouble(rs.getString("x"));
                    double y = Double.parseDouble(rs.getString("y"));
                    double z = Double.parseDouble(rs.getString("z"));
                    Location location = new Location(world, x, y, z);
                    double distance = player.getLocation().distance(location);
                    player.sendMessage(c.yellow + "Your soul is approximately " + c.gold + (int) distance + c.yellow + " blocks away.");
                } else {
                    player.sendMessage(c.red + "You have no soul to locate.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockPlaceEvent event) {
        if (isNearSoulHead(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(c.red + "Sorry, you can't place blocks near a soul head.");
        } else {
            PlayerStatistics playerStatistics = playerStatisticsMap.computeIfAbsent(event.getPlayer().getUniqueId(), p -> new PlayerStatistics());
            playerStatistics.incrementStat(Stat.SURVIVAL_BLOCKS_PLACED, 1);
        }
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isNearSoulHead(event.getBlock().getLocation()) && event.getBlock().getType() != Material.PLAYER_HEAD) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(c.red + "Sorry, you can't break blocks near a soul head.");
        } else if (event.getBlock().getType() == Material.PLAYER_HEAD) {
            Player player = event.getPlayer();
            try (Connection conn = ServerDatabase.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls");
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    event.setCancelled(true);
                    String uuidString = rs.getString("player_uuid");
                    World world = Bukkit.getServer().getWorld(rs.getString("world"));
                    double x = Double.parseDouble(rs.getString("x"));
                    double y = Double.parseDouble(rs.getString("y"));
                    double z = Double.parseDouble(rs.getString("z"));
                    Location location = new Location(world, x, y, z);

                    double radius = 2.0;

                    if (Math.abs(location.getBlockX() - event.getBlock().getLocation().getBlockX()) <= radius &&
                            Math.abs(location.getBlockY() - event.getBlock().getLocation().getBlockY()) <= radius &&
                            Math.abs(location.getBlockZ() - event.getBlock().getLocation().getBlockZ()) <= radius) {
                        if (!UUID.fromString(uuidString).equals(player.getUniqueId())) {
                            if (System.currentTimeMillis() - rs.getLong("death_time") < rs.getLong("grace_period") && rs.getInt("unlocked") == 0) {
                                long deathTime = rs.getLong("death_time");
                                player.sendMessage(c.dred + "☠ " + c.red + "You can't claim this soul yet. " + c.gold + getTimeRemaining(deathTime, rs.getLong("grace_period")) + c.red + " remaining.");
                                return;
                            }
                        }
                        byte[] inventoryData = rs.getBytes("player_inventory");
                        if (!isClaimed(UUID.fromString(uuidString))) {

                            ItemStack[] inventory = itemStackArrayFromByteArray(inventoryData);
                            for (ItemStack item : inventory) {
                                if (item != null) {
                                    player.getLocation().getWorld().dropItemNaturally(location, item);
                                }
                            }

                            ExperienceOrb expOrb = (ExperienceOrb) player.getWorld().spawnEntity(location, EntityType.EXPERIENCE_ORB);
                            expOrb.setExperience(rs.getInt("player_xp"));

                        }
                        if (player.getUniqueId().equals(UUID.fromString(uuidString))) {
                            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM survival_souls WHERE player_uuid = ?")) {
                                deleteStmt.setString(1, uuidString);
                                deleteStmt.execute();
                            }
                            Account account = new Account(player);
                            account.getManager().incrementStat(Stat.SURVIVAL_LIVES, 1, "Claimed soul", false, false);
                            DisguiseAPI.undisguiseToAll(player);
                            event.getBlock().setType(Material.AIR);

                            // Calculate new weakness level and set player's max health
                            int deaths = account.getManager().getStatInt(Stat.SURVIVAL_DEATHS);
                            int weaknessLevel = deaths / 5;
                            double newMaxHealth = 20.0 - (2.0 * deaths); // Decrease max health by 1 heart (2.0 health) per death.
                            newMaxHealth = Math.max(newMaxHealth, 10.0); // Ensure health doesn't go below 5 hearts (10 health points).


                            if (weaknessLevel > 0) {
                                player.sendMessage(c.red + "You now have Weakness " + c.gold + RomanNumber.toRoman(weaknessLevel) + c.red + ".");
                            }

                            player.sendMessage(c.red + "You now have " + c.dred + (newMaxHealth / 2.0) + c.red + " hearts.");
                            player.sendMessage("");
                            player.sendMessage(c.yellow + "Remember: You can earn more lives by defeating other players or using your points in the Points Shop. The more lives you have, the fewer effects you'll experience!");

                        } else {
                            try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE `survival_souls` SET claimed = ?,claimed_uuid = ? WHERE player_uuid = ?")) {
                                updateStmt.setInt(1, 1);
                                updateStmt.setString(2, event.getPlayer().getUniqueId().toString());
                                updateStmt.setString(3, uuidString);
                                updateStmt.execute();
                            }
                        }
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            PlayerStatistics playerStatistics = playerStatisticsMap.computeIfAbsent(event.getPlayer().getUniqueId(), p -> new PlayerStatistics());
            playerStatistics.incrementStat(Stat.SURVIVAL_BLOCKS_BROKEN, 1);
        }
    }


    public boolean isInDatabase(Player player) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean isClaimed(UUID uuid) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ? and claimed = '1'")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean isUnlocked(UUID uuid) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ? and unlocked = '1'")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void claimSoul(UUID playerUUID) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE `survival_souls` SET claimed = ? WHERE player_uuid = ?")) {
            stmt.setInt(1, 1);
            stmt.setString(2, playerUUID.toString());
            stmt.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteSoul(UUID playerUUID) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM `survival_souls` WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID.toString());
            stmt.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void unlockSoul(UUID playerUUID) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE `survival_souls` SET unlocked = ? WHERE player_uuid = ?")) {
            stmt.setInt(1, 1);
            stmt.setString(2, playerUUID.toString());
            stmt.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


    public boolean isNearSoulHead(Location location) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                World world = Bukkit.getServer().getWorld(rs.getString("world"));
                double x = Double.parseDouble(rs.getString("x"));
                double y = Double.parseDouble(rs.getString("y"));
                double z = Double.parseDouble(rs.getString("z"));
                Location headLocation = new Location(world, x, y, z);

                if (location.getWorld().getName().equals(headLocation.getWorld().getName())) {
                    if (location.distance(headLocation) <= 3) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }


    public boolean FreeHome(Player player) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls WHERE player_uuid = ? and free_home = '1'")) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void Particles() {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                World world = Bukkit.getServer().getWorld(rs.getString("world"));
                double x = Double.parseDouble(rs.getString("x"));
                double y = Double.parseDouble(rs.getString("y"));
                double z = Double.parseDouble(rs.getString("z"));
                Location location = new Location(world, x, y, z);
                spawnParticles(location);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void spawnParticles(Location location) {
        World world = location.getWorld();
        if (world != null) {
            Location loc = location.add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.SCRAPE, loc, 5, 0.5, 0.5, 0.5, 0.1, null, true);
        }
    }

    public void CheckExpiredPenanceTokens() {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_souls")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                long expirationTime = rs.getLong("penance_expiration");

                if (System.currentTimeMillis() > expirationTime) {
                    OfflineAccount account = new OfflineAccount(playerUUID);
                    if (account.getManager().getStatInt(Stat.SURVIVAL_PENANCE_TOKEN) > 0) {
                        account.getManager().decrementStat(Stat.SURVIVAL_PENANCE_TOKEN, 1, "Penance Token Expired");

                        Player player = Bukkit.getPlayer(playerUUID);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(c.red + "Your Penance Token has expired.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public String formatEnvironment(World.Environment environment) {
        return switch (environment) {
            case NORMAL -> "Overworld";
            case NETHER -> "Nether";
            case THE_END -> "The End";
            default -> "Unknown";
        };
    }

    public byte[] itemStackArrayToByteArray(ItemStack[] items) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        dataOutput.writeInt(items.length);

        for (int i = 0; i < items.length; i++) {
            dataOutput.writeObject(items[i] != null ? items[i].serialize() : null);
        }

        dataOutput.close();
        return outputStream.toByteArray();
    }

    public ItemStack[] itemStackArrayFromByteArray(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack[] items = new ItemStack[dataInput.readInt()];

        // Read the serialized inventory
        for (int i = 0; i < items.length; i++) {
            Map<String, Object> itemMap = (Map<String, Object>) dataInput.readObject();
            if (itemMap != null) {
                items[i] = ItemStack.deserialize(itemMap);
            }
        }

        dataInput.close();
        return items;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getType() == Material.PLAYER_HEAD && isNearSoulHead(block.getLocation()));
    }

    public Location getMostRecentDeathLocation(Player player) {
        try (Connection conn = ServerDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM survival_deaths WHERE player_uuid = ? ORDER BY death_time DESC LIMIT 1")) {
            stmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String worldName = rs.getString("world");
                World world = Bukkit.getServer().getWorld(worldName);

                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");

                return new Location(world, x, y, z);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public String formatLocation(Location location) {
        if (location == null) {
            return c.red + "Unknown Location";
        }
        return c.gold + "X: " + String.format("%.2f", location.getX()) + c.yellow + ", " + c.gold + "Y: " + String.format("%.2f", location.getY()) + c.yellow + ", " + c.gold + "Z: " + String.format("%.2f", location.getZ()) + c.yellow + " World: " + formatEnvironment(location.getWorld().getEnvironment());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer != null) {

            PlayerStatistics playerStatistics = playerStatisticsMap.computeIfAbsent(killer.getUniqueId(), p -> new PlayerStatistics());

            if (entity.getType() == EntityType.PLAYER) {
                Account account = new Account(killer);

                account.getManager().incrementStat(Stat.SURVIVAL_LIVES, 1, "Killed player", true, false);

                playerStatistics.incrementStat(Stat.SURVIVAL_PLAYERS_KILLED, 1);

            } else {
                playerStatistics.incrementStat(Stat.SURVIVAL_MOBS_KILLED, 1);
            }
        }
    }

    public void updatePlayerStatistics(Player player) {
        PlayerStatistics playerStatistics = playerStatisticsMap.get(player.getUniqueId());

        try {
            for (Stat stat : playerStatistics.getStatistics().keySet()) {
                int value = playerStatistics.getStat(stat);
                Account account = new Account(player);
                account.getManager().incrementStat(stat, value, "Stat Update", false, false);
                playerStatisticsMap.remove(player.getUniqueId());
                account.getManager().sendActionBar(c.green + "Statistics successfully saved");
            }
        } catch (Exception e) {
            Account account = new Account(player);
            account.getManager().sendActionBar(c.red + "Failed to save your statistics. Nothing changed.");
        }
    }

    public void updatePlayerStatistics() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStatistics playerStatistics = playerStatisticsMap.get(player.getUniqueId());

            try {
                for (Stat stat : playerStatistics.getStatistics().keySet()) {
                    int value = playerStatistics.getStat(stat);
                    Account account = new Account(player);
                    account.getManager().incrementStat(stat, value, "Stat Update", false, false);
                    playerStatisticsMap.remove(player.getUniqueId());
                    account.getManager().sendActionBar(c.green + "Statistics successfully saved");
                }
            } catch (Exception e) {
                Account account = new Account(player);
                account.getManager().sendActionBar(c.red + "Failed to save your statistics. Nothing changed.");
            }
        }
    }

    public void GiveEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Account account = new Account(player);
            if (account.getManager().getStatInt(Stat.SURVIVAL_LIVES) == 1) {
                int deaths = account.getManager().getStatInt(Stat.SURVIVAL_DEATHS);
                if (!player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                    int weaknessLevel = deaths / 5;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, weaknessLevel, false, false, false));
                }
                double newMaxHealth = 20.0 - (4.0 * deaths);
                newMaxHealth = Math.max(newMaxHealth, 15.0);
                AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHealthAttribute != null && maxHealthAttribute.getBaseValue() != newMaxHealth) {
                    maxHealthAttribute.setBaseValue(newMaxHealth);
                }
            } else {
                AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHealthAttribute != null && maxHealthAttribute.getBaseValue() != 20.0) {
                    maxHealthAttribute.setBaseValue(20.0);
                }
            }
        }

    }

    public Location getNearestNonWaterBlock(Location deathLocation) {
        World world = deathLocation.getWorld();
        int startX = deathLocation.getBlockX();
        int startY = deathLocation.getBlockY();
        int startZ = deathLocation.getBlockZ();

        for (int r = 1; r < 50; r++) { // Check blocks within a radius of 50 blocks
            for (int x = startX - r; x <= startX + r; x++) {
                for (int y = startY - r; y <= startY + r; y++) {
                    for (int z = startZ - r; z <= startZ + r; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() != Material.WATER) {
                            return block.getLocation();
                        }
                    }
                }
            }
        }

        return null;
    }



}
