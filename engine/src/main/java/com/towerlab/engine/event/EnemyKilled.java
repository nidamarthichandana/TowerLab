package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event published when an enemy is defeated by tower attacks or effects.
 *
 * @param tick      simulation tick
 * @param enemyId   defeated enemy ID
 * @param enemyType type of the defeated enemy
 * @param towerId   ID of the tower that dealt the fatal hit
 * @param reward    gold awarded for defeating the enemy
 * @param goldAfter total player gold balance after reward
 */
@JsonTypeName("EnemyKilled")
public record EnemyKilled(
        int tick,
        int enemyId,
        String enemyType,
        int towerId,
        int reward,
        int goldAfter
) implements GameEvent {

    public EnemyKilled {
        Objects.requireNonNull(enemyType, "enemyType must not be null");
    }
}
