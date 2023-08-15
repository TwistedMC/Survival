package com.twistedmcstudios.survival.tasks;

import com.twistedmcstudios.core.account.Account;
import com.twistedmcstudios.core.common.gui.AbstractGUI;
import com.twistedmcstudios.core.common.gui.ScrollerInventory;
import com.twistedmcstudios.core.common.gui.item.cItemStack;
import com.twistedmcstudios.core.common.util.UtilSound;
import com.twistedmcstudios.core.common.util.c;
import com.twistedmcstudios.core.tasks.Task;
import com.twistedmcstudios.survival.Survival;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class TaskMenu extends AbstractGUI {


    /*
    00 01 02 03 04 05 06 07 08
    09 10 11 12 13 14 15 16 17
    18 19 20 21 22 23 24 25 26
    27 28 29 30 31 32 33 34 35
    36 37 38 39 40 41 42 43 44
    45 46 47 48 49 50 51 52 53
    */

    public TaskMenu(Player player) {
        super(6, "Your Tasks");

        Account account = new Account(player);

        account.playSound(Sound.UI_BUTTON_CLICK, 10, 1.5F);

        ZoneId estZoneId = ZoneId.of("America/New_York");
        LocalDate dateToCheck = LocalDate.now(estZoneId);

        LocalDate startRange = LocalDate.of(dateToCheck.getYear(), 7, 17);
        LocalDate endRange = LocalDate.of(dateToCheck.getYear(), 8, 17);

        if (dateToCheck.isAfter(endRange) ) {
            player.sendMessage(c.red + "Season over! Join our Discord for updates: discord.gg/twistedmc");
            account.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 10, 0.7F);
            player.closeInventory();
            return;
        }

        setStaticItem(cItemStack.toItemStack(player.getUniqueId()), 4);

        List<Task> tasks;
        try {
            tasks = account.getManager().getPlayerTasks();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        tasks = tasks.stream().filter(t -> t.getStatus() == Task.Status.NOT_STARTED || t.getStatus() == Task.Status.STARTED).collect(Collectors.toList());

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            setItem(new cItemStack(task.getTargetMaterial()).setDisplayName(task.isExpired() ? c.red + "Expired " + task.getTaskType().getName() : c.green + task.getTaskType().getName() + c.gray + c.italics + " (" + task.getFormattedTimeForTask() + ")")
                    .addLore(c.dgray + "Task", "")
                    .addLore(
                            (task.getTaskType().equals(Task.TaskType.COLLECT) ? c.white + "Collect" : c.white + "Kill")
                                    + " " + c.aqua + task.getProgress() + c.white + "/" +
                                    c.aqua + task.getCount() + c.white + " " + (task.getTaskType().equals(Task.TaskType.COLLECT) ? formatMaterialName(task.getTargetMaterial().name()) : formatCapitalization(task.getTarget())))
                    .addLore("", c.gray + "Expires in: " + c.white + task.getRemainingTimeUntilExpiration())
                    .addLore("", c.gray + "Reward: " + ChatColor.of("#6effca") + task.getTaskPoints() + " Points")
                    .addLore(
                            "",
                            task.getStatus().equals(Task.Status.NOT_STARTED) ? c.yellow + "Click to start task!" : c.red + "Click to trash task!"),i % 10, (p) -> {

                if (task.getStatus().equals(Task.Status.STARTED)) {
                    account.playSound(Sound.ITEM_TRIDENT_RETURN, 10, 0.7F);
                    player.closeInventory();
                    task.setStatus(Task.Status.TRASHED);
                    account.getManager().setTaskTrashTime(task.getId(), System.currentTimeMillis());
                    player.sendMessage(c.yellow + "Task trashed! You will receive a new one in " + c.gold + "5 minutes" + c.yellow + "!");
                } else if (task.getStatus().equals(Task.Status.NOT_STARTED)) {

                    if (account.getManager().hasStartedTask()) {
                        account.playSound(Sound.ITEM_TRIDENT_RETURN, 10, 0.7F);
                        player.closeInventory();
                        player.sendMessage(c.red + "You already have an active task!");
                    } else {
                        UtilSound.jingleBellSound(player);
                        player.closeInventory();
                        task.setStatus(Task.Status.STARTED);
                        account.getManager().setTaskStartTime(task.getId(), System.currentTimeMillis());
                        player.sendMessage(c.yellow + "Task started! " + (task.getTaskType().equals(Task.TaskType.COLLECT) ? c.white + "Collect" : c.white + "Kill")
                                + " " +
                                c.aqua + task.getCount() + c.white + " " + (task.getTaskType().equals(Task.TaskType.COLLECT) ? formatMaterialName(task.getTargetMaterial().name()) : formatCapitalization(task.getTarget())));
                        if (task.getTaskType().equals(Task.TaskType.COLLECT)) {
                            player.sendMessage(c.red + "WARNING: Shift-clicking the output in a crafting table will not count towards progress! Please collect items one at a time.");
                        }
                    }
                }
            });
        }

        setStaticItem(new cItemStack(Material.LIGHT_BLUE_SHULKER_BOX).setDisplayName(c.aqua + "Points Shop")
                .addEnchant(Enchantment.DURABILITY, 1)
                .addFlags(ItemFlag.HIDE_ENCHANTS),48, (p) -> {
            player.closeInventory();
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Survival.getInstance(), () -> {
                TaskShop taskShop = new TaskShop(player);
                taskShop.openInventory(player);
            }, 2L);

        });

        setStaticItem(new cItemStack(Material.BARRIER).setDisplayName(c.red + "Close"),49, (p) -> {
            player.closeInventory();
        });

        ScrollerInventory scrollerInventory = ScrollerInventory.getPlayerInstance(getInventoryTitle(), this, player, tasks.size());
        scrollerInventory.openForPlayer(player);

    }

    public static String formatCapitalization(String input) {
        String[] words = input.replaceAll("_", " ").split("\\s+");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                String formattedWord = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                result.append(formattedWord).append(" ");
            }
        }

        return result.toString().trim();
    }


    public static String formatMaterialName(String materialName) {
        String[] words = materialName.split("_");
        StringBuilder formattedName = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                formattedName.append(" ");
            }

            String word = words[i];
            if (word.length() > 0) {
                String firstLetter = word.substring(0, 1).toUpperCase();
                String restOfWord = word.substring(1).toLowerCase();
                formattedName.append(firstLetter).append(restOfWord);
            }
        }

        return formattedName.toString();
    }


}
