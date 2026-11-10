package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Event published when a new wave starts.
 *
 * @param tick simulation tick
 * @param wave 1-based wave number
 */
@JsonTypeName("WaveStarted")
public record WaveStarted(int tick, int wave) implements GameEvent {
}
