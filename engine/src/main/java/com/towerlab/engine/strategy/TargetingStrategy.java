package com.towerlab.engine.strategy;

import com.towerlab.engine.model.Enemy;
import com.towerlab.engine.model.Tower;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for selecting an enemy target from a list of in-range candidates.
 * Production implementations are deferred to a later phase.
 */
@FunctionalInterface
public interface TargetingStrategy {

    /**
     * Selects a single target enemy from in-range candidates.
     *
     * @param tower      the tower seeking a target
     * @param candidates in-range eligible enemies
     * @return an Optional containing the chosen enemy, or empty if none selected
     */
    Optional<Enemy> selectTarget(Tower tower, List<Enemy> candidates);
}
