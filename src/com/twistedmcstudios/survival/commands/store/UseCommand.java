package com.twistedmcstudios.survival.commands.store;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.account.stats.Stat;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.recharge.Recharge;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.punish.Punish;
import com.twistedmcstudios.survival.soulstats.SoulAndStatsManager;

public class UseCommand extends CoreCommand {

    public UseCommand() {
        super("use", new String[] {}, 1, Rank.PLAYER, "<item>", false, false);
    }

    @Override
    public void execute() {
        if (Recharge.getInstance().use(getAccount().getPlayer(), "UseCmd", 10000, false, false)) {
            if (getArgs()[0].equals("penance_token")) {
                if (getAccount().getManager().getStatInt(Stat.SURVIVAL_PENANCE_TOKEN) > 0) {
                    getAccount().getManager().decrementStat(Stat.SURVIVAL_PENANCE_TOKEN, 1, "Used Penance Token");

                    SoulAndStatsManager.getInstance().unlockSoul(getAccount().getUUID());

                    Punish.getInstance().SurvivalBan(Punish.getInstance().generateRandomID(), getAccount().getUUID(), "Used Penance Token", Punish.getInstance().formatToMillis("1d"));
                } else {
                    getAccount().getPlayer().sendMessage(c.red + "You do not have any Penance Tokens to use.");
                }

            } else {
                getAccount().getPlayer().sendMessage(c.red + "Huh, we couldn't find an item by that name.");
            }
        } else {
            getAccount().getPlayer().sendMessage(c.red + "Sorry, you are on cooldown.");
        }
    }
}