package com.towerlab.engine.analytics;

/**
 * Immutable metrics for a single tower type within an individual simulation run.
 *
 * @param damage    total damage dealt by towers of this type (sum of TowerFired.damageDealt)
 * @param goldSpent total gold spent on towers of this type (count of TowerBuilt times configured cost)
 * @param shots     distinct (tick, towerId) attack pairs across TowerFired and TowerMissed
 * @param maxShots  sum over built towers of ceil((durationTicks - builtTick) / cooldownTicks)
 */
public record TowerTypeMetrics(
        int damage,
        int goldSpent,
        int shots,
        int maxShots
) {
    public static final TowerTypeMetrics ZERO = new TowerTypeMetrics(0, 0, 0, 0);
}
