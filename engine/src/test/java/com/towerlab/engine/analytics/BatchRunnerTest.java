package com.towerlab.engine.analytics;

import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.config.SpawnGroupConfig;
import com.towerlab.engine.config.WaveConfig;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.exception.TowerLabException;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.simulation.BuildTowerAction;
import com.towerlab.engine.simulation.PlayerAction;
import com.towerlab.engine.simulation.Simulation;
import com.towerlab.engine.simulation.SimulationResult;
import com.towerlab.engine.strategy.TargetingMode;
import com.towerlab.engine.test.TestConfigHelper;
import com.towerlab.engine.test.TestLevelHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchRunnerTest {

    private final GameConfig frozenConfig = TestConfigHelper.getFrozenGameConfig();

    private final List<PlayerAction> sampleActions = List.of(
            new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
            new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST),
            new BuildTowerAction(1, "Sniper", 7, 4, TargetingMode.STRONGEST)
    );

    @Test
    @DisplayName("threads=1 equals threads=8 for identical baseSeed and run count")
    void testSingleThreadEqualsMultiThread() {
        SimulationConfig template = new SimulationConfig(frozenConfig, 100L, sampleActions);

        BatchResult singleThreadResult = BatchRunner.run(template, 6, 100L, 1);
        BatchResult multiThreadResult = BatchRunner.run(template, 6, 100L, 8);

        assertEquals(singleThreadResult.runs(), multiThreadResult.runs());
        assertEquals(singleThreadResult.winRate(), multiThreadResult.winRate(), 1e-6);
        assertEquals(singleThreadResult.averages().damageDealt(), multiThreadResult.averages().damageDealt(), 1e-6);
        assertEquals(singleThreadResult.averages().durationTicks(), multiThreadResult.averages().durationTicks(), 1e-6);
        assertEquals(singleThreadResult.averages().enemiesDefeated(), multiThreadResult.averages().enemiesDefeated(), 1e-6);
        assertEquals(singleThreadResult.averages().leakage(), multiThreadResult.averages().leakage(), 1e-6);
        assertEquals(singleThreadResult.averages().baseHealthRemaining(), multiThreadResult.averages().baseHealthRemaining(), 1e-6);
        assertEquals(singleThreadResult.towerStats(), multiThreadResult.towerStats());
    }

    @Test
    @DisplayName("The same baseSeed gives equal results twice (determinism)")
    void testSameBaseSeedGivesIdenticalResultsTwice() {
        SimulationConfig template = new SimulationConfig(frozenConfig, 500L, sampleActions);

        BatchResult run1 = BatchRunner.run(template, 5, 500L, 4);
        BatchResult run2 = BatchRunner.run(template, 5, 500L, 4);

        assertEquals(run1, run2);
    }

    @Test
    @DisplayName("runs=1 equals aggregating one single Simulation run by hand")
    void testRunsOneEqualsSingleSimulationHandAggregated() {
        long seed = 777L;
        SimulationConfig template = new SimulationConfig(frozenConfig, seed, sampleActions);

        // Run single simulation manually
        Simulation simulation = new Simulation();
        SimulationResult singleResult = simulation.run(new SimulationConfig(frozenConfig, seed, sampleActions));
        RunMetrics singleMetrics = MetricsCalculator.calculate(singleResult, frozenConfig);
        BatchResult handAggregated = MetricsCalculator.aggregate(List.of(singleMetrics));

        // Run via BatchRunner with runs=1
        BatchResult batchResult = BatchRunner.run(template, 1, seed, 1);

        assertEquals(handAggregated, batchResult);
    }

    @Test
    @DisplayName("No towers gives winRate 0 and empty towerStats")
    void testNoTowersGivesWinRateZero() {
        SimulationConfig noTowersTemplate = new SimulationConfig(frozenConfig, 42L, List.of());
        BatchResult result = BatchRunner.run(noTowersTemplate, 3, 42L, 2);

        assertEquals(0.0, result.winRate(), 1e-6);
        assertTrue(result.towerStats().isEmpty(), "No built towers must produce empty towerStats");
    }

    @Test
    @DisplayName("A trivially winning custom level gives winRate 1.0")
    void testTriviallyWinningLevelGivesWinRateOne() {
        List<Position> waypoints = List.of(new Position(0, 1), new Position(10, 1));
        // Single wave with 1 basic enemy and cannon placed to kill it
        WaveConfig wave = new WaveConfig(List.of(new SpawnGroupConfig("Basic", 1, 10, 0)));
        LevelConfig winningLevel = TestLevelHelper.createLevel(
                "TrivialWin", 12, 4, waypoints, List.of(),
                List.of("Cannon"), List.of(wave),
                150, 20, 0, 20, 500, 0
        );
        GameConfig customGameConfig = new GameConfig(frozenConfig.towers(), frozenConfig.enemies(), winningLevel);
        List<PlayerAction> actions = List.of(new BuildTowerAction(0, "Cannon", 2, 2, TargetingMode.FIRST));
        SimulationConfig winningTemplate = new SimulationConfig(customGameConfig, 10L, actions);

        BatchResult result = BatchRunner.run(winningTemplate, 4, 10L, 2);

        assertEquals(1.0, result.winRate(), 1e-6);
        assertEquals(4, result.runs());
        assertEquals(1, result.towerStats().size());
        assertEquals("Cannon", result.towerStats().get(0).towerType());
    }

    @Test
    @DisplayName("Invalid arguments are rejected with InvalidConfigException")
    void testInvalidArgumentsRejected() {
        SimulationConfig template = new SimulationConfig(frozenConfig, 42L, List.of());

        assertThrows(InvalidConfigException.class, () -> BatchRunner.run(null, 5, 42L, 2));
        assertThrows(InvalidConfigException.class, () -> BatchRunner.run(template, 0, 42L, 2));
        assertThrows(InvalidConfigException.class, () -> BatchRunner.run(template, -3, 42L, 2));
        assertThrows(InvalidConfigException.class, () -> BatchRunner.run(template, 5, 42L, 0));
        assertThrows(InvalidConfigException.class, () -> BatchRunner.run(template, 5, 42L, -1));
    }

    @Test
    @DisplayName("A failing worker rethrows TowerLabException reporting its failing seed")
    void testFailingWorkerReportsSeed() {
        // Action with wave index out of bounds triggers InvalidActionException inside simulation
        List<PlayerAction> badActions = List.of(
                new BuildTowerAction(999, "Cannon", 1, 1, TargetingMode.FIRST)
        );
        SimulationConfig badTemplate = new SimulationConfig(frozenConfig, 555L, badActions);

        TowerLabException ex = assertThrows(TowerLabException.class, () ->
                BatchRunner.run(badTemplate, 3, 555L, 2));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("555"),
                "Exception message must include the failing seed, got: " + ex.getMessage());
    }
}
