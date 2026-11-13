package com.towerlab.engine.targeting;

import com.towerlab.engine.model.Creep;
import com.towerlab.engine.model.Tower;

import java.util.Comparator;
import java.util.List;

public class LastTargetingStrategy implements TargetingStrategy {
    private static final Comparator<Creep> COMPARATOR = Comparator
            .comparingDouble(Creep::getPathProgress)
            .thenComparingInt(Creep::getId);

    @Override
    public Creep selectTarget(Tower tower, List<Creep> inRangeCreeps) {
        if (inRangeCreeps == null || inRangeCreeps.isEmpty()) {
            return null;
        }
        return inRangeCreeps.stream().min(COMPARATOR).orElse(null);
    }
}

