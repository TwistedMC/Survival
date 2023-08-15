package com.twistedmcstudios.survival.world.fun;

import com.twistedmcstudios.core.account.Rank;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.survival.world.WorldManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class DefenseCommand extends CoreCommand {

    public DefenseCommand() {
        super("defense", new String[] {}, 0, Rank.ADMIN, null, false, false);
    }

    @Override
    public void execute() {

        if (getArgs()[0].equals("add")) {
            Location feetLocation = getAccount().getPlayer().getLocation();
            Block block = feetLocation.getBlock(); // Get the block the player is standing on
            if (block.getType() == Material.IRON_TRAPDOOR) {
                Block blockBelow = block.getRelative(BlockFace.DOWN);
                if (blockBelow.getType() == Material.REDSTONE_ORE || blockBelow.getType() == Material.DEEPSLATE_REDSTONE_ORE || blockBelow.getType() == Material.GOLD_ORE || blockBelow.getType() == Material.DEEPSLATE_GOLD_ORE) {
                    WorldManager.getInstance().addDefense(block.getLocation());
                    getAccount().getPlayer().sendMessage(c.green + "Defense added successfully.");
                    WorldManager.getInstance().loadDefenseLocations();
                } else {
                    getAccount().getPlayer().sendMessage(c.red + "The block below the trapdoor needs to be a redstone or gold (mob defense) ore.");
                }
            } else {
                getAccount().getPlayer().sendMessage(c.red + "You need to be standing on an iron trapdoor.");
            }
        } else if (getArgs()[0].equals("remove")) {
            WorldManager.getInstance().removeDefense(getAccount().getPlayer());
        }
    }
}