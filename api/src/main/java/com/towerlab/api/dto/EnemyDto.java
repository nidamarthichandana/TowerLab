package com.towerlab.api.dto;

/**
 * Enemy summary for the frontend catalog.
 *
 * @param type       enemy type name
 * @param health     maximum health
 * @param speed      movement speed in cells per tick
 * @param armour     damage reduction factor in [0.0, 1.0]
 * @param reward     gold awarded on defeat
 * @param baseDamage damage dealt to player base if leaked
 */
public record EnemyDto(
        String type,
        double health,
        double speed,
        double armour,
        int reward,
        int baseDamage
) {}
