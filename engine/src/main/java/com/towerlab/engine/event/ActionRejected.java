package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event published when a player action cannot be executed and is rejected.
 *
 * @param tick       simulation tick
 * @param actionType type/name of the action rejected
 * @param reason     detailed reason for rejection
 */
@JsonTypeName("ActionRejected")
public record ActionRejected(
        int tick,
        String actionType,
        String reason
) implements GameEvent {

    public ActionRejected {
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
