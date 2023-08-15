package com.twistedmcstudios.survival.commands.portal;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.survival.tasks.TaskMenu;
import org.bukkit.Location;

public class PortalCommand extends CoreCommand {

    public PortalCommand() {
        super("createportal", new String[] {}, 0, Rank.ADMIN, null, false, false);
    }

    @Override
    public void execute() {
        Location location = getAccount().getPlayer().getLocation();
        PortalManager.getInstance().portalTask.createPortal(location);

        getAccount().getPlayer().sendMessage("A portal has been created at your location.");
    }
}