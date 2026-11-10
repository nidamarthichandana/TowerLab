package com.towerlab.engine.analytics;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of a batch of simulation runs.
 * Field names strictly match the frontend JSON contract.
 *
 * @param runs       total number of runs executed
 * @param winRate    fraction of runs ending in WIN (wins / runs)
 * @param averages   mean performance metrics across runs
 * @param towerStats pooled statistics per built tower type, sorted by type name
 * @param findings   list of balance findings (empty in Phase 5A)
 */
public record BatchResult(
        int runs,
        double winRate,
        Averages averages,
        List<TowerStat> towerStats,
        List<Finding> findings
) {
    public BatchResult {
        Objects.requireNonNull(averages, "averages must not be null");
        towerStats = towerStats == null ? List.of() : List.copyOf(towerStats);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
