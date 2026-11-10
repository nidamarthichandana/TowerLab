package com.towerlab.engine.strategy;

import java.util.Locale;

/**
 * Enumeration of available tower targeting modes per GR-14 and GR-15.
 * Provides access to the corresponding stateless TargetingStrategy instance.
 */
public enum TargetingMode {

    FIRST(new FirstTargeting()),
    LAST(new LastTargeting()),
    STRONGEST(new StrongestTargeting()),
    CLOSEST(new ClosestTargeting());

    private final TargetingStrategy strategy;

    TargetingMode(TargetingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns the stateless strategy instance for this targeting mode.
     *
     * @return the matching TargetingStrategy
     */
    public TargetingStrategy strategy() {
        return strategy;
    }

    /**
     * Parses a string representation into a TargetingMode case-insensitively.
     *
     * @param value string name of the targeting mode
     * @return the matching TargetingMode
     * @throws IllegalArgumentException if value is null, blank, or unrecognized
     */
    public static TargetingMode fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Targeting mode cannot be null or blank");
        }
        try {
            return TargetingMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown targeting mode: " + value, e);
        }
    }
}
