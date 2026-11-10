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
import com.towerlab.engine.test.TestConfigHelper;
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

        BasicEnemy enemy = TestConfigHelper.createBasicEnemy(1);
        state.addEnemy(enemy);

        List<Tower> towersList = state.towers();
        List<Enemy> enemiesList = state.enemies();

        assertThrows(UnsupportedOperationException.class, () -> towersList.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> enemiesList.remove(0));

        // Adding another entity to state creates fresh snapshots
        BasicEnemy enemy2 = TestConfigHelper.createBasicEnemy(2);
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
        BasicEnemy e1 = TestConfigHelper.createBasicEnemy(10);
        BasicEnemy e2 = TestConfigHelper.createBasicEnemy(20);
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

    @Test
    @DisplayName("1. spendGold rejects negative amounts")
    void testSpendGoldRejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> state.spendGold(-1));
        assertThrows(IllegalArgumentException.class, () -> state.spendGold(-100));
        assertEquals(150, state.gold(), "Gold must remain unchanged after rejected spendGold");
    }

    @Test
    @DisplayName("2. spendGold rejects zero amounts")
    void testSpendGoldRejectsZeroAmounts() {
        assertThrows(IllegalArgumentException.class, () -> state.spendGold(0));
        assertEquals(150, state.gold(), "Gold must remain unchanged after rejected spendGold");
    }

    @Test
    @DisplayName("3. addGold rejects negative amounts")
    void testAddGoldRejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> state.addGold(-1));
        assertThrows(IllegalArgumentException.class, () -> state.addGold(-50));
        assertEquals(150, state.gold(), "Gold must remain unchanged after rejected addGold");
    }

    @Test
    @DisplayName("4. addGold rejects zero amounts")
    void testAddGoldRejectsZeroAmounts() {
        assertThrows(IllegalArgumentException.class, () -> state.addGold(0));
        assertEquals(150, state.gold(), "Gold must remain unchanged after rejected addGold");
    }

    @Test
    @DisplayName("5. damageBase rejects negative amounts")
    void testDamageBaseRejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> state.damageBase(-1));
        assertThrows(IllegalArgumentException.class, () -> state.damageBase(-10));
        assertEquals(20, state.baseHealth(), "Base health must remain unchanged after rejected damageBase");
    }

    @Test
    @DisplayName("6. damageBase rejects zero amounts")
    void testDamageBaseRejectsZeroAmounts() {
        assertThrows(IllegalArgumentException.class, () -> state.damageBase(0));
        assertEquals(20, state.baseHealth(), "Base health must remain unchanged after rejected damageBase");
    }

    @Test
    @DisplayName("7. Duplicate tower IDs are rejected without corrupting existing state")
    void testDuplicateTowerIdRejectedWithoutCorruptingExistingState() {
        Position pos1 = new Position(0, 0);
        Position pos2 = new Position(2, 2);

        CannonTower tower1 = new CannonTower(10, cannonConfig, pos1, TargetingMode.FIRST.strategy());
        state.addTower(tower1);
        assertEquals(1, state.towers().size());
        assertEquals(tower1, state.tower(10).orElse(null));

        // Attempt to place another tower with duplicate ID 10 at pos2
        CannonTower duplicateIdTower = new CannonTower(10, cannonConfig, pos2, TargetingMode.FIRST.strategy());
        assertThrows(InvalidPlacementException.class, () -> state.addTower(duplicateIdTower));

        // Verify existing state remains uncorrupted
        assertEquals(1, state.towers().size(), "Tower count must remain unchanged");
        assertEquals(tower1, state.tower(10).orElse(null), "Original tower must remain untouched");
        assertTrue(state.towerAt(pos1).isPresent());
        assertTrue(state.towerAt(pos2).isEmpty(), "Target cell of rejected tower must remain unoccupied");
    }

    @Test
    @DisplayName("8. advanceTick behaves correctly, including expected tick increment and boundary conditions")
    void testAdvanceTickBehavesCorrectlyIncludingBoundaryConditions() {
        assertEquals(0, state.tick(), "Initial tick must be 0");

        state.advanceTick();
        assertEquals(1, state.tick());

        state.advanceTick();
        assertEquals(2, state.tick());

        // Increment across a large sequence of ticks
        for (int i = 3; i <= 500; i++) {
            state.advanceTick();
            assertEquals(i, state.tick(), "Tick must strictly increment by 1 on each advanceTick call");
        }

        // Verify advanceTick does not mutate unrelated state
        assertEquals(150, state.gold());
        assertEquals(20, state.baseHealth());
        assertEquals(GameStatus.READY, state.status());
        assertEquals(0, state.currentWaveNumber());
        assertTrue(state.enemies().isEmpty());
        assertTrue(state.towers().isEmpty());
    }
}
