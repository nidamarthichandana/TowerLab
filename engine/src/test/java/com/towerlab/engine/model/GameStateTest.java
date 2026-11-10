package com.towerlab.engine.model;

import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.SpawnGroupConfig;
import com.towerlab.engine.config.TargetType;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.config.WaveConfig;
import com.towerlab.engine.exception.InsufficientFundsException;
import com.towerlab.engine.exception.InvalidActionException;
import com.towerlab.engine.exception.InvalidPlacementException;
import com.towerlab.engine.strategy.TargetingMode;
import com.towerlab.engine.test.TestLevelHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTest {

    private final List<Position> waypoints = List.of(
            new Position(0, 1),
            new Position(6, 1),
            new Position(6, 5),
            new Position(10, 5),
            new Position(10, 2),
            new Position(11, 2)
    );

    private final LevelConfig levelConfig = TestLevelHelper.createLevel(
            "Level 1", 12, 8, waypoints, List.of("Cannon"),
            List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0)))),
            150, 20, 100
    );

    private final TowerConfig cannonConfig = new TowerConfig(
            "Cannon", "MVP", 60, 2.5, 12.0, 12, TargetType.GROUND, 40, 1.0, 0.0, 0, "Splash"
    );

    private GameMap map;
    private GameState state;

    @BeforeEach
    void setUp() {
        map = new GameMap(levelConfig);
        state = new GameState(map, levelConfig);
    }

    @Test
    @DisplayName("GameState initializes with starting gold (150), base health (20), READY, wave 0, tick 0, empty collections")
    void testInitialValues() {
        assertEquals(150, state.gold());
        assertEquals(20, state.baseHealth());
        assertEquals(GameStatus.READY, state.status());
        assertEquals(0, state.currentWaveNumber());
        assertEquals(0, state.tick());
        assertTrue(state.enemies().isEmpty());
        assertTrue(state.towers().isEmpty());
        assertEquals(map, state.map());
        assertEquals(levelConfig, state.levelConfig());
    }

    @Test
    @DisplayName("spendGold: success, exact balance, insufficient funds, and non-positive rejection")
    void testSpendGold() {
        // Spend normal amount
        state.spendGold(50);
        assertEquals(100, state.gold());

        // Spend exact remaining balance
        state.spendGold(100);
        assertEquals(0, state.gold());

        // Overspending throws InsufficientFundsException
        assertThrows(InsufficientFundsException.class, () -> state.spendGold(1));

        // Non-positive amounts rejected
        assertThrows(IllegalArgumentException.class, () -> state.spendGold(0));
        assertThrows(IllegalArgumentException.class, () -> state.spendGold(-10));
    }

    @Test
    @DisplayName("addGold: adds gold and rejects non-positive amounts")
    void testAddGold() {
        state.addGold(20);
        assertEquals(170, state.gold());

        assertThrows(IllegalArgumentException.class, () -> state.addGold(0));
        assertThrows(IllegalArgumentException.class, () -> state.addGold(-5));
    }

    @Test
    @DisplayName("damageBase reduces health, floors at 0, returns real loss, and rejects non-positive damage")
    void testDamageBase() {
        // Partial damage
        int loss1 = state.damageBase(5);
        assertEquals(5, loss1);
        assertEquals(15, state.baseHealth());

        // Excess damage: floors at 0 and returns actual remaining health lost
        int loss2 = state.damageBase(25);
        assertEquals(15, loss2, "Actual loss must be capped at current health");
        assertEquals(0, state.baseHealth(), "Base health must floor at 0");

        // Further damage when 0 health
        int loss3 = state.damageBase(10);
        assertEquals(0, loss3);
        assertEquals(0, state.baseHealth());

        // Non-positive damage rejected
        assertThrows(IllegalArgumentException.class, () -> state.damageBase(0));
        assertThrows(IllegalArgumentException.class, () -> state.damageBase(-3));
    }

    @Test
    @DisplayName("Defensive copies: modifying returned lists does not affect internal state")
    void testDefensiveCopies() {
        CannonTower tower = new CannonTower(1, cannonConfig, new Position(0, 0), TargetingMode.FIRST.strategy());
        state.addTower(tower);

        BasicEnemy enemy = new BasicEnemy(1);
        state.addEnemy(enemy);

        List<Tower> towersList = state.towers();
        List<Enemy> enemiesList = state.enemies();

        assertThrows(UnsupportedOperationException.class, () -> towersList.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> enemiesList.remove(0));

        // Adding another entity to state creates fresh snapshots
        BasicEnemy enemy2 = new BasicEnemy(2);
        state.addEnemy(enemy2);
        assertEquals(1, enemiesList.size(), "Original snapshot must remain unchanged");
        assertEquals(2, state.enemies().size(), "New query reflects updated state");
    }

    @Test
    @DisplayName("addTower rejects non-buildable cells, duplicate cells, and duplicate tower IDs")
    void testTowerPlacementValidation() {
        // 1. Non-buildable cell (on the path)
        Position pathCell = new Position(0, 1);
        CannonTower pathTower = new CannonTower(1, cannonConfig, pathCell, TargetingMode.FIRST.strategy());
        assertThrows(InvalidPlacementException.class, () -> state.addTower(pathTower));

        // 2. Non-buildable cell (outside the grid)
        Position outCell = new Position(-1, 0);
        CannonTower outTower = new CannonTower(2, cannonConfig, outCell, TargetingMode.FIRST.strategy());
        assertThrows(InvalidPlacementException.class, () -> state.addTower(outTower));

        // 3. Valid placement
        Position validPos = new Position(0, 0);
        CannonTower tower1 = new CannonTower(10, cannonConfig, validPos, TargetingMode.FIRST.strategy());
        state.addTower(tower1);
        assertEquals(1, state.towers().size());
        assertTrue(state.towerAt(validPos).isPresent());

        // 4. Duplicate cell placement
        CannonTower duplicateCellTower = new CannonTower(11, cannonConfig, validPos, TargetingMode.FIRST.strategy());
        assertThrows(InvalidPlacementException.class, () -> state.addTower(duplicateCellTower));

        // 5. Duplicate ID placement on different cell
        Position otherCell = new Position(2, 2);
        CannonTower duplicateIdTower = new CannonTower(10, cannonConfig, otherCell, TargetingMode.FIRST.strategy());
        assertThrows(InvalidPlacementException.class, () -> state.addTower(duplicateIdTower));
    }

    @Test
    @DisplayName("removeTower removes the tower and frees the cell for new placement")
    void testRemoveTowerFreesCell() {
        Position pos = new Position(3, 3);
        CannonTower tower = new CannonTower(1, cannonConfig, pos, TargetingMode.FIRST.strategy());
        state.addTower(tower);
        assertEquals(1, state.towers().size());

        // Removing non-existent returns false
        assertFalse(state.removeTower(99));

        // Remove tower
        assertTrue(state.removeTower(1));
        assertEquals(0, state.towers().size());
        assertTrue(state.towerAt(pos).isEmpty());

        // The same cell can now receive a new tower
        CannonTower newTower = new CannonTower(2, cannonConfig, pos, TargetingMode.CLOSEST.strategy());
        state.addTower(newTower);
        assertEquals(1, state.towers().size());
        assertEquals(2, state.towerAt(pos).get().id());
    }

    @Test
    @DisplayName("removeEnemy removes the enemy by ID")
    void testRemoveEnemy() {
        BasicEnemy e1 = new BasicEnemy(10);
        BasicEnemy e2 = new BasicEnemy(20);
        state.addEnemy(e1);
        state.addEnemy(e2);
        assertEquals(2, state.enemies().size());

        assertTrue(state.removeEnemy(10));
        assertFalse(state.removeEnemy(10)); // already removed
        assertEquals(1, state.enemies().size());
        assertEquals(20, state.enemies().get(0).id());
    }

    @Test
    @DisplayName("Terminal statuses (WON, LOST) cannot be changed to another status")
    void testTerminalStatusTransitions() {
        // READY -> PLAYING -> WAVE_COMPLETED -> PLAYING -> WON
        state.changeStatus(GameStatus.PLAYING);
        assertEquals(GameStatus.PLAYING, state.status());

        state.changeStatus(GameStatus.WAVE_COMPLETED);
        assertEquals(GameStatus.WAVE_COMPLETED, state.status());

        state.changeStatus(GameStatus.WON);
        assertEquals(GameStatus.WON, state.status());

        // Changing away from WON is rejected with InvalidActionException
        assertThrows(InvalidActionException.class, () -> state.changeStatus(GameStatus.PLAYING));
        assertThrows(InvalidActionException.class, () -> state.changeStatus(GameStatus.READY));
        assertThrows(InvalidActionException.class, () -> state.changeStatus(GameStatus.LOST));

        // Reasserting WON is valid (idempotent)
        state.changeStatus(GameStatus.WON);

        // LOST terminal state
        GameState lostState = new GameState(map, levelConfig);
        lostState.changeStatus(GameStatus.LOST);
        assertThrows(InvalidActionException.class, () -> lostState.changeStatus(GameStatus.PLAYING));
        assertThrows(InvalidActionException.class, () -> lostState.changeStatus(GameStatus.WON));
    }

    @Test
    @DisplayName("advanceTick and startNextWave increment their respective counters")
    void testTickAndWaveCounters() {
        assertEquals(0, state.tick());
        state.advanceTick();
        assertEquals(1, state.tick());
        state.advanceTick();
        assertEquals(2, state.tick());

        assertEquals(0, state.currentWaveNumber());
        state.startNextWave();
        assertEquals(1, state.currentWaveNumber());
        state.startNextWave();
        assertEquals(2, state.currentWaveNumber());
    }

    @Test
    @DisplayName("IDs from two separate GameState instances are completely independent")
    void testPerInstanceIdGenerators() {
        GameState state1 = new GameState(map, levelConfig);
        GameState state2 = new GameState(map, levelConfig);

        assertEquals(0, state1.nextEnemyId());
        assertEquals(1, state1.nextEnemyId());
        assertEquals(2, state1.nextEnemyId());

        assertEquals(0, state2.nextEnemyId(), "state2 enemy IDs must start from 0 independently");
        assertEquals(1, state2.nextEnemyId());

        assertEquals(0, state1.nextTowerId());
        assertEquals(1, state1.nextTowerId());

        assertEquals(0, state2.nextTowerId(), "state2 tower IDs must start from 0 independently");
    }

    @Test
    @DisplayName("Null arguments are rejected across all public methods")
    void testNullValidation() {
        assertThrows(IllegalArgumentException.class, () -> new GameState(null, levelConfig));
        assertThrows(IllegalArgumentException.class, () -> new GameState(map, null));
        assertThrows(IllegalArgumentException.class, () -> state.addEnemy(null));
        assertThrows(IllegalArgumentException.class, () -> state.addTower(null));
        assertThrows(IllegalArgumentException.class, () -> state.changeStatus(null));
        assertThrows(IllegalArgumentException.class, () -> state.towerAt(null));
    }
}
