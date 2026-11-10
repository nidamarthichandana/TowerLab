package com.towerlab.engine.analytics;

import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.exception.TowerLabException;
import com.towerlab.engine.simulation.Simulation;
import com.towerlab.engine.simulation.SimulationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes batches of simulations on a fixed thread pool and combines results in seed order.
 * Ensures zero static mutable state and avoids holding large event lists in memory.
 */
public class BatchRunner {

    public BatchRunner() {}

    /**
     * Runs a batch of simulations with seeds baseSeed..baseSeed+runs-1 on a thread pool.
     *
     * @param template simulation configuration template providing game config and player actions
     * @param runs     number of runs to execute (must be positive)
     * @param baseSeed base RNG seed
     * @param threads  number of worker threads (must be positive)
     * @return aggregated BatchResult
     * @throws InvalidConfigException if template is null or runs/threads are non-positive
     * @throws TowerLabException      if any worker simulation fails (identifies failing seed)
     */
    public static BatchResult run(SimulationConfig template, int runs, long baseSeed, int threads) {
        if (template == null) {
            throw new InvalidConfigException("SimulationConfig template must not be null");
        }
        if (runs <= 0) {
            throw new InvalidConfigException("runs must be positive: " + runs);
        }
        if (threads <= 0) {
            throw new InvalidConfigException("threads must be positive: " + threads);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<RunMetrics>> futures = new ArrayList<>(runs);

            for (int i = 0; i < runs; i++) {
                final long seed = baseSeed + i;
                final SimulationConfig runConfig = new SimulationConfig(template.gameConfig(), seed, template.actions());
                futures.add(executor.submit(() -> {
                    try {
                        Simulation simulation = new Simulation();
                        SimulationResult result = simulation.run(runConfig);
                        // Compute RunMetrics immediately and discard event lists
                        return MetricsCalculator.calculate(result, template.gameConfig());
                    } catch (Throwable t) {
                        throw new TowerLabException("Simulation failed for seed " + seed + ": " + t.getMessage(), t);
                    }
                }));
            }

            // Collect results strictly in seed order
            List<RunMetrics> metricsList = new ArrayList<>(runs);
            for (int i = 0; i < runs; i++) {
                long seed = baseSeed + i;
                try {
                    metricsList.add(futures.get(i).get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TowerLabException("Batch execution interrupted at seed " + seed, e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof TowerLabException tle && tle.getMessage().contains(String.valueOf(seed))) {
                        throw tle;
                    }
                    throw new TowerLabException("Simulation failed for seed " + seed + ": " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
                }
            }

            return MetricsCalculator.aggregate(metricsList);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Convenience method delegating to MetricsCalculator.aggregate.
     */
    public static BatchResult aggregate(List<RunMetrics> runs) {
        return MetricsCalculator.aggregate(runs);
    }
}
