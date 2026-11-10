package com.towerlab.engine.simulation;

import com.towerlab.engine.event.GameEvent;

import java.util.List;
import java.util.Objects;

/**
 * Result produced by a simulation run.
 *
 * @param outcome    terminal simulation outcome (WIN or LOSS)
 * @param baseHealth player base health remaining at completion
 * @param gold       player gold balance remaining at completion
 * @param ticks      total simulation ticks elapsed
 * @param events     immutable list of all game events published during the run
 */
public record SimulationResult(
        Outcome outcome,
        int baseHealth,
        int gold,
        int ticks,
        List<GameEvent> events
) {
    public SimulationResult {
        Objects.requireNonNull(outcome, "outcome must not be null");
        events = events == null ? List.of() : List.copyOf(events);
    }
}
