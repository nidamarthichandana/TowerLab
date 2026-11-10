package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Event published when an enemy moves to a new position along the path.
 *
 * @param tick    simulation tick
 * @param enemyId unique enemy identifier
 * @param x       current x-coordinate
 * @param y       current y-coordinate
 */
@JsonTypeName("EnemyMoved")
public record EnemyMoved(int tick, int enemyId, double x, double y) implements GameEvent {
}
