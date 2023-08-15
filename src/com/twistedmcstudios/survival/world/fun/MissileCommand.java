package com.twistedmcstudios.survival.world.fun;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.survival.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class MissileCommand extends CoreCommand {

    public MissileCommand() {
        super("missile", new String[] {}, 0, Rank.OWNER, null, false, false);
    }

    @Override
    public void execute() {
        if (getArgs().length == 2 || getArgs().length == 3) {
            Player target = Bukkit.getPlayer(getArgs()[0]);
            int numberOfMissiles = 1;
            float explosionSize = 3.0f; // default explosion size
            if (getArgs().length >= 2) {
                try {
                    numberOfMissiles = Integer.parseInt(getArgs()[1]);
                } catch (NumberFormatException e) {
                    getAccount().getPlayer().sendMessage(c.red + "Invalid number of missiles.");
                    return;
                }
            }
            if (getArgs().length == 3) {
                try {
                    explosionSize = Float.parseFloat(getArgs()[2]);
                } catch (NumberFormatException e) {
                    getAccount().getPlayer().sendMessage(c.red + "Invalid explosion size.");
                    return;
                }
            }
            if (target != null) {
                Location targetLocation = target.getLocation();
                WorldManager.getInstance().launchMissile(targetLocation, target, numberOfMissiles, explosionSize);
            } else {
                getAccount().getPlayer().sendMessage(c.red + "Player not found.");
            }
        } else if (getArgs().length == 4 || getArgs().length == 5) {
            try {
                double x = Double.parseDouble(getArgs()[0]);
                double y = Double.parseDouble(getArgs()[1]);
                double z = Double.parseDouble(getArgs()[2]);
                Location targetLocation = new Location(getAccount().getPlayer().getServer().getWorld("world"), x, y, z);
                int numberOfMissiles = 1;
                float explosionSize = 3.0f; // default explosion size
                if (getArgs().length >= 4) {
                    try {
                        numberOfMissiles = Integer.parseInt(getArgs()[3]);
                    } catch (NumberFormatException e) {
                        getAccount().getPlayer().sendMessage(c.red + "Invalid number of missiles.");
                        return;
                    }
                }
                if (getArgs().length == 5) {
                    try {
                        explosionSize = Float.parseFloat(getArgs()[4]);
                    } catch (NumberFormatException e) {
                        getAccount().getPlayer().sendMessage(c.red + "Invalid explosion size.");
                        return;
                    }
                }
                WorldManager.getInstance().launchMissile(targetLocation, null, numberOfMissiles, explosionSize);
            } catch (NumberFormatException e) {
                getAccount().getPlayer().sendMessage(c.red + "Invalid coordinates.");
            }
        } else {
            getAccount().getPlayer().sendMessage(c.red + "Invalid command usage.");
        }


    }
}