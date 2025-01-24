package org.qniman.pillars;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.qniman.pillars.listeners.BlockPlace;
import org.qniman.pillars.listeners.PlayerDeath;
import org.qniman.pillars.managers.GameManager;

public final class Pillars extends JavaPlugin {

    GameManager gameManager;

    // Включение плагина
    @Override
    public void onEnable() {
        gameManager = new GameManager(this);

        getServer().getPluginManager().registerEvents(new BlockPlace(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeath(gameManager), this);

        this.getCommand("pillars").setExecutor(new Commands(gameManager));
    }

    // Выключение плагина
    @Override
    public void onDisable() {
    }
}
