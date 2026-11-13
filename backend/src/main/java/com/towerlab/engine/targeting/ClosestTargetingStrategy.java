package com.towerlab.engine.targeting;

import com.towerlab.engine.model.Creep;
import com.towerlab.engine.model.Tower;

import java.util.Comparator;
import java.util.List;

public class ClosestTargetingStrategy implements TargetingStrategy {

    @Override
    public Creep selectTarget(Tower tower, List<Creep> inRangeCreeps) {
        if (inRangeCreeps == null || inRangeCreeps.isEmpty()) {
            return null;
        }

        Comparator<Creep> comparator = Comparator
                .comparingDouble((Creep c) -> tower.distanceTo(c.getX(), c.getY()))
                .thenComparing(Comparator.comparingDouble(Creep::getPathProgress).reversed())
                .thenComparingInt(Creep::getId);

        return inRangeCreeps.stream().min(comparator).orElse(null);
    }
}

