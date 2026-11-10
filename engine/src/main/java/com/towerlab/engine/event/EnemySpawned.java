package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event published when an enemy spawns on the map.
 *
 * @param tick      simulation tick
 * @param enemyId   unique enemy identifier
 * @param enemyType type of the spawned enemy
 * @param wave      wave number during which the enemy was spawned
 */
@JsonTypeName("EnemySpawned")
public record EnemySpawned(int tick, int enemyId, String enemyType, int wave) implements GameEvent {

    public EnemySpawned {
        Objects.requireNonNull(enemyType, "enemyType must not be null");
    }
}
