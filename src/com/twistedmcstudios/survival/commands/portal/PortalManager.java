package com.twistedmcstudios.survival.commands.portal;

import com.twistedmcstudios.core.Core;
import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.boosters.Booster;
import com.twistedmcstudios.core.cache.RankCache;
import com.twistedmcstudios.core.common.Module;
import com.twistedmcstudios.core.common.util.TickUnit;
import com.twistedmcstudios.core.common.util.UtilSound;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.database.ServerDatabase;
import com.twistedmcstudios.survival.Survival;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PortalManager extends Module {

    private PortalManager() {
        super("Portal Manager");
    }

    private static final PortalManager instance = new PortalManager();

    public static PortalManager getInstance() {
        return instance;
    }

    public PortalTask portalTask;

    @Override
    public void Enable() {
        portalTask = new PortalTask();
        portalTask.runTaskTimer(Survival.getInstance(), 0L, 20L);
    }

    @Override
    public void Disable() {
        portalTask.cancel();
    }
}
