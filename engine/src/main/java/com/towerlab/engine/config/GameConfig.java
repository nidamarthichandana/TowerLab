package com.towerlab.engine.config;

import java.util.Map;

/**
 * Encapsulates a complete set of validated game configurations.
 *
 * @param towers  map of tower type to tower configuration
 * @param enemies map of enemy type to enemy configuration
 * @param level   level configuration
 */
public record GameConfig(
        Map<String, TowerConfig> towers,
        Map<String, EnemyConfig> enemies,
        LevelConfig level
) {
    public GameConfig {
        towers = towers == null ? Map.of() : Map.copyOf(towers);
        enemies = enemies == null ? Map.of() : Map.copyOf(enemies);
    }
}
