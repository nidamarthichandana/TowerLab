package com.towerlab.engine;

import com.towerlab.engine.events.EventType;
import com.towerlab.engine.events.SimulationEvent;
import com.towerlab.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSimulationTest {

    private LevelConfig sampleLevel;

    @BeforeEach
    void setUp() {
        List<PathWaypoint> waypoints = List.of(
                new PathWaypoint(0, 3),
                new PathWaypoint(5, 3),
                new PathWaypoint(5, 6),
                new PathWaypoint(11, 6)
        );

        List<WaveDefinition> waves = List.of(
                new WaveDefinition(1, 10, List.of(
                        new WaveDefinition.CreepSpawnSchedule(CreepType.STANDARD, 3, 10)
                ))
        );

        sampleLevel = new LevelConfig("test-level", "Test Map", "A testing ground", 12, 8, 100, 350, waypoints, waves);
    }

    @Test
    @DisplayName("Identical seed and plan must produce identical deterministic results")
    void testDeterminism() {
        List<PlayerAction> actions = List.of(
                new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.CANNON, 2, 2, TargetingMode.FIRST),
                new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.FROST, 4, 2, TargetingMode.CLOSEST)
        );

        long seed = 424242L;

        GameSimulation sim1 = new GameSimulation(sampleLevel, seed, actions);
        GameSimulation.SimulationResult res1 = sim1.run();

        GameSimulation sim2 = new GameSimulation(sampleLevel, seed, actions);
        GameSimulation.SimulationResult res2 = sim2.run();

        assertEquals(res1.getOutcome(), res2.getOutcome());
        assertEquals(res1.getBaseHealth(), res2.getBaseHealth());
        assertEquals(res1.getGold(), res2.getGold());
        assertEquals(res1.getTotalTicks(), res2.getTotalTicks());
        assertEquals(res1.getEvents().size(), res2.getEvents().size());

        for (int i = 0; i < res1.getEvents().size(); i++) {
            SimulationEvent e1 = res1.getEvents().get(i);
            SimulationEvent e2 = res2.getEvents().get(i);
            assertEquals(e1.getTick(), e2.getTick());
            assertEquals(e1.getType(), e2.getType());
        }
    }

    @Test
    @DisplayName("Building on path tile must be rejected")
    void testCannotBuildOnPathTile() {
        // (2, 3) is on the path segment between (0, 3) and (5, 3)
        List<PlayerAction> actions = List.of(
                new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.CANNON, 2, 3, TargetingMode.FIRST)
        );

        GameSimulation sim = new GameSimulation(sampleLevel, 123L, actions);
        GameSimulation.SimulationResult res = sim.run();

        boolean rejectedFound = res.getEvents().stream()
                .anyMatch(e -> e.getType() == EventType.ACTION_REJECTED && e.getMessage().contains("path"));

        assertTrue(rejectedFound, "Should emit ACTION_REJECTED when placing tower on path");
    }

    @Test
    @DisplayName("Building without sufficient gold must be rejected")
    void testInsufficientGoldRejection() {
        sampleLevel.setInitialGold(50); // Cannon costs 100

        List<PlayerAction> actions = List.of(
                new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.CANNON, 1, 1, TargetingMode.FIRST)
        );

        GameSimulation sim = new GameSimulation(sampleLevel, 123L, actions);
        GameSimulation.SimulationResult res = sim.run();

        boolean rejectedFound = res.getEvents().stream()
                .anyMatch(e -> e.getType() == EventType.ACTION_REJECTED && e.getMessage().contains("Insufficient gold"));

        assertTrue(rejectedFound, "Should emit ACTION_REJECTED when gold is insufficient");
    }

    @Test
    @DisplayName("Defeat occurs when base health reaches 0")
    void testDefeatCondition() {
        sampleLevel.setInitialHealth(5); // Creep leakage deals at least 10 damage

        // No towers placed
        List<PlayerAction> actions = List.of();

        GameSimulation sim = new GameSimulation(sampleLevel, 999L, actions);
        GameSimulation.SimulationResult res = sim.run();

        assertEquals(SimulationOutcome.DEFEAT, res.getOutcome());
        assertEquals(0, res.getBaseHealth());
    }

    @Test
    @DisplayName("Frost tower applies slow effect to creeps")
    void testFrostSlowApplication() {
        List<PlayerAction> actions = List.of(
                new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.FROST, 1, 2, TargetingMode.FIRST)
        );

        GameSimulation sim = new GameSimulation(sampleLevel, 888L, actions);
        GameSimulation.SimulationResult res = sim.run();

        boolean slowEventFound = res.getEvents().stream()
                .anyMatch(e -> e.getType() == EventType.CREEP_SLOWED);

        assertTrue(slowEventFound, "Should emit CREEP_SLOWED when Frost tower fires at creeps");
    }
}

