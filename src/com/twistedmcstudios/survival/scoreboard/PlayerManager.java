package com.twistedmcstudios.survival.scoreboard;

import com.twistedmcstudios.core.Core;
import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.account.stats.Stat;
import com.twistedmcstudios.core.common.Module;
import com.twistedmcstudios.core.common.update.UpdateEvent;
import com.twistedmcstudios.core.common.update.UpdateType;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.reboot.RebootManager;
import com.twistedmcstudios.core.tasks.Task;
import com.twistedmcstudios.survival.Survival;
import com.twistedmcstudios.survival.tasks.TaskMenu;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PlayerManager extends Module {

    private PlayerManager() { super("Player Manager"); }
    private static final PlayerManager instance = new PlayerManager();
    public static PlayerManager getInstance() { return instance; }

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public static Map<UUID, SurvivalScoreboard> playerScoreboards = new HashMap<>();

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        SurvivalScoreboard scoreboard = new SurvivalScoreboard();
        player.setScoreboard(scoreboard.getScoreboard());
        playerScoreboards.put(player.getUniqueId(), scoreboard);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clear the player's scoreboard
        player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());

        // Cancel and remove the player's scoreboard update task
        playerScoreboards.remove(player.getUniqueId());
    }

    private Map<UUID, Boolean> isAquaMap = new HashMap<>();

    @EventHandler
    public void UpdateLines(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboardLines(playerScoreboards.get(player.getUniqueId()), player);
        }
    }

    @EventHandler
    public void UpdateTitle(UpdateEvent event) {
        if (event.getType() != UpdateType.FIFTY) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerScoreboards.get(player.getUniqueId()) != null) {
                playerScoreboards.get(player.getUniqueId()).updateTitle();
            }
        }
    }

    private void updateScoreboardLines(SurvivalScoreboard scoreboard, Player player) {

        Account account = new Account(player);


        scoreboard.reset();

        scoreboard.write(scoreboard.centerText(
                c.gray + PlaceholderAPI.setPlaceholders(player, "%localtime_timezone_America/New_York,MM/dd/yyyy%") + " " + c.dgray + Core.getServerDataManager().getServerIdentifier().getIdentifier(),
                25
        ));

        scoreboard.writeNewLine();
        scoreboard.write("&#87CEEBᴄᴏɪɴs: &6" + formatter.format(account.getManager().getStatInt(Stat.COINS)));
        scoreboard.write("&#87CEEBɢᴇᴍs: &2" + formatter.format(account.getManager().getStatInt(Stat.GEMS)));
        scoreboard.write("&#87CEEBʟɪᴠᴇs: &4" + formatter.format(account.getManager().getStatInt(Stat.SURVIVAL_LIVES)));
        scoreboard.write("&#87CEEBᴘʟᴀʏᴇʀ ᴋɪʟʟs: &c" + formatter.format(account.getManager().getStatInt(Stat.SURVIVAL_PLAYERS_KILLED)));
        scoreboard.write("&#87CEEBᴛᴀsᴋ ᴘᴏɪɴᴛs: &#6effca" + formatter.format(account.getManager().getStatInt(Stat.SURVIVAL_TASK_POINTS)));

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));

        ZonedDateTime newTasksTime = ZonedDateTime.of(now.toLocalDate(), LocalTime.of(23, 59, 59), ZoneId.of("America/New_York"));

        String newTasks = getTimeRemaining(newTasksTime.toLocalDateTime());

        if (account.getManager().getNumberOfNotStartedTasks() >= 1) {
            scoreboard.writeNewLine();
            UUID playerId = player.getUniqueId();
            Boolean isAqua = isAquaMap.getOrDefault(playerId, true);
            String message = isAqua ? "&#00ffc8➔ " + account.getManager().getNumberOfNotStartedTasks() + " tasks pending!"
                    : "&#039173➔ " + account.getManager().getNumberOfNotStartedTasks() + " tasks pending!";
            isAquaMap.put(playerId, !isAqua);
            scoreboard.write(message);
            scoreboard.write(c.white + " /tasks");
        } else if (account.getManager().getNumberOfNotStartedTasks() == 0) {
            scoreboard.writeNewLine();
            scoreboard.write(c.aqua + "➔ " + "&#87CEEBNew tasks " + c.white + newTasks);
        }

        if (account.getManager().hasStartedTask()) {
            try {
                Task task = account.getManager().getCurrentTask();

                scoreboard.writeNewLine();
                scoreboard.write("&#9a52ff✦ " + (task.getTaskType().equals(Task.TaskType.COLLECT) ? "&#9a52ff" + "Collect" : "&#9a52ff" + "Kill")
                        + " " + "&#9a52ff" + task.getProgress() + c.white + "/" +
                        "&#9a52ff" + task.getCount());
                scoreboard.write("&#9a52ff" + (task.getTaskType().equals(Task.TaskType.COLLECT) ? TaskMenu.formatMaterialName(task.getTargetMaterial().name()) : TaskMenu.formatCapitalization(task.getTarget())));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }


        if (RebootManager.timeRemaining <= 2700) {
            scoreboard.writeNewLine();

            int seconds = RebootManager.timeRemaining;
            ChatColor textColor;
            if (seconds <= 30) {
                textColor = seconds % 2 == 0 ? ChatColor.WHITE : ChatColor.RED;
            } else {
                textColor = ChatColor.RED;
            }

            if (!RebootManager.getInstance().getFormattedTime().contains("-")) {
                scoreboard.write(textColor + "Instance rebooting " + RebootManager.getInstance().getFormattedTime());
            } else {
                scoreboard.write(textColor + "Instance rebooting soon");
            }
        }

        scoreboard.writeNewLine();

        scoreboard.write("&#3498DB➥ sᴛᴏʀᴇ.ᴛᴡɪsᴛᴇᴅᴍᴄ.ɴᴇᴛ");

        scoreboard.draw();
    }

    public void GiveScoreboard(Player player) {
        SurvivalScoreboard scoreboard = new SurvivalScoreboard();
        player.setScoreboard(scoreboard.getScoreboard());
        playerScoreboards.put(player.getUniqueId(), scoreboard);
    }

    public static String formatValue(double value) {
        NumberFormat fmt = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        return fmt.format(value);
    }

    public static String getTimeRemaining(LocalDateTime targetDateTime) {
        ZoneId newYorkZone = ZoneId.of("America/New_York");
        ZonedDateTime currentDateTime = ZonedDateTime.now(newYorkZone);
        ZonedDateTime targetZonedDateTime = targetDateTime.atZone(newYorkZone);

        Duration duration = Duration.between(currentDateTime, targetZonedDateTime);

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        String formattedDuration;
        if (days > 0) {
            formattedDuration = String.format("%02dd %02dh %02dm", days, hours, minutes);
        } else if (hours > 0) {
            formattedDuration = String.format("%02dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            formattedDuration = String.format("%02dm %02ds", minutes, seconds);
        } else {
            formattedDuration = String.format("%02ds", seconds);
        }

        return formattedDuration;
    }

    /*@EventHandler
    public void Rewards(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Account account = new Account(player);
            try {
                account.getManager().giveRandomReward();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }*/
}
