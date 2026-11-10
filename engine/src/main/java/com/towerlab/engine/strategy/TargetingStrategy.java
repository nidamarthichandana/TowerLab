package com.towerlab.engine.strategy;

import com.towerlab.engine.model.Tower;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for selecting an enemy target candidate from in-range candidates.
 */
@FunctionalInterface
public interface TargetingStrategy {

    /**
     * Selects a single target candidate from in-range candidates.
     *
     * @param tower      the tower seeking a target
     * @param candidates in-range eligible candidate targets
     * @return an Optional containing the chosen candidate, or empty if none selected
     */
    Optional<TargetCandidate> selectTarget(Tower tower, List<TargetCandidate> candidates);
}

