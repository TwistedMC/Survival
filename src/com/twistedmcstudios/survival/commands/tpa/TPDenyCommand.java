package com.twistedmcstudios.survival.commands.tpa;

import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.account.OfflineAccount;
import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.recharge.Recharge;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.survival.commands.tpa.managers.TeleportQueue;
import com.twistedmcstudios.survival.commands.tpa.managers.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TPDenyCommand extends CoreCommand {

    TeleportQueue tpQueue = TeleportQueue.INSTANCE;

    public TPDenyCommand() {
        super("tpdeny", new String[] {}, 0, Rank.PLAYER, null, false, false);
    }

    @Override
    public void execute() {

        TeleportRequest tpRequest = tpQueue.TELEPORT_REQUESTS.get(getAccount().getPlayer().getUniqueId());

        if (tpRequest == null) {
            getAccount().getPlayer().sendMessage(c.red + "You do not have any pending teleportation requests!");
            return;
        }

        Player target = Bukkit.getPlayer(tpRequest.requestee);
        OfflineAccount targetAccount = new OfflineAccount(tpRequest.requestee);
        if (target == null) {
            getAccount().getPlayer().sendMessage(c.red + "Huh, " + targetAccount.getManager().getRankName() + " is offline.");
            tpQueue.TELEPORT_REQUESTS.remove(getAccount().getPlayer().getUniqueId());
            return;
        }

        Account targetAccountOnline = new Account(target);

        getAccount().getPlayer().sendMessage(c.red + "You denied " + targetAccount.getManager().getRankName() + c.red + "'s teleportation request.");
        targetAccountOnline.getPlayer().sendMessage(c.red + "Your teleportation request to " + getAccount().getManager().getRankName() + c.red + " was denied.");
        tpQueue.TELEPORT_REQUESTS.remove(getAccount().getPlayer().getUniqueId());
    }
}