package com.towerlab.engine.simulation;

import com.towerlab.engine.config.SimulationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimulationTest {

    @Test
    @DisplayName("run() returns the expected fixed dummy SimulationResult")
    void testRunReturnsExpectedFixedResult() {
        Simulation simulation = new Simulation();
        SimulationConfig config = new SimulationConfig();

        SimulationResult result = simulation.run(config);

        assertNotNull(result, "SimulationResult must not be null");
        assertEquals("VICTORY", result.outcome(), "Outcome should match expected fixed value");
        assertEquals(20, result.livesRemaining(), "Lives remaining should match expected fixed value");
        assertEquals(150, result.goldRemaining(), "Gold remaining should match expected fixed value");
    }
}
