package com.towerlab.engine.strategy;

import com.towerlab.engine.model.ContinuousPosition;
import com.towerlab.engine.model.Enemy;

/**
 * Immutable candidate representation passed to a targeting strategy.
 * Encapsulates the candidate enemy, its continuous position, and precomputed Euclidean distance to the tower.
 *
 * @param enemy           the candidate enemy
 * @param position        the candidate enemy's continuous position
 * @param distanceToTower the Euclidean distance from the tower to the enemy
 */
public record TargetCandidate(Enemy enemy, ContinuousPosition position, double distanceToTower) {

    public TargetCandidate {
        if (enemy == null) {
            throw new IllegalArgumentException("Enemy cannot be null");
        }
        if (position == null) {
            throw new IllegalArgumentException("ContinuousPosition cannot be null");
        }
        if (distanceToTower < 0.0) {
            throw new IllegalArgumentException("Distance to tower cannot be negative, got: " + distanceToTower);
        }
    }
}
