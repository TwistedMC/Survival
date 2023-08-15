package com.twistedmcstudios.survival.world.fun;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.survival.world.WorldManager;

public class RefreshDefensesCommand extends CoreCommand {

    public RefreshDefensesCommand() {
        super("refreshdefenses", new String[] {}, 0, Rank.ADMIN, null, false, false);
    }

    @Override
    public void execute() {
        WorldManager.getInstance().loadDefenseLocations();
    }
}