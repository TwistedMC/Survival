package com.twistedmcstudios.survival.commands.spawn;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.spawn.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class SpawnCommand extends CoreCommand {

    public SpawnCommand() {
        super("spawn", new String[]{}, 1, Rank.PLAYER, null, false);
    }

    @Override
    public void execute() {
        Player player = getAccount().getPlayer();
        if (getArgs().length == 0) {
            try {
                SpawnManager.teleportToSpawn(player, true);
            } catch (SQLException e) {
                player.sendMessage(c.red + "Error: No spawn exists. Please report this to a staff member.");
                throw new RuntimeException(e);
            }
        } else if (getArgs().length == 1 && getAccount().compareWith(Rank.ADMIN)) {
            if (getArgs()[0].equalsIgnoreCase("setspawn")) {
                try {
                    player.sendMessage(c.green + "Spawn point has been set to your location!");
                    SpawnManager.setSpawnLocation(player);
                } catch (SQLException e) {
                    player.sendMessage(c.red + "An error occurred.");
                    throw new RuntimeException(e);
                }
            } else {
                String playerName = getArgs()[0];
                Player targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    player.sendMessage(c.red + "I'm sorry, but the specified player '" + c.red + c.bold + getArgs()[0] + c.red + "' is not online.");
                    return;
                }

                try {
                    player.sendMessage(c.green + "Teleported '" + c.green + c.bold + targetPlayer.getName() + c.green + "' to spawn!");
                    SpawnManager.teleportToSpawn(targetPlayer, true);
                } catch (SQLException e) {
                    player.sendMessage(c.red + "An error occurred.");
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (getAccount().compareWith(Rank.ADMIN)) {
                getAccount().getPlayer().sendMessage(c.yellow + "Correct usages:");
                getAccount().getPlayer().sendMessage(c.aqua + "/spawn " + c.yellow + "-" + c.aqua + " teleport to spawn.");
                getAccount().getPlayer().sendMessage(c.aqua + "/spawn <player> " + c.yellow + "-" + c.aqua + " teleport specific player to spawn.");
                getAccount().getPlayer().sendMessage(c.aqua + "/spawn setspawn " + c.yellow + "-" + c.aqua + " set spawn point to your location.");
            } else {
                try {
                    SpawnManager.teleportToSpawn(player, true);
                } catch (SQLException e) {
                    player.sendMessage(c.red + "Error: No spawn exists. Please report this to a staff member.");
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
