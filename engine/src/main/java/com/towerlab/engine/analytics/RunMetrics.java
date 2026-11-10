package com.towerlab.engine.analytics;

import com.towerlab.engine.simulation.Outcome;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable record capturing metrics derived from a single SimulationResult and GameConfig.
 *
 * @param outcome              terminal outcome of the simulation (WIN or LOSS)
 * @param durationTicks        total duration of the simulation in ticks
 * @param baseHealthRemaining  remaining player base health at end of simulation
 * @param damageDealt          total damage dealt across all towers (sum of TowerFired.damageDealt)
 * @param enemiesDefeated      total enemies defeated (count of EnemyKilled)
 * @param leakage              total enemy leakage (count of LifeLost)
 * @param towerMetrics         immutable map of per-tower-type metrics ordered by tower type name
 */
public record RunMetrics(
        Outcome outcome,
        int durationTicks,
        int baseHealthRemaining,
        int damageDealt,
        int enemiesDefeated,
        int leakage,
        Map<String, TowerTypeMetrics> towerMetrics
) {
    public RunMetrics {
        Objects.requireNonNull(outcome, "outcome must not be null");
        towerMetrics = towerMetrics == null ? Collections.emptyMap() : Collections.unmodifiableMap(towerMetrics);
    }

    /**
     * Alias for towerMetrics for flexible caller ergonomics.
     */
    public Map<String, TowerTypeMetrics> perTowerType() {
        return towerMetrics;
    }
}
