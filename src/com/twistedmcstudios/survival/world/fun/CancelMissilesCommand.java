package com.twistedmcstudios.survival.world.fun;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.survival.world.WorldManager;
import org.bukkit.scheduler.BukkitRunnable;

public class CancelMissilesCommand extends CoreCommand {

    public CancelMissilesCommand() {
        super("cancelmissiles", new String[] {"cm"}, 0, Rank.OWNER, null, false, false);
    }

    @Override
    public void execute() {
        for (BukkitRunnable missile : WorldManager.getInstance().activeMissiles) {
            missile.cancel();
        }
        WorldManager.getInstance().activeMissiles.clear();
        getAccount().getPlayer().sendMessage(c.green + "All active missiles cancelled.");

    }
}