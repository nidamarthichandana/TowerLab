package com.towerlab.engine.config;

/**
 * Minimal configuration record for initializing game simulations.
 *
 * @param initialLives the player's starting lives/health
 * @param initialGold  the player's starting gold/coins
 * @param totalWaves   the number of waves in the simulation
 * @param maxTicks     the maximum tick limit before timeout
 * @param randomSeed   seed used for reproducible simulation runs
 */
public record SimulationConfig(
        int initialLives,
        int initialGold,
        int totalWaves,
        int maxTicks,
        long randomSeed
) {
    /**
     * Default constructor providing standard MVP baseline values.
     */
    public SimulationConfig() {
        this(20, 150, 10, 20000, 1L);
    }
}
