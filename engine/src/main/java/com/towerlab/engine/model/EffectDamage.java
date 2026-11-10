package com.towerlab.engine.model;

/**
 * Immutable record representing damage dealt to an entity by a status effect.
 *
 * @param sourceTowerId identifier of the tower that inflicted the effect
 * @param amount        actual damage amount dealt to the entity
 */
public record EffectDamage(int sourceTowerId, int amount) {
    public EffectDamage {
        if (amount < 0) {
            throw new IllegalArgumentException("Effect damage amount cannot be negative, got: " + amount);
        }
    }
}
