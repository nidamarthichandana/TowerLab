package com.towerlab.engine.model;

import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.exception.InsufficientFundsException;
import com.towerlab.engine.exception.InvalidActionException;
import com.towerlab.engine.exception.InvalidPlacementException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the mutable game session state per GR-4, GR-9, GR-27, GR-33.
 * Maintains invariants on gold, base health, tower placement, enemy tracking, and status transitions.
 * Does not implement simulation loop, attacking, or win/loss evaluation.
 */
public final class GameState {

    private final GameMap map;
    private final LevelConfig levelConfig;

    private GameStatus status;
    private int baseHealth;
    private int gold;
    private int currentWaveNumber;
    private int tick;

    private final Map<Integer, Enemy> enemies;
    private final Map<Integer, Tower> towers;

    private int nextEnemyId;
    private int nextTowerId;

    /**
     * Constructs a GameState from a GameMap and LevelConfig.
     *
     * @param map         the game map
     * @param levelConfig the level configuration
     */
    public GameState(GameMap map, LevelConfig levelConfig) {
        if (map == null) {
            throw new IllegalArgumentException("GameMap cannot be null");
        }
        if (levelConfig == null) {
            throw new IllegalArgumentException("LevelConfig cannot be null");
        }
        this.map = map;
        this.levelConfig = levelConfig;

        this.status = GameStatus.READY;
        this.baseHealth = levelConfig.startingBaseHealth();
        this.gold = levelConfig.startingGold();
        this.currentWaveNumber = 0;
        this.tick = 0;

        this.enemies = new LinkedHashMap<>();
        this.towers = new LinkedHashMap<>();

        this.nextEnemyId = 0;
        this.nextTowerId = 0;
    }

    public GameMap map() {
        return map;
    }

    public LevelConfig levelConfig() {
        return levelConfig;
    }

    public GameStatus status() {
        return status;
    }

    public int baseHealth() {
        return baseHealth;
    }

    public int gold() {
        return gold;
    }

    public int currentWaveNumber() {
        return currentWaveNumber;
    }

    public int tick() {
        return tick;
    }

    public int nextEnemyId() {
        return nextEnemyId++;
    }

    public int nextTowerId() {
        return nextTowerId++;
    }

    /**
     * Changes the current game status. Rejects changes away from terminal states (WON, LOST).
     *
     * @param newStatus the new status
     */
    public void changeStatus(GameStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("GameStatus cannot be null");
        }
        if (this.status == GameStatus.WON || this.status == GameStatus.LOST) {
            if (newStatus != this.status) {
                throw new InvalidActionException("Cannot change status away from terminal status: " + this.status + " to " + newStatus);
            }
        }
        this.status = newStatus;
    }

    /**
     * Adds an enemy to the current active enemies.
     *
     * @param enemy the enemy to add
     */
    public void addEnemy(Enemy enemy) {
        if (enemy == null) {
            throw new IllegalArgumentException("Enemy cannot be null");
        }
        if (enemies.containsKey(enemy.id())) {
            throw new IllegalArgumentException("Enemy id already in use: " + enemy.id());
        }
        enemies.put(enemy.id(), enemy);
    }

    /**
     * Removes an enemy by its unique ID.
     *
     * @param id the enemy id
     * @return true if an enemy was removed, false otherwise
     */
    public boolean removeEnemy(int id) {
        return enemies.remove(id) != null;
    }

    /**
     * Places a tower on the grid.
     * Rejects placement if the cell is not buildable, already holds a tower, or if the tower ID is duplicated.
     *
     * @param tower the tower to place
     */
    public void addTower(Tower tower) {
        if (tower == null) {
            throw new IllegalArgumentException("Tower cannot be null");
        }
        if (!map.isBuildable(tower.position())) {
            throw new InvalidPlacementException("Cell " + tower.position() + " is not buildable");
        }
        for (Tower existing : towers.values()) {
            if (existing.position().equals(tower.position())) {
                throw new InvalidPlacementException("Cell " + tower.position() + " already holds a tower");
            }
        }
        if (towers.containsKey(tower.id())) {
            throw new InvalidPlacementException("Tower id already in use: " + tower.id());
        }
        towers.put(tower.id(), tower);
    }

    /**
     * Removes a tower by ID, freeing its cell for future placement.
     *
     * @param id the tower id
     * @return true if a tower was removed, false otherwise
     */
    public boolean removeTower(int id) {
        return towers.remove(id) != null;
    }

    /**
     * Advances the game simulation tick counter by 1.
     */
    public void advanceTick() {
        this.tick++;
    }

    /**
     * Advances to the next wave number.
     */
    public void startNextWave() {
        this.currentWaveNumber++;
    }

    /**
     * Spends gold. Rejects non-positive amounts and amounts exceeding current balance.
     *
     * @param amount gold amount to spend
     */
    public void spendGold(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to spend must be positive, got: " + amount);
        }
        if (amount > gold) {
            throw new InsufficientFundsException("Insufficient gold: required " + amount + ", but current gold is " + gold);
        }
        this.gold -= amount;
    }

    /**
     * Adds gold to the current balance. Rejects non-positive amounts.
     *
     * @param amount gold amount to add
     */
    public void addGold(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to add must be positive, got: " + amount);
        }
        this.gold += amount;
    }

    /**
     * Reduces base health with a floor of 0 and returns the actual health lost.
     *
     * @param amount raw damage to base
     * @return health actually deducted
     */
    public int damageBase(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Damage amount must be positive, got: " + amount);
        }
        int actualLoss = Math.min(this.baseHealth, amount);
        this.baseHealth -= actualLoss;
        return actualLoss;
    }

    /**
     * Returns an unmodifiable snapshot copy of all active enemies in ascending ID order.
     *
     * @return sorted unmodifiable list of enemies
     */
    public List<Enemy> enemies() {
        List<Enemy> list = new ArrayList<>(enemies.values());
        list.sort(Comparator.comparingInt(Enemy::id));
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns an unmodifiable snapshot copy of all placed towers in ascending ID order.
     *
     * @return sorted unmodifiable list of towers
     */
    public List<Tower> towers() {
        List<Tower> list = new ArrayList<>(towers.values());
        list.sort(Comparator.comparingInt(Tower::id));
        return Collections.unmodifiableList(list);
    }

    public Optional<Tower> towerAt(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        for (Tower tower : towers.values()) {
            if (tower.position().equals(position)) {
                return Optional.of(tower);
            }
        }
        return Optional.empty();
    }

    public Optional<Enemy> enemy(int id) {
        return Optional.ofNullable(enemies.get(id));
    }

    public Optional<Tower> tower(int id) {
        return Optional.ofNullable(towers.get(id));
    }
}
