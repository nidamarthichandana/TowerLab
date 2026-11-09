package com.towerlab.engine.model;

/**
 * Defines entities capable of taking damage, tracking health, and checking alive status.
 */
public interface Damageable {

    /**
     * Applies raw damage considering entity armour per GR-18:
     * final damage = max(1, round(rawDamage * (1 - armour))).
     *
     * @param rawDamage incoming damage before armour calculation (must be positive)
     * @return actual damage dealt to health
     */
    int takeDamage(int rawDamage);

    /**
     * Applies true damage directly to health, completely ignoring armour (used for burn damage per GR-18).
     *
     * @param amount true damage amount (must be positive)
     * @return damage dealt to health
     */
    int takeTrueDamage(int amount);

    /**
     * Checks if the entity is currently alive (health > 0).
     *
     * @return true if health > 0, false otherwise
     */
    boolean isAlive();

    /**
     * Returns the current health of the entity.
     *
     * @return current health points
     */
    int currentHealth();
}
