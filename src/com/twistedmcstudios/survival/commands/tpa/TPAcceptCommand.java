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

public class TPAcceptCommand extends CoreCommand {

    TeleportQueue tpQueue = TeleportQueue.INSTANCE;

    public TPAcceptCommand() {
        super("tpaccept", new String[] {}, 0, Rank.PLAYER, null, false, false);
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
        } else {
            Account targetAccountOnline = new Account(target);

            if (!tpRequest.isReversed) {
                tpQueue.PREVIOUS_LOCATIONS.put(tpRequest.requestee, targetAccountOnline.getPlayer().getLocation());
                targetAccountOnline.getPlayer().teleport(getAccount().getPlayer());
                tpQueue.TELEPORT_REQUESTS.remove(getAccount().getPlayer().getUniqueId());
                targetAccountOnline.getPlayer().sendMessage(c.green + "You are teleporting to " + getAccount().getManager().getRankName() + c.green + "!");
                getAccount().getPlayer().sendMessage(targetAccountOnline.getManager().getRankName() + c.green + " is teleporting to you!");
            } else {
                tpQueue.PREVIOUS_LOCATIONS.put(getAccount().getPlayer().getUniqueId(), getAccount().getPlayer().getLocation());
                getAccount().getPlayer().teleport(targetAccountOnline.getPlayer());
                tpQueue.TELEPORT_REQUESTS.remove(getAccount().getPlayer().getUniqueId());
                getAccount().getPlayer().sendMessage(c.green + "You are teleporting to " + targetAccountOnline.getManager().getRankName() + c.green + "!");
                targetAccountOnline.getPlayer().sendMessage(getAccount().getManager().getRankName() + c.green + " is teleporting to you!");
            }
        }
    }
}