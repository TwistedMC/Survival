package com.twistedmcstudios.survival;

import com.twistedmcstudios.core.Core;
import com.twistedmcstudios.core.blood.Blood;
import com.twistedmcstudios.core.common.Module;
import com.twistedmcstudios.core.common.util.*;
import com.twistedmcstudios.core.database.ServerDatabase;
import com.twistedmcstudios.core.database.UnauthorizedServer;
import com.twistedmcstudios.core.server.ServerStatus;
import com.twistedmcstudios.core.tasks.TaskManager;
import com.twistedmcstudios.core.world.ExploitProtectionManager;
import com.twistedmcstudios.survival.commands.CommandManager;
import com.twistedmcstudios.survival.commands.portal.PortalManager;
import com.twistedmcstudios.survival.commands.spawn.SpawnManager;
import com.twistedmcstudios.survival.events.JoinManager;
import com.twistedmcstudios.survival.scoreboard.PlayerManager;
import com.twistedmcstudios.survival.soulstats.SoulAndStatsManager;
import com.twistedmcstudios.survival.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class Survival extends JavaPlugin {

    private static Survival instance;
    private static long startTime;

    public static FileConfiguration config;

    private ArrayList<Module> modules;

    @Override
    public void onLoad() {
        startTime = System.currentTimeMillis();
        LogManager.log("Survival", "Starting...");
    }

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Core")) {
            LogManager.log("Survival", "Core not found or enabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (ServerDatabase.getInstance().serverAuthorized()) {

            LogManager.log("Authorization Manager", "Server is authorized to use Survival.");

            instance = this;

            modules = new ArrayList<>();

            modules.add(ExploitProtectionManager.getInstance());
            modules.add(PlayerManager.getInstance());
            modules.add(SoulAndStatsManager.getInstance());
            modules.add(WorldManager.getInstance());
            modules.add(JoinManager.getInstance());
            modules.add(SpawnManager.getInstance());
            modules.add(TaskManager.getInstance());
            modules.add(PortalManager.getInstance());
            modules.add(Blood.getInstance());

            for (Module module : modules) {
                module.enable();
            }

           CommandManager.getInstance().createCommands();

           // new PlaceholderListener().register();

            LogManager.log("Survival", "Successfully started in " + UtilTime.convertString(System.currentTimeMillis() - startTime, 1, TimeUnit.FIT) + "!");
        } else {
            UnauthorizedServer.getInstance().enable();
            UnauthorizedServer.getInstance().sendCeaseAndDesist();
            ServerDatabase.getInstance().setServerStatus(Core.getServerDataManager().getServerIdentifier(), ServerStatus.UNAUTHORIZED);
        }
    }

    @Override
    public void onDisable() {
        if (ServerDatabase.getInstance().serverAuthorized()) {
            for (Module module : modules) {
                module.disable();
            }
        } else {
            LogManager.log("Survival", "An error occurred while disabling the plugin.");
        }
    }

    public static Survival getInstance() {
        return instance;
    }

    public static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Survival");
    }

}
