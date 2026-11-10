package com.towerlab.engine.analytics;

/**
 * Aggregated pooled statistics for a specific tower type across a batch of runs.
 * Field names strictly match the frontend JSON contract.
 *
 * @param towerType          name of the tower type (e.g. "Cannon", "Frost", "Sniper")
 * @param damageContribution total damage by type / total damage of all types (0 if no damage)
 * @param costEfficiency     total damage by type / total gold spent on type (0 if none spent)
 * @param utilization        total shots / total maxShots (0 if none), capped at 1.0
 */
public record TowerStat(
        String towerType,
        double damageContribution,
        double costEfficiency,
        double utilization
) {}
