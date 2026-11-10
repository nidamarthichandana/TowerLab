package com.towerlab.engine.model;

/**
 * Immutable scheduled spawn entry representing a relative tick and the enemy type to spawn.
 *
 * @param relativeTick nominal tick offset relative to the wave start
 * @param enemyType    the type of enemy to spawn
 */
public record ScheduledSpawn(int relativeTick, String enemyType) {

    public ScheduledSpawn {
        if (enemyType == null || enemyType.isBlank()) {
            throw new IllegalArgumentException("Enemy type cannot be null or blank");
        }
        if (relativeTick < 0) {
            throw new IllegalArgumentException("Relative tick cannot be negative, got: " + relativeTick);
        }
    }

    public int tick() {
        return relativeTick;
    }
}
