package com.towerlab.engine.simulation;

import com.towerlab.engine.config.ConfigLoader;
import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.strategy.TargetingMode;
import com.towerlab.engine.test.TestConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationParallelSafetyTest {

    @Test
    @DisplayName("Parallel safety: 50 concurrent simulation runs on an 8-thread pool match sequential baseline")
    void testParallelSafetyAcrossThreads() throws Exception {
        GameConfig gameConfig = TestConfigHelper.getFrozenGameConfig();

        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST),
                new BuildTowerAction(1, "Sniper", 7, 4, TargetingMode.STRONGEST)
        );

        SimulationConfig config = new SimulationConfig(gameConfig, 42L, actions);

        // 1. Generate sequential baseline result
        Simulation baselineSimulation = new Simulation();
        SimulationResult baselineResult = baselineSimulation.run(config);

        // 2. Prepare 8-thread pool to execute 50 parallel runs
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<SimulationResult>> tasks = new ArrayList<>(50);
            for (int i = 0; i < 50; i++) {
                tasks.add(() -> {
                    // Each invocation creates and uses isolated mutable simulation state
                    Simulation sim = new Simulation();
                    return sim.run(config);
                });
            }

            List<Future<SimulationResult>> futures = executor.invokeAll(tasks);

            for (Future<SimulationResult> future : futures) {
                SimulationResult parallelResult = future.get();

                assertEquals(baselineResult.outcome(), parallelResult.outcome());
                assertEquals(baselineResult.baseHealth(), parallelResult.baseHealth());
                assertEquals(baselineResult.gold(), parallelResult.gold());
                assertEquals(baselineResult.ticks(), parallelResult.ticks());
                assertEquals(baselineResult.events(), parallelResult.events(),
                        "Ordered event sequence in parallel execution must match sequential baseline exactly");
            }
        } finally {
            // Shut down executor reliably, even if an assertion or future.get() fails
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
}
