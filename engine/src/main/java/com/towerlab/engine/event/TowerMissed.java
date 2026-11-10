package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Event published when a tower's attack misses due to enemy evasion per GR-19.
 *
 * @param tick    simulation tick
 * @param towerId firing tower ID
 * @param enemyId target enemy ID that evaded
 */
@JsonTypeName("TowerMissed")
public record TowerMissed(int tick, int towerId, int enemyId) implements GameEvent {
}
