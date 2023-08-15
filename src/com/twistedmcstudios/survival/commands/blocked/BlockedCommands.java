package com.twistedmcstudios.survival.commands.blocked;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.util.c;

public class BlockedCommands extends CoreCommand {

    public BlockedCommands() {
        super("give", new String[] {"minecraft:gamemode", "minecraft:give", "minecraft:tp", "minecraft:teleport"}, 0, Rank.PLAYER, null, false, true);
    }

    @Override
    public void execute() {
        getAccount().getPlayer().sendMessage(c.red + "Sorry, you are on cooldown.");
    }
}