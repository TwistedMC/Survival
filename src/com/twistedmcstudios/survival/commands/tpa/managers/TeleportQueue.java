package com.twistedmcstudios.survival.commands.tpa.managers;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.UUID;

public class TeleportQueue {
    public static final TeleportQueue INSTANCE = new TeleportQueue();
    private TeleportQueue() {
        // Exists only to defeat instantiation.
    }
    public final HashMap<UUID, TeleportRequest> TELEPORT_REQUESTS = new HashMap<UUID, TeleportRequest>();
    public final HashMap<UUID, Location> PREVIOUS_LOCATIONS = new HashMap<UUID, Location>();
}

