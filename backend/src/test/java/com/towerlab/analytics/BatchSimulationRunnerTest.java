package com.towerlab.analytics;

import com.towerlab.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchSimulationRunnerTest {

    private BatchSimulationRunner runner;
    private LevelConfig sampleLevel;

    @BeforeEach
    void setUp() {
        runner = new BatchSimulationRunner();

        List<PathWaypoint> waypoints = List.of(
                new PathWaypoint(0, 3),
                new PathWaypoint(5, 3),
                new PathWaypoint(5, 6),
                new PathWaypoint(11, 6)
        );

        List<WaveDefinition> waves = List.of(
                new WaveDefinition(1, 10, List.of(
                        new WaveDefinition.CreepSpawnSchedule(CreepType.STANDARD, 4, 10)
                ))
        );

        sampleLevel = new LevelConfig("test-level", "Test Map", "A testing ground", 12, 8, 100, 350, waypoints, waves);
    }

    @Test
    @DisplayName("Batch simulation aggregates win rate, averages, and tower metrics accurately")
    void testBatchSimulationAggregation() {
        List<PlayerAction> actions = List.of(
                new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.CANNON, 2, 2, TargetingMode.FIRST),
                new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.FROST, 3, 2, TargetingMode.CLOSEST)
        );

        BatchSimulationResult result = runner.runBatch(sampleLevel, actions, 20, 1000L);

        assertEquals(20, result.getTotalRuns());
        assertEquals(20, result.getWins() + result.getLosses());
        assertTrue(result.getWinRate() >= 0.0 && result.getWinRate() <= 100.0);
        assertTrue(result.getAvgTicks() > 0);
        assertNotNull(result.getTowerMetrics());
        assertTrue(result.getTowerMetrics().containsKey("CANNON") || result.getTowerMetrics().containsKey("FROST"));

        if (result.getTowerMetrics().containsKey("CANNON")) {
            TowerMetrics cannonStats = result.getTowerMetrics().get("CANNON");
            assertEquals("CANNON", cannonStats.getTowerType());
            assertTrue(cannonStats.getTotalDamageDealt() > 0);
            assertTrue(cannonStats.getCostEfficiency() >= 0);
        }
    }
}

