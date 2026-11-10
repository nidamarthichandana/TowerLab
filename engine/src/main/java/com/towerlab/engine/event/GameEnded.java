package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event published when the game session concludes with an outcome (e.g. WON, LOST).
 *
 * @param tick    simulation tick
 * @param outcome terminal outcome (e.g., "WON", "LOST")
 */
@JsonTypeName("GameEnded")
public record GameEnded(int tick, String outcome) implements GameEvent {

    public GameEnded {
        Objects.requireNonNull(outcome, "outcome must not be null");
    }
}
