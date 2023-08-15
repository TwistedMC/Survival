package com.twistedmcstudios.survival.commands;

import com.twistedmcstudios.core.commands.tasks.GiveTasksCommand;
import com.twistedmcstudios.core.commands.vanish.VanishCommand;
import com.twistedmcstudios.core.common.CoreCommand;
import com.twistedmcstudios.core.common.util.LogManager;
import com.twistedmcstudios.core.common.util.TimeUnit;
import com.twistedmcstudios.core.common.util.UtilTime;
import com.twistedmcstudios.survival.commands.blocked.BlockedCommands;
import com.twistedmcstudios.survival.commands.portal.PortalCommand;
import com.twistedmcstudios.survival.commands.spawn.SpawnCommand;
import com.twistedmcstudios.survival.commands.store.BuyCommand;
import com.twistedmcstudios.survival.commands.store.UseCommand;
import com.twistedmcstudios.survival.commands.tasks.PointsShopCommand;
import com.twistedmcstudios.survival.commands.tasks.TasksCommand;
import com.twistedmcstudios.survival.commands.tpa.TPACommand;
import com.twistedmcstudios.survival.commands.tpa.TPAcceptCommand;
import com.twistedmcstudios.survival.commands.tpa.TPDenyCommand;
import com.twistedmcstudios.survival.commands.tpa.TPHereCommand;
import com.twistedmcstudios.survival.world.fun.DefenseCommand;
import com.twistedmcstudios.survival.world.fun.CancelMissilesCommand;
import com.twistedmcstudios.survival.world.fun.MissileCommand;
import com.twistedmcstudios.survival.world.fun.RefreshDefensesCommand;

import java.util.ArrayList;

public class CommandManager {
    private static final CommandManager instance = new CommandManager();

    public static CommandManager getInstance() {
        return instance;
    }

    private ArrayList<CoreCommand> commands;

    public void createCommands() {
        commands = new ArrayList<>();

        long startTime = UtilTime.getCurrentMillis();
        LogManager.log("Command Manager", "Initializing...");

        // Admin
        commands.add(new MissileCommand());
        commands.add(new CancelMissilesCommand());
        commands.add(new GiveTasksCommand());
        commands.add(new PortalCommand());
        commands.add(new RefreshDefensesCommand());
        commands.add(new DefenseCommand());

        // Sr Mod

        // Mod
        //commands.add(new StaffModeCommand());

        // Helper
        commands.add(new VanishCommand());

        // General
        commands.add(new BuyCommand());
        commands.add(new TPACommand());
        commands.add(new TPAcceptCommand());
        commands.add(new TPDenyCommand());
        commands.add(new TPHereCommand());
        commands.add(new UseCommand());
        commands.add(new SpawnCommand());
        commands.add(new TasksCommand());
        commands.add(new PointsShopCommand());

        // Other
        commands.add(new BlockedCommands());
        commands.add(new com.twistedmcstudios.core.chat.blocked.BlockedCommands());

        for (CoreCommand command : commands) {
            LogManager.log("Command Manager", "Enabling /" + command.getName() + " command.");
            command.registerCommand();
            command.registerEvents();
        }

        LogManager.log("Command Manager", "Enabled in " + UtilTime.convertString(UtilTime.getCurrentMillis() - startTime, 1, TimeUnit.FIT) + ".");
    }

    public CoreCommand getCommand(String name) {
        for (CoreCommand command : commands) {
            if (command.getName().contains(name) || command.getName().startsWith(name))
                return command;
        }

        return null;
    }

    public ArrayList<CoreCommand> getCommands() {
        return commands;
    }
}
