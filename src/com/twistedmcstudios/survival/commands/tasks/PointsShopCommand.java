package com.twistedmcstudios.survival.commands.tasks;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.survival.tasks.TaskMenu;
import com.twistedmcstudios.survival.tasks.TaskShop;

public class PointsShopCommand extends CoreCommand {

    public PointsShopCommand() {
        super("pointsshop", new String[] {}, 0, Rank.PLAYER, null, false, false);
    }

    @Override
    public void execute() {
        TaskShop taskShop = new TaskShop(getAccount().getPlayer());
        taskShop.openInventory(getAccount().getPlayer());
    }
}