package com.twistedmcstudios.survival.commands.tasks;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.recharge.Recharge;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.ingamestore.StoreType;
import com.twistedmcstudios.core.ingamestore.confirmpurchase.ConfirmPurchaseMenu;
import com.twistedmcstudios.survival.tasks.TaskMenu;

public class TasksCommand extends CoreCommand {

    public TasksCommand() {
        super("tasks", new String[] {}, 0, Rank.PLAYER, null, false, false);
    }

    @Override
    public void execute() {
        TaskMenu taskMenu = new TaskMenu(getAccount().getPlayer());
        taskMenu.openInventory(getAccount().getPlayer());
    }
}