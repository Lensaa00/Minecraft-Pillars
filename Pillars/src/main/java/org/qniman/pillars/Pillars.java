package org.qniman.pillars;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.boss.*;

import java.util.*;

public final class Pillars extends JavaPlugin {

    //Префикс чата для режима
    String chatPrefix = ChatColor.YELLOW + "[Столбы] " + ChatColor.WHITE;

    // Переменные режима
    boolean isGameRunning = false;
    World playWorld = null;
    List<Player> players = new ArrayList<>();
    List<Player> readyPlayers = new ArrayList<>();
    List<Player> playingPlayers = new ArrayList<>();
    int minPlayers = 2;

    // Переменные для платформы лобби
    Location lobbyLocation;
    Material lobbyMaterial = Material.BEDROCK;
    int lobbyRadius = 20;
    List<Location> lobbyBlocks = new ArrayList<>();

    // Переменные для настройки столбов
    Material pillarMaterial = Material.BEDROCK;
    int pillarHeight = 10;
    float pillarRadius = 10;
    Location pillarsCenterLocation = new Location(Bukkit.getWorld("world"), 2500, 0, 2500);
    List<Location> pillarBlocks = new ArrayList<>();

    // Игровые переменные
    int shedulerTaskId = -1;
    int giveItemTimer = 0;
    int giveItemDelay = 10;
    List<Location> spawnedBlocks = new ArrayList<>();
    BossBar bossBar;

    // Включение плагина
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        this.getCommand("pillars").setExecutor(new PillarsCommand());
        playWorld = Bukkit.getWorld("world");
        lobbyLocation = new Location(playWorld, 0, 0, 0);
        createLobbyPlatform();

        bossBar = Bukkit.createBossBar("Следующий предмет", BarColor.PINK, BarStyle.SEGMENTED_10);
        bossBar.setVisible(false);
    }

    // Выключение плагина
    @Override
    public void onDisable() {
    }

    // Создание платформы-лобби
    void createLobbyPlatform() {
        int startX = lobbyLocation.getBlockX() - lobbyRadius / 2;
        int startZ = lobbyLocation.getBlockZ() - lobbyRadius / 2;

        for (int col = 0; col < lobbyRadius; col++) {
            startX = lobbyRadius - col;
            for (int row = 0; row < lobbyRadius; row++) {
                startZ = lobbyRadius - row;
                Location blockLocation = new Location(playWorld, startX, lobbyLocation.getBlockY(), startZ);
                lobbyBlocks.add(blockLocation);
                blockLocation.getBlock().setType(lobbyMaterial);
            }
        }
    }

    // Функция входа игрока в игру
    void joinGame(Player player) {
        if (isGameRunning) {
            player.sendMessage(chatPrefix + "Игра уже начата! Вы не можете присоединиться к игре.");
        } else {
            if (!readyPlayers.contains(player)) {
                readyPlayers.add(player);
                Bukkit.broadcastMessage(chatPrefix + player.getName() + " присоединился к игре!");
                bossBar.addPlayer(player);

                if (readyPlayers.size() < minPlayers) {
                    int remainingPlayers = minPlayers - readyPlayers.size();
                    Bukkit.broadcastMessage(chatPrefix + ChatColor.GREEN + "Осталось игроков до старта игры: " + remainingPlayers);
                } else {
                    Bukkit.broadcastMessage(chatPrefix + ChatColor.GREEN + "Вы можете начать игру, написав команду /pillars start");
                }
            } else {
                player.sendMessage(chatPrefix + "Вы уже учавствуете в игре!");
            }
        }
    }

    // Функция выхода игрока из игры
    void leaveGame(Player player) {
        if (isGameRunning) {
            player.sendMessage(chatPrefix + Color.RED + "Вы не можете покинуть активную игру");
        } else {
            if (readyPlayers.contains(player)) {
                readyPlayers.remove(player);
                Bukkit.broadcastMessage(chatPrefix + player.getName() + " больше не учавствует в игре");
            } else {
                player.sendMessage(chatPrefix + "Вы не находитесь в игре, чтобы ее покинуть");
            }
        }
    }

    // Функция создания столба
    void createPillar(Location location) {
        int locationX = location.getBlockX();
        int locationY = location.getBlockY();
        int locationZ = location.getBlockZ();

        for (int i = 1; i <= pillarHeight; i++) {
            Location spawnLocation = new Location(playWorld, locationX, locationY - i, locationZ);
            spawnLocation.getBlock().setType(pillarMaterial);
            pillarBlocks.add(spawnLocation);
        }
    }

    // Функция создания столбов для игроков
    void createPillars() {
        int playerCount = playingPlayers.size();
        double angleIncrement = 2 * Math.PI / playerCount; // Угол между игроками

        for (int i = 0; i < playerCount; i++) {
            // Вычисление угла для игрока
            double angle = i * angleIncrement;

            // Координаты на круге
            double x = pillarsCenterLocation.getX() + pillarRadius * Math.cos(angle);
            double z = pillarsCenterLocation.getZ() + pillarRadius * Math.sin(angle);
            double y = pillarsCenterLocation.getY();

            // Создаём столб
            Location pillarLocation = new Location(playWorld, x, y, z);
            createPillar(pillarLocation);

            // Перемещаем игрока на вершину столба
            Player player = playingPlayers.get(i);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.getInventory().clear();
            player.teleport(pillarLocation.clone().add(0, pillarHeight, 0)); // Вверх на высоту столба
        }
    }

    // Удаление столбов
    void removePillars() {
        for (Location location : pillarBlocks) {
            location.getBlock().setType(Material.AIR);
        }
        pillarBlocks.clear();
    }

    // Очистка блоков игроков
    void clearPlayerBlocks() {
        for (Location location : spawnedBlocks) {
            location.getBlock().setType(Material.AIR);
        }
        spawnedBlocks.clear();
    }

    // Выдача предметов
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

    // Основной цикл игры
    void gameLoop() {
        if (isGameRunning) {
            if (giveItemTimer < giveItemDelay) {
                giveItemTimer++;
            } else {
                for (Player ply : playingPlayers) {
                    giveItem(ply);
                }
                giveItemTimer = 0;
            }

            bossBar.setVisible(true);
            bossBar.setProgress((double) giveItemTimer / 10);


            if (playingPlayers.size() <= 1) {
                Player winner = playingPlayers.get(0);
                Bukkit.broadcastMessage(chatPrefix + winner.getName() + ChatColor.GREEN + " выиграл в игре!");
                endGame();
            }
        }
    }

    // Функция начала игры
    void startGame(Player player) {
        if (isGameRunning) {
            player.sendMessage(chatPrefix + "Игра уже запущена.");
        } else {
            if (readyPlayers.size() >= minPlayers) {
                isGameRunning = true;

                playingPlayers = new ArrayList<>(readyPlayers);
                readyPlayers.clear();

                bossBar.setVisible(true);

                createPillars();
                Bukkit.broadcastMessage(chatPrefix + ChatColor.GREEN + "Игра началась!");
                shedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::gameLoop, 0, 20L);

            } else {
                player.sendMessage(chatPrefix + "Недостаточно игроков. Минимум игроков для начала: " + minPlayers);
            }
        }
    }

    // Функция окончания игры
    void endGame() {
        if (isGameRunning) {
            removePillars();
            clearPlayerBlocks();
            Bukkit.getScheduler().cancelTask(shedulerTaskId);

            for (Player player : playingPlayers) {
                player.teleport(lobbyLocation);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.getInventory().clear();
                bossBar.removePlayer(player);
            }

            playingPlayers.clear();
            giveItemTimer = 0;
            bossBar.setVisible(false);
            isGameRunning = false;
        }
    }

    // Класс команды /pillars
    public class PillarsCommand implements CommandExecutor {
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
                    joinGame((Player) sender);
                    break;
                case "leave":
                    leaveGame((Player) sender);
                    break;
                case "start":
                    startGame((Player) sender);
                    break;
            }

            return true;
        }
    }

    // Слушатель эвентов игры
    public class EventListener implements Listener {
        @EventHandler
        public void onPlayerSpawnBlock(BlockPlaceEvent event) {
            if (isGameRunning) {
                spawnedBlocks.add(event.getBlock().getLocation());
            }
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (isGameRunning) {
                Player player = event.getPlayer();
                player.getInventory().clear();
                playingPlayers.remove(player);
                if (playingPlayers.size() - 1 > 1) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        }
    }
}
