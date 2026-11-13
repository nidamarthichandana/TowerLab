package com.towerlab.engine.targeting;

import com.towerlab.engine.model.TargetingMode;

import java.util.EnumMap;
import java.util.Map;

public class TargetingStrategyFactory {
    private static final Map<TargetingMode, TargetingStrategy> STRATEGIES = new EnumMap<>(TargetingMode.class);

    static {
        STRATEGIES.put(TargetingMode.FIRST, new FirstTargetingStrategy());
        STRATEGIES.put(TargetingMode.LAST, new LastTargetingStrategy());
        STRATEGIES.put(TargetingMode.STRONGEST, new StrongestTargetingStrategy());
        STRATEGIES.put(TargetingMode.CLOSEST, new ClosestTargetingStrategy());
    }

    public static TargetingStrategy getStrategy(TargetingMode mode) {
        if (mode == null) {
            return STRATEGIES.get(TargetingMode.FIRST);
        }
        return STRATEGIES.getOrDefault(mode, STRATEGIES.get(TargetingMode.FIRST));
    }
}

