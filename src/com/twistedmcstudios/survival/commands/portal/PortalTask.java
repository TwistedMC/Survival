package com.twistedmcstudios.survival.commands.portal;

import com.twistedmcstudios.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PortalTask extends BukkitRunnable {
    private List<Portal> portals = new ArrayList<>();
    private Random random = new Random();

    public void createPortal(Location location) {
        portals.add(new Portal(location));
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> portals.removeIf(portal -> portal.getLocation().equals(location)), 20L * (random.nextInt(60) + 60));
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Portal portal : new ArrayList<>(portals)) {
                spawnPortalParticles(portal.getLocation(), 100, 2.0);

                if (portal.isPlayerInside(player)) {
                    ItemStack chestplate = player.getInventory().getChestplate();
                    portal.setPreviousChestplate(player, chestplate);

                    player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
                    player.teleport(portal.getLocation().add(0, 50, 0));
                    portal.setInFlight(player, true);
                }

                if (portal.isInFlight(player) && player.isOnGround()) {
                    player.getInventory().setChestplate(portal.getPreviousChestplate(player));
                    portal.setInFlight(player, false);
                }
            }
        }
    }

    public void spawnPortalParticles(Location location, int count, double radius) {
        World world = location.getWorld();
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            world.spawnParticle(Particle.PORTAL, location.clone().add(x, 0, z), 0);
            world.spawnParticle(Particle.ENCHANTMENT_TABLE, location.clone().add(x, 0, z), 0);
        }
    }
}

