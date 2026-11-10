package com.towerlab.engine.strategy;

import com.towerlab.engine.model.Tower;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy selecting the candidate with the smallest distance to the tower (GR-15).
 * Ties are broken by lowest numeric enemy ID.
 */
public final class ClosestTargeting implements TargetingStrategy {

    private static final Comparator<TargetCandidate> COMPARATOR = Comparator
            .comparingDouble(TargetCandidate::distanceToTower)
            .thenComparingInt(c -> c.enemy().id());

    @Override
    public Optional<TargetCandidate> selectTarget(Tower tower, List<TargetCandidate> candidates) {
        if (tower == null) {
            throw new IllegalArgumentException("Tower cannot be null");
        }
        if (candidates == null) {
            throw new IllegalArgumentException("Candidates list cannot be null");
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        for (TargetCandidate candidate : candidates) {
            if (candidate == null) {
                throw new IllegalArgumentException("Target candidate cannot be null");
            }
        }
        return Optional.of(Collections.min(candidates, COMPARATOR));
    }
}
