package com.towerlab.engine.model;

/**
 * Represents the outcome of an attack on an individual enemy.
 *
 * @param enemyId       the numeric identifier of the target enemy
 * @param damageDealt   actual health lost by the enemy (0 if evaded or dead)
 * @param evaded        whether the enemy evaded the attack
 * @param effectApplied whether a status effect was successfully applied
 * @param killed        whether this hit delivered the killing blow (wasAliveBeforeAttack && !enemy.isAlive())
 */
public record HitResult(
        int enemyId,
        int damageDealt,
        boolean evaded,
        boolean effectApplied,
        boolean killed
) {
    public HitResult {
        if (enemyId < 0) {
            throw new IllegalArgumentException("Enemy id cannot be negative, got: " + enemyId);
        }
        if (damageDealt < 0) {
            throw new IllegalArgumentException("Damage dealt cannot be negative, got: " + damageDealt);
        }
    }
}
