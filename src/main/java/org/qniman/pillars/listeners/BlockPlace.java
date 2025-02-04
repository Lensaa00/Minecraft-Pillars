package org.qniman.pillars.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.qniman.pillars.managers.GameManager;

public class BlockPlace implements Listener {
    GameManager gameManager;

    public BlockPlace(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (gameManager.isGameRunning) {
            gameManager.spawnedBlocks.add(e.getBlock().getLocation());
        }
    }
}
