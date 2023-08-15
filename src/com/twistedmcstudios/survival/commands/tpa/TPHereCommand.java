package com.twistedmcstudios.survival.commands.tpa;

import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.account.OfflineAccount;
import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.recharge.Recharge;
import com.twistedmcstudios.core.common.util.UtilPlayer;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.survival.commands.tpa.managers.TeleportQueue;
import com.twistedmcstudios.survival.commands.tpa.managers.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TPHereCommand extends CoreCommand {

    TeleportQueue tpQueue = TeleportQueue.INSTANCE;

    public TPHereCommand() {
        super("tphere", new String[] {}, 1, Rank.PLAYER, "<player>", false, false);
    }

    @Override
    public void execute() {
        Player target = Bukkit.getPlayerExact(getArgs()[0]);
        if (target == null || new OfflineAccount(target.getUniqueId()).getManager().isVanished()) {
            getAccount().getPlayer().sendMessage(c.red + "I'm sorry, but the specified player '" + c.red + c.bold + getArgs()[0] + c.red + "' is not online.");
            return;
        }

        if (UtilPlayer.match(getAccount().getPlayer(), getArgs()[0])) {
            return;
        }

        Account targetAccount = new Account(target);

        if (getAccount().getManager().isInStaffMode() || getAccount().getManager().isVanished()) {
            getAccount().getPlayer().sendMessage(c.red + "You cannot tpa to other players while in " + (getAccount().getManager().isInStaffMode() ? "staff mode" : "vanish") + ".");
            return;
        }

        if (target.getUniqueId().equals(getAccount().getPlayer().getUniqueId())) {
            getAccount().getPlayer().sendMessage(c.red + "You cannot TPA to yourself!");
            return;
        }

        tpQueue.TELEPORT_REQUESTS.put(target.getUniqueId(), new TeleportRequest(getAccount().getUUID(), true));
        target.sendMessage(getAccount().getManager().getRankName() + c.yellow + " has requested for you to teleport to them! To accept, type " + c.gold + "/tpaccept" + c.yellow + ". To deny, type " + c.gold + "/tpdeny" + c.yellow + ".");
        getAccount().getPlayer().sendMessage(c.yellow + "Sent a teleportation request to " + targetAccount.getManager().getRankName() + c.yellow + "!");
    }
}