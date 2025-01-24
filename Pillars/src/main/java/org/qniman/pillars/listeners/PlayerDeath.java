package org.qniman.pillars.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.qniman.pillars.managers.GameManager;

public class PlayerDeath implements Listener {
    GameManager gm;

    public PlayerDeath(GameManager gm) {
        this.gm = gm;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (gm.isGameRunning) {
            Player player = e.getPlayer();
            player.getInventory().clear();
            gm.playingPlayers.remove(player);
            if (gm.playingPlayers.size() - 1 > 1) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }
}
