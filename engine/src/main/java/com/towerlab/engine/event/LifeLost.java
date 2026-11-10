package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Event published when an enemy reaches the end of the path and inflicts base damage.
 *
 * @param tick            simulation tick
 * @param enemyId         enemy ID that reached base
 * @param damage          damage dealt to the player's base
 * @param baseHealthAfter base health remaining after damage
 */
@JsonTypeName("LifeLost")
public record LifeLost(
        int tick,
        int enemyId,
        int damage,
        int baseHealthAfter
) implements GameEvent {
}
