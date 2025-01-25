package org.qniman.pillars.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerManager {
    GameManager gameManager;

    public PlayerManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    // ВЫДАЧА РАНДОМОГО ПРЕДМЕТА ИГРОКУ
    public void giveItem(Player player) {
        Material[] materials = Material.values();
        Random random = new Random();
        Material randomMaterial = materials[random.nextInt(materials.length)];
        while (!randomMaterial.isItem()) {
            randomMaterial = materials[random.nextInt(materials.length)];
        }
        ItemStack randomItem = new ItemStack(randomMaterial);

        player.getInventory().addItem(randomItem);
    }

    public void broadcastTitle(String title, String subtitle){
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : players) {
            player.sendTitle(title, subtitle, 10, 10, 10);
        }
    }
}
