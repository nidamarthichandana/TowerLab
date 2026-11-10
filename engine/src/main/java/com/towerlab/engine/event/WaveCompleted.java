package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Event published when all enemies of a wave are defeated.
 *
 * @param tick      simulation tick
 * @param wave      completed wave number
 * @param bonusGold gold awarded as wave-clear bonus
 * @param goldAfter player gold balance after bonus
 */
@JsonTypeName("WaveCompleted")
public record WaveCompleted(
        int tick,
        int wave,
        int bonusGold,
        int goldAfter
) implements GameEvent {
}
