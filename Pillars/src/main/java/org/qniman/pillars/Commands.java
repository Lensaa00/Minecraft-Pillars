package org.qniman.pillars;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.qniman.pillars.managers.GameManager;

public class Commands implements CommandExecutor {
    GameManager gameManager;

    public Commands(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Сейчас вы не можете использовать эту команду.");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Ошибка!" + ChatColor.WHITE + " Использование: " + command.getUsage());
            return false;
        }

        switch (args[0]) {
            case "join":
                gameManager.joinGame((Player) sender);
                break;
            case "leave":
                gameManager.leaveGame((Player) sender);
                break;
            case "start":
                gameManager.startGame((Player) sender);
                break;
        }

        return true;
    }
}
