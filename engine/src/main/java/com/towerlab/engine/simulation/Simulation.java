package com.towerlab.engine.simulation;

import com.towerlab.engine.config.SimulationConfig;

/**
 * Executes game simulations.
 */
public class Simulation {

    public static final String DUMMY_OUTCOME = "VICTORY";
    public static final int DUMMY_LIVES_REMAINING = 20;
    public static final int DUMMY_GOLD_REMAINING = 150;

    /**
     * Runs a game simulation with the provided configuration.
     * Returns a fixed dummy SimulationResult for the initial stub.
     *
     * @param config the simulation configuration
     * @return the simulation result
     */
    public SimulationResult run(SimulationConfig config) {
        return new SimulationResult(DUMMY_OUTCOME, DUMMY_LIVES_REMAINING, DUMMY_GOLD_REMAINING);
    }
}
