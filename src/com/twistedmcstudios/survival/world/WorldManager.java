package com.twistedmcstudios.survival.world;

import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.account.stats.Stat;
import com.twistedmcstudios.core.common.Module;
import com.twistedmcstudios.core.common.recharge.Recharge;
import com.twistedmcstudios.core.common.update.UpdateType;
import com.twistedmcstudios.core.common.util.LogManager;
import com.twistedmcstudios.core.common.util.UtilScheduler;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.database.ServerDatabase;
import com.twistedmcstudios.survival.Survival;
import com.twistedmcstudios.survival.soulstats.SoulAndStatsManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WorldManager extends Module {

    private WorldManager() {
        super("World Manager");
    }

    private static final WorldManager instance = new WorldManager();

    public static WorldManager getInstance() {
        return instance;
    }

    private final HashMap<UUID, Long> _playerJoinTime = new HashMap<>();

    private final Set<Location> defenseLocations = new HashSet<>();

    private final List<Location> launchLocations = Arrays.asList(
            new Location(Bukkit.getWorld("world"), -12424, 89, -232),
            new Location(Bukkit.getWorld("world"), -12421, 89, -232),
            new Location(Bukkit.getWorld("world"), -12419, 89, -232),
            new Location(Bukkit.getWorld("world"), -12418, 89, -232),
            new Location(Bukkit.getWorld("world"), -12417, 89, -234),
            new Location(Bukkit.getWorld("world"), -12419, 89, -234),
            new Location(Bukkit.getWorld("world"), -12421, 89, -234),
            new Location(Bukkit.getWorld("world"), -12423, 89, -234),
            new Location(Bukkit.getWorld("world"), -12423, 89, -236),
            new Location(Bukkit.getWorld("world"), -12421, 89, -236),
            new Location(Bukkit.getWorld("world"), -12419, 89, -236),
            new Location(Bukkit.getWorld("world"), -12417, 89, -236)
    );

    public List<BukkitRunnable> activeMissiles = new ArrayList<>();


    @Override
    public void Enable() {
        for (World world : Bukkit.getWorlds()) {

            world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 25);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);
            world.setDifficulty(Difficulty.HARD);

            LogManager.log(this, "World setup complete: " + world.getName());
        }

        createAirDefenseTables();
        loadDefenseLocations();

        UtilScheduler.runEvery(UpdateType.SEC, this::detectEntities);
        UtilScheduler.runEvery(UpdateType.MIN_05, this::RemoveWarnedPlayers);
    }

    public void loadDefenseLocations() {
        defenseLocations.clear();

        try (Connection conn = ServerDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT x, y, z FROM survival_defense")) {

            while (rs.next()) {
                defenseLocations.add(new Location(Bukkit.getServer().getWorld("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void TrackJoinTime(PlayerJoinEvent e) {
        _playerJoinTime.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void SaveTimeSpent(PlayerQuitEvent e) {
        Account player = new Account(e.getPlayer());
        int timeInGame = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - _playerJoinTime.get(player.getUUID()));
        player.getManager().incrementStat(Stat.SURVIVAL_PLAYTIME, timeInGame, "Time spent in-game update", false, false);
        SoulAndStatsManager.getInstance().updatePlayerStatistics(player.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Account account = new Account(event.getPlayer());
        if (event.getBlock().getType().equals(Material.DEEPSLATE)) {
            int xp = (int) Math.ceil(1.5);

            event.setExpToDrop(xp);
        } else if (event.getBlock().getType().equals(Material.NETHERRACK)) {
            double randomNumber = Math.random();
            if (randomNumber < 0.1) {
                int xp = (int) Math.ceil(0.5);

                event.setExpToDrop(xp);
            }
        } else if (event.getBlock().getType().equals(Material.DIRT) || event.getBlock().getType().equals(Material.SAND)) {
            double randomNumber = Math.random();
            if (randomNumber < 0.1) {
                int xp = (int) Math.ceil(0.5);

                event.setExpToDrop(xp);
            }
        }
    }

    // Air defense

    private void createAirDefenseTables() {
        try (Connection connection = ServerDatabase.getInstance().getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS survival_defense (x INT, y INT, z INT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Item detector

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block block = player.getLocation().getBlock();

        if (block.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                && block.getRelative(0, -1, 0).getType() == Material.IRON_BLOCK) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && !item.getType().isEdible()) {
                    new BukkitRunnable(){
                        int counter = 0;
                        public void run(){
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                            counter++;
                            if(counter >= 5) {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(Survival.getInstance(), 0L, 8L);
                    break;
                }
            }
        }
    }

    private Set<UUID> warnedPlayers = new HashSet<>();
    private Map<UUID, BukkitTask> warningTasks = new HashMap<>();

    @EventHandler
    public void onAirDefense(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isGliding()) {
            return;
        }

        if (player.getUniqueId().equals(UUID.fromString("bf78481c-171c-4ebc-82ea-2f3779fa943a")) || player.getUniqueId().equals(UUID.fromString("93be7fde-2271-4127-af6f-ac444c3b1434"))) {
            return;
        }

        Location playerLoc = player.getLocation();
        for (Location defenseLoc : defenseLocations) {
            if (defenseLoc.getWorld().equals(playerLoc.getWorld()) && defenseLoc.distanceSquared(playerLoc) <= 10000 && Math.abs(defenseLoc.getY() - player.getLocation().getY()) <= 250) {
                Block block = defenseLoc.getBlock();
                if (block.getType() == Material.IRON_TRAPDOOR) {
                    Block blockBelow = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
                    if (blockBelow.getType() == Material.REDSTONE_ORE || blockBelow.getType() == Material.DEEPSLATE_REDSTONE_ORE) {
                        // Calculate direction from defense to player
                        Vector direction = playerLoc.toVector().subtract(defenseLoc.toVector()).normalize();


                        if (!warnedPlayers.contains(player.getUniqueId())) {
                            if (Recharge.getInstance().use(player, "TriskelionAirDefenseMsg", 1000, false, false)) {
                                player.sendMessage(c.red + "Warning! You have entered a protected airspace. Exit immediately or face the consequences!");
                                player.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.6f);
                                BukkitTask task = Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
                                    warnedPlayers.add(player.getUniqueId());
                                    warningTasks.remove(player.getUniqueId());
                                }, 20 * 5);

                                warningTasks.put(player.getUniqueId(), task);
                            }
                            return;
                        }

                        // Shoot "laser"
                        if (Recharge.getInstance().use(player, "TriskelionAirDefense", 800, false, false)) {
                            if (Recharge.getInstance().use(player, "TriskelionAirDefenseMsg", 5000, false, false)) {
                                player.sendMessage(c.red + "Warning! You are in a protected airspace. Exit immediately!");
                                player.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.6f);
                            }

                            for (double d = 0; d <= 250; d += 0.5) {
                                Location point = defenseLoc.clone().add(direction.clone().multiply(d));

                                point.getWorld().spawnParticle(Particle.END_ROD, point, 0, 0, 0, 0, 1, null, true);

                                // Check if player is near the "laser"
                                if (playerLoc.distanceSquared(point) <= 1) {
                                    player.damage(3.0); // Damage the player
                                    break; // No need to continue the "laser" after damaging the player
                                }

                                player.getWorld().playSound(defenseLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
                                if (Recharge.getInstance().use(player, "TriskelionAirDefenseSound", 500, false, false)) {
                                    player.getWorld().playSound(playerLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.6f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBoatMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();

        // Check if the vehicle is a boat
        if (!(vehicle instanceof Boat)) {
            return;
        }

        // Check if there is a player in the boat
        Entity passenger = vehicle.getPassengers().size() > 0 ? vehicle.getPassengers().get(0) : null;
        if (!(passenger instanceof Player player)) {
            return;
        }

        if (player.getUniqueId().equals(UUID.fromString("bf78481c-171c-4ebc-82ea-2f3779fa943a")) || player.getUniqueId().equals(UUID.fromString("93be7fde-2271-4127-af6f-ac444c3b1434"))) {
            return;
        }

        Location playerLoc = player.getLocation();
        for (Location defenseLoc : defenseLocations) {
            if (defenseLoc.getWorld().equals(playerLoc.getWorld()) && defenseLoc.distanceSquared(playerLoc) <= 10000 && Math.abs(defenseLoc.getY() - player.getLocation().getY()) <= 250) {
                Block block = defenseLoc.getBlock();
                if (block.getType() == Material.IRON_TRAPDOOR) {
                    Block blockBelow = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
                    if (blockBelow.getType() == Material.REDSTONE_ORE || blockBelow.getType() == Material.DEEPSLATE_REDSTONE_ORE) {
                        // Calculate direction from defense to player
                        Vector direction = playerLoc.toVector().subtract(defenseLoc.toVector()).normalize();


                        if (!warnedPlayers.contains(player.getUniqueId())) {
                            if (Recharge.getInstance().use(player, "TriskelionAirDefenseMsg", 1000, false, false)) {
                                player.sendMessage(c.red + "Warning! You have entered a protected maritime area. Exit immediately or face the consequences!");
                                player.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.6f);
                                BukkitTask task = Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
                                    warnedPlayers.add(player.getUniqueId());
                                    warningTasks.remove(player.getUniqueId());
                                }, 20 * 5);

                                warningTasks.put(player.getUniqueId(), task);
                            }
                            return;
                        }

                        // Shoot "laser"
                        if (Recharge.getInstance().use(player, "TriskelionAirDefense", 800, false, false)) {
                            if (Recharge.getInstance().use(player, "TriskelionAirDefenseMsg", 5000, false, false)) {
                                player.sendMessage(c.red + "Warning! You are in a protected maritime area. Exit immediately!");
                                player.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.6f);
                            }

                            for (double d = 0; d <= 250; d += 0.5) {
                                Location point = defenseLoc.clone().add(direction.clone().multiply(d));

                                point.getWorld().spawnParticle(Particle.END_ROD, point, 0, 0, 0, 0, 1, null, true);

                                // Check if player is near the "laser"
                                if (playerLoc.distanceSquared(point) <= 1) {
                                    player.damage(3.0); // Damage the player
                                    break; // No need to continue the "laser" after damaging the player
                                }

                                player.getWorld().playSound(defenseLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
                                if (Recharge.getInstance().use(player, "TriskelionAirDefenseSound", 500, false, false)) {
                                    player.getWorld().playSound(playerLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.6f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void detectEntities() {
        for (Location defenseLoc : defenseLocations) {
            World world = defenseLoc.getWorld();
            if (world != null) {
                for (Entity entity : world.getEntities()) {
                    if (isHostile(entity)) {
                        if (defenseLoc.getWorld().equals(entity.getWorld()) && defenseLoc.distanceSquared(entity.getLocation()) <= 5000 && Math.abs(defenseLoc.getY() - entity.getLocation().getY()) <= 30) {
                            Block block = defenseLoc.getBlock();
                            if (block.getType() == Material.IRON_TRAPDOOR) {
                                Block blockBelow = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
                                if (blockBelow.getType() == Material.GOLD_ORE || blockBelow.getType() == Material.DEEPSLATE_GOLD_ORE) {
                                    shootLaser((LivingEntity) entity, defenseLoc);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<Location, Long> lastSoundTimes = new HashMap<>();

    public void shootLaser(LivingEntity entity, Location defenseLocation) {
        Vector direction = entity.getLocation().toVector().subtract(defenseLocation.toVector()).normalize();

        for (double d = 0; d <= 90; d += 0.5) {
            Location point = defenseLocation.clone().add(direction.clone().multiply(d));

            point.getWorld().spawnParticle(Particle.SPELL_WITCH, point, 0, 0, 0, 0, 1, null, true);

            if (entity.getLocation().distanceSquared(point) <= 1) {
                entity.damage(7.0);
                break;
            }

            Long lastSoundTime = lastSoundTimes.get(defenseLocation);
            if (lastSoundTime == null || System.currentTimeMillis() - lastSoundTime >= 5000) {
                entity.getWorld().playSound(defenseLocation, Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.0F);
                lastSoundTimes.put(defenseLocation, System.currentTimeMillis());
            }
        }
    }

    private boolean isHostile(Entity entity) {
        return entity instanceof Monster || entity instanceof Ghast || entity instanceof Phantom;
    }

    public void addDefense(Location location) {
        try {
            try (Connection connection = ServerDatabase.getInstance().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO survival_defense (x,y,z) VALUES (?, ?, ?)")) {
                statement.setDouble(1, location.getX());
                statement.setDouble(2, location.getY());
                statement.setDouble(3, location.getZ());

                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Missiles

    public void launchMissile(final Location target, Player player, int numberOfMissiles, float explosionSize) {
        for (int i = 0; i < numberOfMissiles; i++) {
            Random random = new Random();
            Location launchLocation = launchLocations.get(random.nextInt(launchLocations.size()));

            // Add a random offset to the x and z coordinates
            double xOffset = random.nextDouble() * 2 - 1; // Random value between -1 and 1
            double zOffset = random.nextDouble() * 2 - 1; // Random value between -1 and 1
            launchLocation.add(xOffset, 0, zOffset);

            // Delay each missile by 20 ticks (1 second) multiplied by the missile number
            Bukkit.getScheduler().scheduleSyncDelayedTask(Survival.getInstance(), new Runnable() {
                @Override
                public void run() {
                    // Create a new BukkitRunnable to control the missile
                    new BukkitRunnable() {
                        int count = 0;
                        Location currentLocation = launchLocation.clone();
                        Location currentTarget = (player != null) ? player.getLocation() : target.getWorld().getHighestBlockAt(target).getLocation();

                        @Override
                        public void run() {
                            // Update target location if a player is specified
                            if (player != null) {
                                currentTarget = player.getLocation();
                            }

                            // Update direction
                            Vector direction = currentTarget.toVector().subtract(currentLocation.toVector()).normalize();
                            Vector step = new Vector(0, 0, 0);  // Default initialization

                            // Calculate blend ratio based on a sinusoidal function for arc effect
                            double blend = 0.5 * (1 - Math.cos(Math.PI * count / 800));

                            if (count < 800) { // Ascend and bend trajectory towards the target over 800 ticks
                                // Calculate direction
                                Vector upwards = new Vector(0, 1, 0);
                                Vector blendedDirection = upwards.multiply(1 - blend).add(direction.multiply(blend));
                                step = blendedDirection.normalize().multiply(1);
                            } else if (currentLocation.distance(currentTarget) < 1) { // Check if reached target
                                currentLocation.getWorld().createExplosion(currentLocation, explosionSize); // Create explosion
                                currentLocation.getWorld().playSound(currentLocation, Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 1.0f);
                                this.cancel(); // Cancel the task
                                activeMissiles.remove(this);
                            } else { // Move towards the target
                                step = direction.clone().multiply(1);
                            }
                            currentLocation.add(step);

                            // Spawn particles
                            currentLocation.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation, 30, 0.1, 0.1, 0.1, 0, null, true);

                            // Spawn additional "fire trail" particles in front of the missile
                            Location frontLocation = currentLocation.clone().add(step);
                            currentLocation.getWorld().spawnParticle(Particle.FLAME, frontLocation, 100, 0.1, 0.1, 0.1, 0, null, true);

                            if (count == 0) {
                                launchLocation.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, launchLocation, 10, 2.0, 2.0, 2.0, 2, null, true);
                                launchLocation.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, launchLocation, 100, 2.0, 2.0, 2.0, 2, null, true);
                                launchLocation.getWorld().playSound(launchLocation, Sound.ENTITY_WITHER_DEATH, 1.0f, 0f);
                            }

                            count++;
                        }
                    }.runTaskTimer(Survival.getInstance(), 0L, 1L);
                }
            }, 20L * i);
        }
    }

    public void removeDefense(Player player) {
        Block block = player.getTargetBlock(null, 5);
        if (block.getType() == Material.IRON_TRAPDOOR) {
            Location location = block.getLocation();
            if (defenseLocations.contains(location)) {
                defenseLocations.remove(location);
                String query = "DELETE FROM survival_defense WHERE x = ? AND y = ? AND z = ?";
                try (Connection connection = ServerDatabase.getInstance().getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query);) {
                    preparedStatement.setInt(1, location.getBlockX());
                    preparedStatement.setInt(2, location.getBlockY());
                    preparedStatement.setInt(3, location.getBlockZ());
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                player.sendMessage(c.green + "Defense removed successfully!");
                loadDefenseLocations();
            } else {
                player.sendMessage(c.red + "There's no defense here!");
            }
        } else {
            player.sendMessage(c.red + "You need to look at an defense to remove it!");
        }
    }

    public void removeDefense(Location location) {
        if (defenseLocations.contains(location)) {
            defenseLocations.remove(location);
            String query = "DELETE FROM survival_defense WHERE x = ? AND y = ? AND z = ?";
            try (Connection connection = ServerDatabase.getInstance().getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query);) {
                preparedStatement.setInt(1, location.getBlockX());
                preparedStatement.setInt(2, location.getBlockY());
                preparedStatement.setInt(3, location.getBlockZ());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        loadDefenseLocations();
    }

    public void RemoveWarnedPlayers() {
        Iterator<UUID> iterator = warnedPlayers.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Location playerLoc = player.getLocation();
                boolean isInDefenseZone = defenseLocations.stream()
                        .anyMatch(defenseLoc -> defenseLoc.getWorld().equals(playerLoc.getWorld())
                                && defenseLoc.distanceSquared(playerLoc) <= 8100);
                if (!isInDefenseZone) {
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }













}
