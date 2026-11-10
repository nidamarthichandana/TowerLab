package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event published when a tower fires and deals damage to an enemy (one event per damaged enemy).
 *
 * @param tick        simulation tick
 * @param towerId     firing tower ID
 * @param towerType   firing tower type
 * @param enemyId     damaged enemy ID
 * @param damageDealt actual damage dealt
 */
@JsonTypeName("TowerFired")
public record TowerFired(
        int tick,
        int towerId,
        String towerType,
        int enemyId,
        int damageDealt
) implements GameEvent {

    public TowerFired {
        Objects.requireNonNull(towerType, "towerType must not be null");
    }
}
