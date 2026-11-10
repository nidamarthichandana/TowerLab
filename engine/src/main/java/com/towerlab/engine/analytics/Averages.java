package com.towerlab.engine.analytics;

/**
 * Mean performance metrics across a batch of simulation runs.
 * Field names strictly match the frontend JSON contract.
 *
 * @param damageDealt          mean damage dealt per run
 * @param enemiesDefeated      mean enemies defeated per run
 * @param leakage              mean enemy leakage per run
 * @param baseHealthRemaining  mean remaining base health per run
 * @param durationTicks        mean game duration in ticks per run
 */
public record Averages(
        double damageDealt,
        double enemiesDefeated,
        double leakage,
        double baseHealthRemaining,
        double durationTicks
) {}
