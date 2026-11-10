package com.towerlab.engine.analytics;

import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.event.EnemyKilled;
import com.towerlab.engine.event.GameEvent;
import com.towerlab.engine.event.LifeLost;
import com.towerlab.engine.event.TowerBuilt;
import com.towerlab.engine.event.TowerFired;
import com.towerlab.engine.event.TowerMissed;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.simulation.Outcome;
import com.towerlab.engine.simulation.SimulationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Calculates per-run analytics metrics and aggregates batch results.
 * Pure computation derived only from event logs and configurations.
 */
public final class MetricsCalculator {

    private record ShotKey(int tick, int towerId) {}

    private MetricsCalculator() {}

    /**
     * Calculates RunMetrics from a SimulationResult and GameConfig.
     * Derived ONLY from the result and config without re-simulation or randomness.
     *
     * @param result completed simulation result
     * @param config validated game configuration
     * @return immutable RunMetrics
     * @throws InvalidConfigException if arguments are null or contain unknown tower types
     */
    public static RunMetrics calculate(SimulationResult result, GameConfig config) {
        if (result == null) {
            throw new InvalidConfigException("SimulationResult must not be null");
        }
        if (config == null || config.towers() == null) {
            throw new InvalidConfigException("GameConfig and its towers configuration must not be null");
        }

        Map<String, TowerConfig> configuredTowers = config.towers();

        int totalDamageDealt = 0;
        int enemiesDefeated = 0;
        int leakage = 0;

        Map<Integer, String> towerTypeByTowerId = new HashMap<>();
        Map<String, Integer> builtCountByType = new HashMap<>();
        Map<String, List<Integer>> builtTicksByType = new HashMap<>();
        Map<String, Integer> damageByType = new HashMap<>();
        Map<String, Set<ShotKey>> shotsByType = new HashMap<>();

        for (GameEvent event : result.events()) {
            if (event instanceof TowerBuilt tb) {
                String type = tb.towerType();
                if (!configuredTowers.containsKey(type)) {
                    throw new InvalidConfigException("Unknown tower type in TowerBuilt event: " + type);
                }
                towerTypeByTowerId.put(tb.towerId(), type);
                builtCountByType.merge(type, 1, Integer::sum);
                builtTicksByType.computeIfAbsent(type, k -> new ArrayList<>()).add(tb.tick());
            } else if (event instanceof TowerFired tf) {
                String type = tf.towerType();
                if (!configuredTowers.containsKey(type)) {
                    throw new InvalidConfigException("Unknown tower type in TowerFired event: " + type);
                }
                towerTypeByTowerId.putIfAbsent(tf.towerId(), type);
                totalDamageDealt += tf.damageDealt();
                damageByType.merge(type, tf.damageDealt(), Integer::sum);
                shotsByType.computeIfAbsent(type, k -> new HashSet<>())
                        .add(new ShotKey(tf.tick(), tf.towerId()));
            } else if (event instanceof TowerMissed tm) {
                String type = towerTypeByTowerId.get(tm.towerId());
                if (type == null || !configuredTowers.containsKey(type)) {
                    throw new InvalidConfigException("Unknown tower ID or type in TowerMissed event: " + tm.towerId());
                }
                shotsByType.computeIfAbsent(type, k -> new HashSet<>())
                        .add(new ShotKey(tm.tick(), tm.towerId()));
            } else if (event instanceof EnemyKilled) {
                enemiesDefeated++;
            } else if (event instanceof LifeLost) {
                leakage++;
            }
        }

        // Only tower types that were built appear in towerMetrics
        Set<String> builtTowerTypes = new TreeSet<>();
        builtTowerTypes.addAll(builtCountByType.keySet());
        builtTowerTypes.addAll(damageByType.keySet());
        builtTowerTypes.addAll(shotsByType.keySet());

        Map<String, TowerTypeMetrics> towerMetrics = new LinkedHashMap<>();
        int durationTicks = result.ticks();

        for (String type : builtTowerTypes) {
            TowerConfig towerConfig = configuredTowers.get(type);
            int cost = towerConfig.cost();
            int cooldown = towerConfig.cooldown();

            int damage = damageByType.getOrDefault(type, 0);
            int builtCount = builtCountByType.getOrDefault(type, 0);
            int goldSpent = builtCount * cost;
            int shots = shotsByType.getOrDefault(type, Set.of()).size();

            // maxShots: sum over towers of ceil((durationTicks - builtTick) / cooldownTicks)
            int maxShots = 0;
            List<Integer> builtTicks = builtTicksByType.getOrDefault(type, List.of());
            for (int builtTick : builtTicks) {
                int ticksOnMap = Math.max(0, durationTicks - builtTick);
                maxShots += (int) Math.ceil((double) ticksOnMap / cooldown);
            }

            towerMetrics.put(type, new TowerTypeMetrics(damage, goldSpent, shots, maxShots));
        }

        return new RunMetrics(
                result.outcome(),
                durationTicks,
                result.baseHealth(),
                totalDamageDealt,
                enemiesDefeated,
                leakage,
                Collections.unmodifiableMap(towerMetrics)
        );
    }

    /**
     * Aggregates a list of RunMetrics across a batch of runs into a BatchResult.
     * Uses POOLED metrics for tower stats and plain means for averages.
     *
     * @param runs list of RunMetrics to aggregate
     * @return BatchResult with pooled tower stats and averages
     */
    public static BatchResult aggregate(List<RunMetrics> runs) {
        if (runs == null || runs.isEmpty()) {
            return new BatchResult(0, 0.0, new Averages(0.0, 0.0, 0.0, 0.0, 0.0), List.of(), List.of());
        }

        int count = runs.size();
        long wins = runs.stream().filter(r -> r.outcome() == Outcome.WIN).count();
        double winRate = (double) wins / count;

        double totalDamage = runs.stream().mapToDouble(RunMetrics::damageDealt).sum();
        double totalKills = runs.stream().mapToDouble(RunMetrics::enemiesDefeated).sum();
        double totalLeakage = runs.stream().mapToDouble(RunMetrics::leakage).sum();
        double totalBaseHealth = runs.stream().mapToDouble(RunMetrics::baseHealthRemaining).sum();
        double totalDuration = runs.stream().mapToDouble(RunMetrics::durationTicks).sum();

        Averages averages = new Averages(
                totalDamage / count,
                totalKills / count,
                totalLeakage / count,
                totalBaseHealth / count,
                totalDuration / count
        );

        // Find all built tower types across all runs, ordered alphabetically
        Set<String> builtTypes = new TreeSet<>();
        for (RunMetrics rm : runs) {
            builtTypes.addAll(rm.towerMetrics().keySet());
        }

        // Sum pooled metrics per tower type
        double totalDamageAllTypes = 0.0;
        Map<String, Long> pooledDamageByType = new HashMap<>();
        Map<String, Long> pooledGoldSpentByType = new HashMap<>();
        Map<String, Long> pooledShotsByType = new HashMap<>();
        Map<String, Long> pooledMaxShotsByType = new HashMap<>();

        for (String type : builtTypes) {
            long typeDamage = 0;
            long typeGoldSpent = 0;
            long typeShots = 0;
            long typeMaxShots = 0;

            for (RunMetrics rm : runs) {
                TowerTypeMetrics tm = rm.towerMetrics().get(type);
                if (tm != null) {
                    typeDamage += tm.damage();
                    typeGoldSpent += tm.goldSpent();
                    typeShots += tm.shots();
                    typeMaxShots += tm.maxShots();
                }
            }

            pooledDamageByType.put(type, typeDamage);
            pooledGoldSpentByType.put(type, typeGoldSpent);
            pooledShotsByType.put(type, typeShots);
            pooledMaxShotsByType.put(type, typeMaxShots);
            totalDamageAllTypes += typeDamage;
        }

        List<TowerStat> towerStats = new ArrayList<>(builtTypes.size());
        for (String type : builtTypes) {
            long typeDamage = pooledDamageByType.getOrDefault(type, 0L);
            long typeGoldSpent = pooledGoldSpentByType.getOrDefault(type, 0L);
            long typeShots = pooledShotsByType.getOrDefault(type, 0L);
            long typeMaxShots = pooledMaxShotsByType.getOrDefault(type, 0L);

            double damageContribution = totalDamageAllTypes == 0.0 ? 0.0 : (double) typeDamage / totalDamageAllTypes;
            double costEfficiency = typeGoldSpent == 0 ? 0.0 : (double) typeDamage / typeGoldSpent;
            double utilization = typeMaxShots == 0 ? 0.0 : Math.min(1.0, (double) typeShots / typeMaxShots);

            towerStats.add(new TowerStat(type, damageContribution, costEfficiency, utilization));
        }

        return new BatchResult(count, winRate, averages, Collections.unmodifiableList(towerStats), List.of());
    }
}
