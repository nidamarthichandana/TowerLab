package com.towerlab.analytics;

import com.towerlab.engine.GameSimulation;
import com.towerlab.engine.model.LevelConfig;
import com.towerlab.engine.model.PlayerAction;
import com.towerlab.engine.model.SimulationOutcome;
import com.towerlab.engine.model.Tower;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class BatchSimulationRunner {

    public BatchSimulationResult runBatch(LevelConfig levelConfig, List<PlayerAction> actions, int runs, long baseSeed) {
        int effectiveRuns = Math.max(1, Math.min(runs, 1000)); // cap between 1 and 1000 for safety

        List<GameSimulation.SimulationResult> results = IntStream.range(0, effectiveRuns)
                .parallel()
                .mapToObj(i -> {
                    long runSeed = baseSeed + i * 31L;
                    GameSimulation sim = new GameSimulation(levelConfig, runSeed, actions);
                    return sim.run();
                })
                .collect(Collectors.toList());

        int wins = 0;
        int losses = 0;
        long totalBaseHealth = 0;
        long totalTicks = 0;
        long totalGold = 0;

        // Tower aggregation across runs: type -> stats
        Map<String, TowerAccumulator> towerAccMap = new HashMap<>();

        for (GameSimulation.SimulationResult res : results) {
            if (res.getOutcome() == SimulationOutcome.VICTORY) {
                wins++;
            } else {
                losses++;
            }
            totalBaseHealth += res.getBaseHealth();
            totalTicks += res.getTotalTicks();
            totalGold += res.getGold();

            for (Tower tower : res.getDeployedTowers()) {
                String typeName = tower.getType().name();
                TowerAccumulator acc = towerAccMap.computeIfAbsent(typeName, k -> new TowerAccumulator(typeName, tower.getType().getCost()));
                acc.instancesCount++;
                acc.totalDamage += tower.getTotalDamageDealt();
                acc.totalKills += tower.getTotalKills();
                acc.totalActiveTicks += tower.getActiveFiringTicks();
                acc.totalSimulationTicksObserved += res.getTotalTicks();
            }
        }

        double winRate = (double) wins / effectiveRuns * 100.0;
        double avgBaseHealth = (double) totalBaseHealth / effectiveRuns;
        double avgTicks = (double) totalTicks / effectiveRuns;
        double avgGold = (double) totalGold / effectiveRuns;

        long overallDamageAllTowers = towerAccMap.values().stream().mapToLong(a -> a.totalDamage).sum();
        int overallKillsAllTowers = towerAccMap.values().stream().mapToInt(a -> a.totalKills).sum();

        Map<String, TowerMetrics> towerMetrics = new HashMap<>();
        for (Map.Entry<String, TowerAccumulator> entry : towerAccMap.entrySet()) {
            TowerAccumulator acc = entry.getValue();
            int countPerRun = effectiveRuns > 0 ? (int) Math.round((double) acc.instancesCount / effectiveRuns) : 0;
            double damageShare = overallDamageAllTowers > 0 ? ((double) acc.totalDamage / overallDamageAllTowers) * 100.0 : 0.0;
            double killShare = overallKillsAllTowers > 0 ? ((double) acc.totalKills / overallKillsAllTowers) * 100.0 : 0.0;
            long totalCostInvested = (long) acc.instancesCount * acc.unitCost;
            double costEfficiency = totalCostInvested > 0 ? (double) acc.totalDamage / totalCostInvested : 0.0;
            double utilization = acc.totalSimulationTicksObserved > 0
                    ? ((double) acc.totalActiveTicks / acc.totalSimulationTicksObserved) * 100.0
                    : 0.0;

            towerMetrics.put(entry.getKey(), new TowerMetrics(
                    entry.getKey(),
                    countPerRun,
                    acc.totalDamage,
                    roundTwoDecimals(damageShare),
                    acc.totalKills,
                    roundTwoDecimals(killShare),
                    roundTwoDecimals(costEfficiency),
                    roundTwoDecimals(utilization)
            ));
        }

        return new BatchSimulationResult(
                effectiveRuns,
                wins,
                losses,
                roundTwoDecimals(winRate),
                roundTwoDecimals(avgBaseHealth),
                roundTwoDecimals(avgTicks),
                roundTwoDecimals(avgGold),
                towerMetrics
        );
    }

    private static double roundTwoDecimals(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private static class TowerAccumulator {
        final String towerType;
        final int unitCost;
        int instancesCount = 0;
        long totalDamage = 0;
        int totalKills = 0;
        long totalActiveTicks = 0;
        long totalSimulationTicksObserved = 0;

        TowerAccumulator(String towerType, int unitCost) {
            this.towerType = towerType;
            this.unitCost = unitCost;
        }
    }
}

