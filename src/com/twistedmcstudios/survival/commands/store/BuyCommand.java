package com.twistedmcstudios.survival.commands.store;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.recharge.Recharge;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.ingamestore.StoreType;
import com.twistedmcstudios.core.ingamestore.confirmpurchase.ConfirmPurchaseMenu;

public class BuyCommand extends CoreCommand {

    public BuyCommand() {
        super("buy", new String[] {}, 1, Rank.PLAYER, "<item>", false, false);
    }

    @Override
    public void execute() {
        if (getArgs()[0].equals("PenanceToken")) {

            if (Recharge.getInstance().use(getAccount().getPlayer(), "BuyPenance", 5000, false, false)) {
                ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(getAccount().getPlayer(), "penance_token", c.red + "Penance Token", 1, 5, StoreType.GEM_STORE);
                menu.openInventory(getAccount().getPlayer());
                return;
            }

            getAccount().getPlayer().sendMessage(c.red + "Sorry, you are on cooldown.");

        } else if (getArgs()[0].equals("SoulLocView")) {

            if (Recharge.getInstance().use(getAccount().getPlayer(), "BuySoulLocView", 5000, false, false)) {
                ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(getAccount().getPlayer(), "SoulLocView", c.gold + "Soul Location", 1, 5, StoreType.GEM_STORE);
                menu.openInventory(getAccount().getPlayer());
                return;
            }

            getAccount().getPlayer().sendMessage(c.red + "Sorry, you are on cooldown.");

        } else if (getArgs()[0].equals("DeathLocView")) {

            if (Recharge.getInstance().use(getAccount().getPlayer(), "BuyDeathLocView", 5000, false, false)) {
                ConfirmPurchaseMenu menu = new ConfirmPurchaseMenu(getAccount().getPlayer(), "DeathLocView", c.gold + "Death Location", 1, 5, StoreType.GEM_STORE);
                menu.openInventory(getAccount().getPlayer());
                return;
            }

            getAccount().getPlayer().sendMessage(c.red + "Sorry, you are on cooldown.");

        } else {
            getAccount().getPlayer().sendMessage(c.red + "Huh, we couldn't find an item by that name.");
        }
    }
}