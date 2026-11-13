package com.towerlab.engine.targeting;

import com.towerlab.engine.model.Creep;
import com.towerlab.engine.model.Tower;

import java.util.List;

public interface TargetingStrategy {
    /**
     * Selects a single creep target from the list of creeps currently within the tower's range.
     * Must be completely deterministic: if properties are tied, break tie by creepId ASC.
     *
     * @param tower The attacking tower
     * @param inRangeCreeps Alive creeps within range
     * @return Selected Creep, or null if list is empty
     */
    Creep selectTarget(Tower tower, List<Creep> inRangeCreeps);
}

