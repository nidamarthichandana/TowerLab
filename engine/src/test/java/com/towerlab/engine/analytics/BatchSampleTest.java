package com.towerlab.engine.analytics;

import com.towerlab.engine.config.ConfigLoader;
import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.simulation.BuildTowerAction;
import com.towerlab.engine.simulation.PlayerAction;
import com.towerlab.engine.strategy.TargetingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchSampleTest {

    @Test
    @DisplayName("BatchSampleTest: run Level 1 sample loadout for 200 runs, print metrics and elapsed time")
    void testBatchSampleLevel1Loadout() {
        ConfigLoader loader = new ConfigLoader();
        // Uses live main configs as specified in Phase 5A requirements
        GameConfig liveConfig = loader.loadFromClasspath("");

        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST),
                new BuildTowerAction(1, "Sniper", 7, 4, TargetingMode.STRONGEST)
        );

        int runs = 200;
        long baseSeed = 42L;
        int threads = Math.min(8, Math.max(2, Runtime.getRuntime().availableProcessors()));
        SimulationConfig template = new SimulationConfig(liveConfig, baseSeed, actions);

        long start = System.currentTimeMillis();
        BatchResult batchResult = BatchRunner.run(template, runs, baseSeed, threads);
        long elapsedMs = System.currentTimeMillis() - start;

        // Print batch results
        System.out.println("==================================================");
        System.out.printf("Batch Sample Run (Level 1, %d runs, %d threads):%n", runs, threads);
        System.out.printf("  Elapsed Time:            %d ms (%.2f ms/run)%n", elapsedMs, (double) elapsedMs / runs);
        System.out.printf("  Win Rate:                %.2f%%%n", batchResult.winRate() * 100);
        System.out.println("  Averages:");
        System.out.printf("    Damage Dealt:          %.1f%n", batchResult.averages().damageDealt());
        System.out.printf("    Enemies Defeated:      %.1f%n", batchResult.averages().enemiesDefeated());
        System.out.printf("    Enemy Leakage:         %.1f%n", batchResult.averages().leakage());
        System.out.printf("    Base Health Remaining: %.1f%n", batchResult.averages().baseHealthRemaining());
        System.out.printf("    Duration (ticks):      %.1f%n", batchResult.averages().durationTicks());
        System.out.println("  Tower Stats (pooled):");
        for (TowerStat stat : batchResult.towerStats()) {
            System.out.printf("    %-8s -> Contribution: %5.1f%% | Cost Efficiency: %5.2f | Utilization: %5.1f%%%n",
                    stat.towerType(),
                    stat.damageContribution() * 100,
                    stat.costEfficiency(),
                    stat.utilization() * 100);
        }
        System.out.println("==================================================");

        // Sanity assertions (no assertions on specific balance numbers per requirements)
        assertEquals(runs, batchResult.runs());
        assertTrue(batchResult.winRate() >= 0.0 && batchResult.winRate() <= 1.0);
        assertNotNull(batchResult.averages());
        assertNotNull(batchResult.towerStats());
        assertFalse(batchResult.towerStats().isEmpty(), "Sample loadout builds towers, towerStats must not be empty");
    }
}
