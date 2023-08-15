package com.twistedmcstudios.survival.commands.portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Portal {
    private Location location;
    private Map<UUID, ItemStack> previousChestplates;
    private Set<UUID> inFlight;

    public Portal(Location location) {
        this.location = location;
        this.previousChestplates = new HashMap<>();
        this.inFlight = new HashSet<>();
    }

    public Location getLocation() {
        return location;
    }

    public void setPreviousChestplate(Player player, ItemStack previousChestplate) {
        this.previousChestplates.put(player.getUniqueId(), previousChestplate);
    }

    public ItemStack getPreviousChestplate(Player player) {
        return previousChestplates.get(player.getUniqueId());
    }

    public boolean isInFlight(Player player) {
        return inFlight.contains(player.getUniqueId());
    }

    public void setInFlight(Player player, boolean inFlight) {
        if (inFlight) {
            this.inFlight.add(player.getUniqueId());
        } else {
            this.inFlight.remove(player.getUniqueId());
        }
    }

    public boolean isPlayerInside(Player player) {
        return player.getLocation().distance(location) <= 2.0;
    }
}

