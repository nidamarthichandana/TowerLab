package com.towerlab.engine.simulation;

import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.event.EventBus;
import com.towerlab.engine.event.EventLog;
import com.towerlab.engine.event.GameEvent;
import com.towerlab.engine.event.TowerBuilt;
import com.towerlab.engine.exception.InsufficientFundsException;
import com.towerlab.engine.exception.InvalidActionException;
import com.towerlab.engine.exception.InvalidPlacementException;
import com.towerlab.engine.factory.TowerFactory;
import com.towerlab.engine.model.GameMap;
import com.towerlab.engine.model.GameState;
import com.towerlab.engine.model.GameStatus;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.model.Tower;
import com.towerlab.engine.strategy.TargetingMode;
import com.towerlab.engine.test.TestConfigHelper;
import com.towerlab.engine.test.TestLevelHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildTowerActionTest {

    private GameMap gameMap;
    private GameState gameState;
    private TowerFactory towerFactory;
    private EventBus eventBus;
    private EventLog eventLog;

    @BeforeEach
    void setUp() {
        // Map 10x10 with path: (0, 2) -> (5, 2) -> (5, 8)
        List<Position> waypoints = List.of(
                new Position(0, 2),
                new Position(5, 2),
                new Position(5, 8)
        );
        LevelConfig levelConfig = TestLevelHelper.createStandardLevel(
                "TestLevel", 10, 10, waypoints, List.of("Cannon", "Sniper", "Frost"), List.of()
        );
        gameMap = new GameMap(levelConfig);
        gameState = new GameState(gameMap, levelConfig);

        towerFactory = new TowerFactory(TestConfigHelper.getFrozenTowers());

        eventBus = new EventBus();
        eventLog = new EventLog();
        eventBus.register(eventLog);
    }

    @Test
    @DisplayName("Build tower successfully during READY status")
    void testBuildTowerSuccessReadyStatus() {
        assertEquals(150, gameState.gold());
        assertEquals(0, gameState.towers().size());

        BuildTowerAction action = new BuildTowerAction(1, "Cannon", 1, 1, TargetingMode.FIRST);
        action.execute(gameState, towerFactory, eventBus, 10);

        // Gold deducted: 150 - 60 = 90
        assertEquals(90, gameState.gold());

        // Tower added with id 0
        assertEquals(1, gameState.towers().size());
        Tower tower = gameState.towers().get(0);
        assertEquals(0, tower.id());
        assertEquals("Cannon", tower.config().type());
        assertEquals(new Position(1, 1), tower.position());

        // TowerBuilt event emitted with correct details and goldAfter
        List<GameEvent> events = eventLog.events();
        assertEquals(1, events.size());
        assertInstanceOf(TowerBuilt.class, events.get(0));
        TowerBuilt builtEvent = (TowerBuilt) events.get(0);
        assertEquals(10, builtEvent.tick());
        assertEquals(0, builtEvent.towerId());
        assertEquals("Cannon", builtEvent.towerType());
        assertEquals(1, builtEvent.x());
        assertEquals(1, builtEvent.y());
        assertEquals(90, builtEvent.goldAfter());
    }

    @Test
    @DisplayName("Build tower successfully during WAVE_COMPLETED status")
    void testBuildTowerSuccessWaveCompletedStatus() {
        gameState.changeStatus(GameStatus.PLAYING);
        gameState.changeStatus(GameStatus.WAVE_COMPLETED);

        BuildTowerAction action = new BuildTowerAction(2, "Sniper", 3, 5, TargetingMode.STRONGEST);
        action.execute(gameState, towerFactory, eventBus, 150);

        // 150 - 90 = 60
        assertEquals(60, gameState.gold());
        assertEquals(1, gameState.towers().size());
        assertEquals("Sniper", gameState.towers().get(0).config().type());

        assertEquals(1, eventLog.events().size());
        TowerBuilt builtEvent = (TowerBuilt) eventLog.events().get(0);
        assertEquals(150, builtEvent.tick());
        assertEquals(60, builtEvent.goldAfter());
    }

    @Test
    @DisplayName("Build tower fails and leaves state unchanged when status is PLAYING")
    void testBuildTowerFailsInPlayingStatus() {
        gameState.changeStatus(GameStatus.PLAYING);
        int initialGold = gameState.gold();

        BuildTowerAction action = new BuildTowerAction(1, "Cannon", 1, 1, TargetingMode.FIRST);
        assertThrows(InvalidActionException.class, () ->
                action.execute(gameState, towerFactory, eventBus, 5));

        // State must remain completely unchanged
        assertEquals(initialGold, gameState.gold());
        assertTrue(gameState.towers().isEmpty());
        assertTrue(eventLog.events().isEmpty());
        assertEquals(0, gameState.nextTowerId()); // Id counter was not incremented
    }

    @Test
    @DisplayName("Build tower fails and leaves state unchanged when gold is insufficient")
    void testBuildTowerFailsInsufficientFunds() {
        // Reduce gold to 50 (Cannon costs 60)
        gameState.spendGold(100);
        assertEquals(50, gameState.gold());

        BuildTowerAction action = new BuildTowerAction(1, "Cannon", 1, 1, TargetingMode.FIRST);
        assertThrows(InsufficientFundsException.class, () ->
                action.execute(gameState, towerFactory, eventBus, 5));

        // State remains unchanged
        assertEquals(50, gameState.gold());
        assertTrue(gameState.towers().isEmpty());
        assertTrue(eventLog.events().isEmpty());
    }

    @Test
    @DisplayName("Build tower fails and leaves state unchanged when cell is on the path")
    void testBuildTowerFailsOnPathCell() {
        int initialGold = gameState.gold();
        // (2, 2) is on the path between (0, 2) and (5, 2)
        BuildTowerAction action = new BuildTowerAction(1, "Cannon", 2, 2, TargetingMode.FIRST);
        assertThrows(InvalidPlacementException.class, () ->
                action.execute(gameState, towerFactory, eventBus, 5));

        assertEquals(initialGold, gameState.gold());
        assertTrue(gameState.towers().isEmpty());
        assertTrue(eventLog.events().isEmpty());
    }

    @Test
    @DisplayName("Build tower fails and leaves state unchanged when cell is already occupied")
    void testBuildTowerFailsOccupiedCell() {
        // Place first tower successfully at (1, 1)
        BuildTowerAction action1 = new BuildTowerAction(1, "Cannon", 1, 1, TargetingMode.FIRST);
        action1.execute(gameState, towerFactory, eventBus, 1);
        assertEquals(90, gameState.gold());
        assertEquals(1, gameState.towers().size());
        assertEquals(1, eventLog.events().size());

        // Try to place another tower at the exact same cell (1, 1)
        BuildTowerAction action2 = new BuildTowerAction(1, "Frost", 1, 1, TargetingMode.CLOSEST);
        assertThrows(InvalidPlacementException.class, () ->
                action2.execute(gameState, towerFactory, eventBus, 2));

        // State remains unchanged from after action1
        assertEquals(90, gameState.gold());
        assertEquals(1, gameState.towers().size());
        assertEquals(1, eventLog.events().size());
    }

    @Test
    @DisplayName("Build tower fails and leaves state unchanged when cell is out of bounds")
    void testBuildTowerFailsOutOfBounds() {
        int initialGold = gameState.gold();
        BuildTowerAction action = new BuildTowerAction(1, "Cannon", -1, 5, TargetingMode.FIRST);
        assertThrows(InvalidPlacementException.class, () ->
                action.execute(gameState, towerFactory, eventBus, 5));

        assertEquals(initialGold, gameState.gold());
        assertTrue(gameState.towers().isEmpty());
        assertTrue(eventLog.events().isEmpty());
    }

    @Test
    @DisplayName("TargetingMode and towerType are required in BuildTowerAction")
    void testRequiredFieldsInAction() {
        assertThrows(NullPointerException.class, () ->
                new BuildTowerAction(1, null, 1, 1, TargetingMode.FIRST));
        assertThrows(NullPointerException.class, () ->
                new BuildTowerAction(1, "Cannon", 1, 1, null));
    }

    @Test
    @DisplayName("Execute rejects null arguments")
    void testExecuteRejectsNullArguments() {
        BuildTowerAction action = new BuildTowerAction(1, "Cannon", 1, 1, TargetingMode.FIRST);
        assertThrows(NullPointerException.class, () ->
                action.execute(null, towerFactory, eventBus, 0));
        assertThrows(NullPointerException.class, () ->
                action.execute(gameState, null, eventBus, 0));
        assertThrows(NullPointerException.class, () ->
                action.execute(gameState, towerFactory, null, 0));
    }
}
