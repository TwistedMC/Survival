package com.twistedmcstudios.survival.commands.spawn;

import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.Module;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.survival.events.JoinManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.BeaconInventory;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.util.Vector;

import java.util.Objects;

public class SpawnManager extends Module {

    private SpawnManager() {
        super("Spawn Manager");
    }

    private static final SpawnManager instance = new SpawnManager();

    public static SpawnManager getInstance() {
        return instance;
    }

    /**
     * Prevent tnt
     */
    @EventHandler
    public void tnt(BlockPlaceEvent event) {
        if (event.getBlock().getWorld().getName().equals("Spawn")) {
            if (event.getBlock().getType() == Material.TNT || event.getBlock().getType() == Material.TNT_MINECART) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomBlock(PlayerInteractEvent event){
        if (event.getPlayer().getWorld().getName().equals("Spawn")) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getRelative(BlockFace.UP).getType() == Material.FIRE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getWorld().getName().equals("Spawn")) {
            if (event.isCancelled()) return;
            if (event.getBlock().getType().toString().equals("FIRE")) {
                event.setCancelled(false);
            }
        }
    }

    /**
     * Prevent sheep eating grass
     */
    @EventHandler
    public void onSheepEatGrass(EntityChangeBlockEvent event) {
        EntityType a = EntityType.SHEEP;
        if (event.getEntity().getWorld().getName().equals("Spawn")) {
            if (event.getEntity().getType() == a) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMobTrample(EntityInteractEvent event) {
        if (event.getEntity().getWorld().getName().equals("Spawn")) {
            if (event.getBlock().getType().equals(Material.FARMLAND)) {
                if (event.getEntity() instanceof Player) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTrample(PlayerInteractEvent event) {
        if (event.getPlayer().getWorld().getName().equals("Spawn")) {
            if (event.getAction() == Action.PHYSICAL && Objects.requireNonNull(event.getClickedBlock()).getType().equals(Material.FARMLAND)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity().getWorld().getName().equals("Spawn")) {
            if (event.getEntityType() == EntityType.FALLING_BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void BlockBreak(BlockBreakEvent event) {
        Account player = new Account(event.getPlayer());
        if (event.getPlayer().getWorld().getName().equals("Spawn")) {
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE && !player.compareWith(Rank.ADMIN)) {
                event.setCancelled(true);
                return;
            }
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE && player.compareWith(Rank.ADMIN)) {
                return;
            }
            if (event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void BlockPlace(BlockPlaceEvent event) {
        Account player = new Account(event.getPlayer());
        if (event.getPlayer().getWorld().getName().equals("Spawn")) {
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE && !player.compareWith(Rank.ADMIN)) {
                event.setCancelled(true);
                return;
            }
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE && player.compareWith(Rank.ADMIN)) {
                return;
            }
            if (event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        Player player = event.getPlayer();

        if (to != null && to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ() && to.getBlockY() == from.getBlockY()) {
            return;
        }

        if (Objects.requireNonNull(event.getTo()).getBlock().getType().equals(Material.LIGHT_WEIGHTED_PRESSURE_PLATE) && player.getLocation().getWorld().getName().equals("Spawn")) {

            player.setVelocity(event.getPlayer().getVelocity().add(new Vector(
                    event.getTo().getDirection().getX()*3, 1.1, event.getTo().getDirection().getZ()*6
            )));

            player.playSound(event.getTo(), Sound.ENTITY_GENERIC_EXPLODE, 100, 2);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getWorld().getName().equals("Spawn")) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().getWorld().getName().equals("Spawn")) {
            if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void openBeacon(InventoryOpenEvent event) {
        if (event.getPlayer().getWorld().getName().equals("Spawn")) {
            if (event.getInventory() instanceof BeaconInventory || event.getInventory() instanceof BrewerInventory || event.getInventory() instanceof Chest) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void creatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity().getWorld().getName().equals("Spawn")) {
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent block burning
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBurn(BlockBurnEvent event) {
        if (event.getBlock().getWorld().getName().equals("Spawn")) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent blocks catching fire
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockIgnite(BlockIgniteEvent event) {
        if (event.getBlock().getWorld().getName().equals("Spawn")) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents block growth
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockGrow(BlockGrowEvent event) {
        if (event.getBlock().getWorld().getName().equals("Spawn")) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent block spreading, e.g vines
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockSpread(BlockSpreadEvent event) {
        if (event.getBlock().getWorld().getName().equals("Spawn")) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent leaves decaying
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void leavesDecay(LeavesDecayEvent event) {
        if (event.getBlock().getWorld().getName().equals("Spawn")) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent the crafting of items
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void itemCraft(CraftItemEvent event) {
        if (event.getWhoClicked().getWorld().getName().equals("Spawn")) {
            event.setCancelled(true);
        }
    }



}
