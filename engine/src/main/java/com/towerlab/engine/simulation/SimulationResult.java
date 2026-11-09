package com.towerlab.engine.simulation;

/**
 * Result produced by a simulation run.
 *
 * @param outcome        the simulation outcome (e.g., "VICTORY", "DEFEAT")
 * @param livesRemaining the number of player lives remaining at completion
 * @param goldRemaining  the amount of gold remaining at completion
 */
public record SimulationResult(
        String outcome,
        int livesRemaining,
        int goldRemaining
) {
}
