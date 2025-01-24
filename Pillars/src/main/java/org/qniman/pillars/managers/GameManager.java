package org.qniman.pillars.managers;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GameManager {

    JavaPlugin plugin; // Плагин

    PlayerManager playerManager; // Менеджер игроков

    // Конфигурация режима //
    public int minPlayers = 2; // Минимум игроков для запуска режима

    int lobbyRadius = 20; // Радиус лобби
    int pillarHeight = 10; // Высота столбов
    float pillarRadius = 10; // Радиус на котором спавнятся столбы
    Material pillarMaterial = Material.BEDROCK; // Материал столбов
    Location pillarsCenterLocation = new Location(Bukkit.getWorld("world"), 2500, 0, 2500); // Позиция лобби
    Material lobbyMaterial = Material.BEDROCK; // Материал лобби
    int giveItemDelay = 10; // Задержка между выдачей предметов
    String chatPrefix = ChatColor.YELLOW + "[Столбы] " + ChatColor.WHITE;

    // Переменные режима
    public boolean isGameRunning = false;
    public List<Player> readyPlayers = new ArrayList<>();
    public List<Player> playingPlayers = new ArrayList<>();
    public List<Location> spawnedBlocks = new ArrayList<>();

    World playWorld = null;
    int shedulerTaskId = -1;
    int giveItemTimer = 0;
    BossBar bossBar;
    List<Location> pillarBlocks = new ArrayList<>();
    Location lobbyLocation;
    List<Location> lobbyBlocks = new ArrayList<>();

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerManager = new PlayerManager(this);
        this.bossBar = Bukkit.createBossBar("Следующий блок", BarColor.YELLOW, BarStyle.SEGMENTED_20);
        bossBar.setProgress(0);
        bossBar.setVisible(false);
    }

    // ФУНКЦИЯ НАЧАЛА ИГРЫ
    public void startGame(Player player) {
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
                shedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::gameLoop, 0, 20L);

            } else {
                player.sendMessage(chatPrefix + "Недостаточно игроков. Минимум игроков для начала: " + minPlayers);
            }
        }
    }

    // ФУНКЦИЯ ВХОДА ИГРОКА В ИГРУ
    public void joinGame(Player player) {
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

    // ФУНКЦИЯ ВЫХОДА ИГРОКА ИЗ ИГРЫ
    public void leaveGame(Player player) {
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

    // ФУНКЦИЯ ОКОНЧАНИЯ ИГРЫ
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

    // ОСНОВНОЙ ЦИКЛ РЕЖИМА
    void gameLoop() {
        if (isGameRunning) {
            if (giveItemTimer < giveItemDelay) {
                giveItemTimer++;
            } else {
                for (Player ply : playingPlayers) {
                    playerManager.giveItem(ply);
                }
                giveItemTimer = 0;
            }

            bossBar.setVisible(true);
            bossBar.setProgress(20D - 20D / giveItemTimer);


            if (playingPlayers.size() <= 1) {
                Player winner = playingPlayers.get(0);
                Bukkit.broadcastMessage(chatPrefix + winner.getName() + ChatColor.GREEN + " выиграл в игре!");
                endGame();
            }
        }
    }

    // ФУНКЦИЯ СОЗДАНИЯ СТОЛБА
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

    // ФУНКЦИЯ СОЗДАНИЯ СТОЛБОВ ДЛЯ ИГРОКОВ
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

    // УДАЛЕНИЕ ВСЕХ СТОЛБОВ
    void removePillars() {
        for (Location location : pillarBlocks) {
            location.getBlock().setType(Material.AIR);
        }
        pillarBlocks.clear();
    }

    // ОЧИСТКА БЛОКОВ ИГРОКОВ
    void clearPlayerBlocks() {
        for (Location location : spawnedBlocks) {
            location.getBlock().setType(Material.AIR);
        }
        spawnedBlocks.clear();
    }

    // СОЗДАНИЕ ПЛАТФОРМЫ-ЛОББИ
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
}
