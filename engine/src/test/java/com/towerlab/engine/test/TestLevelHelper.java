package com.towerlab.engine.test;

import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.PathSegment;
import com.towerlab.engine.config.SpawnGroupConfig;
import com.towerlab.engine.config.WaveConfig;
import com.towerlab.engine.model.Position;

import java.util.List;

/**
 * Test helper for constructing valid LevelConfig instances with explicit values.
 */
public final class TestLevelHelper {

    public static final int DEFAULT_STARTING_GOLD = 150;
    public static final int DEFAULT_STARTING_BASE_HEALTH = 20;
    public static final int DEFAULT_PREP_PERIOD_TICKS = 100;
    public static final int DEFAULT_WAVE_CLEAR_BONUS = 20;
    public static final int DEFAULT_MAX_TICKS = 20000;
    public static final int DEFAULT_SPAWN_JITTER_TICKS = 3;

    private TestLevelHelper() {}

    /**
     * Creates a LevelConfig with explicit values for every property including waveClearBonus, maxTicks, spawnJitterTicks.
     */
    public static LevelConfig createLevel(
            String name,
            int width,
            int height,
            List<Position> waypoints,
            List<PathSegment> segments,
            List<String> allowedTowers,
            List<WaveConfig> waves,
            Integer startingGold,
            Integer startingBaseHealth,
            Integer prepPeriodTicks,
            Integer waveClearBonus,
            Integer maxTicks,
            Integer spawnJitterTicks
    ) {
        return new LevelConfig(name, width, height, waypoints, segments, allowedTowers, waves,
                startingGold, startingBaseHealth, prepPeriodTicks, waveClearBonus, maxTicks, spawnJitterTicks);
    }

    /**
     * Creates a LevelConfig with explicit values for every property, using default bonus/ticks/jitter.
     */
    public static LevelConfig createLevel(
            String name,
            int width,
            int height,
            List<Position> waypoints,
            List<PathSegment> segments,
            List<String> allowedTowers,
            List<WaveConfig> waves,
            Integer startingGold,
            Integer startingBaseHealth,
            Integer prepPeriodTicks
    ) {
        return createLevel(name, width, height, waypoints, segments, allowedTowers, waves,
                startingGold, startingBaseHealth, prepPeriodTicks,
                DEFAULT_WAVE_CLEAR_BONUS, DEFAULT_MAX_TICKS, DEFAULT_SPAWN_JITTER_TICKS);
    }

    /**
     * Creates a LevelConfig with waypoints and explicit starting values.
     */
    public static LevelConfig createLevel(
            String name,
            int width,
            int height,
            List<Position> waypoints,
            List<String> allowedTowers,
            List<WaveConfig> waves,
            Integer startingGold,
            Integer startingBaseHealth,
            Integer prepPeriodTicks
    ) {
        return createLevel(name, width, height, waypoints, List.of(), allowedTowers, waves,
                startingGold, startingBaseHealth, prepPeriodTicks);
    }

    /**
     * Creates a standard LevelConfig with explicit default values.
     */
    public static LevelConfig createStandardLevel(
            String name,
            int width,
            int height,
            List<Position> waypoints,
            List<String> allowedTowers,
            List<WaveConfig> waves
    ) {
        return createLevel(name, width, height, waypoints, allowedTowers, waves,
                DEFAULT_STARTING_GOLD, DEFAULT_STARTING_BASE_HEALTH, DEFAULT_PREP_PERIOD_TICKS);
    }

    /**
     * Creates a standard LevelConfig with explicit segments and default values.
     */
    public static LevelConfig createStandardLevelWithSegments(
            String name,
            int width,
            int height,
            List<Position> waypoints,
            List<PathSegment> segments,
            List<String> allowedTowers,
            List<WaveConfig> waves
    ) {
        return createLevel(name, width, height, waypoints, segments, allowedTowers, waves,
                DEFAULT_STARTING_GOLD, DEFAULT_STARTING_BASE_HEALTH, DEFAULT_PREP_PERIOD_TICKS);
    }
}
