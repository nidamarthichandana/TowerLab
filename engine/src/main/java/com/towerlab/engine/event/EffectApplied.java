package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event published when a status effect is successfully applied to an enemy.
 *
 * @param tick          simulation tick
 * @param enemyId       affected enemy ID
 * @param effectType    name/type of the status effect
 * @param magnitude     strength of the effect
 * @param durationTicks duration of the effect in ticks
 */
@JsonTypeName("EffectApplied")
public record EffectApplied(
        int tick,
        int enemyId,
        String effectType,
        double magnitude,
        int durationTicks
) implements GameEvent {

    public EffectApplied {
        Objects.requireNonNull(effectType, "effectType must not be null");
    }
}
