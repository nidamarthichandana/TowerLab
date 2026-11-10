package com.towerlab.engine.config;

/**
 * Valid targeting capabilities for towers per GR-13 and GR Table 4.
 */
public enum TargetType {
    GROUND,
    AIR,
    BOTH;

    /**
     * Parses a string to TargetType case-insensitively.
     *
     * @param value string name of the target type
     * @return TargetType enum instance
     * @throws IllegalArgumentException if value is null, blank, or unrecognized
     */
    public static TargetType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Target type cannot be null or blank");
        }
        try {
            return TargetType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown target type: " + value, e);
        }
    }
}
