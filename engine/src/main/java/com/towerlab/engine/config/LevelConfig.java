package com.towerlab.engine.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.towerlab.engine.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable configuration record for a level, including map dimensions, path layout,
 * allowed towers, waves, and starting parameters per GR-4, GR-7, GR-28, and GR-31.
 *
 * @param name               the level name
 * @param width              grid width in cells (must be positive, per GR-7)
 * @param height             grid height in cells (must be positive, per GR-7)
 * @param waypoints          ordered list of path waypoints (per GR-7, GR-8)
 * @param segments           explicit list of path segments (optional alternative or companion to waypoints)
 * @param allowedTowers      tower types available to build in this level
 * @param waves              ordered waves of enemy spawn groups (per GR-28)
 * @param startingGold       starting gold amount (must be positive, per GR-4)
 * @param startingBaseHealth starting player base health (must be positive, per GR-4)
 * @param prepPeriodTicks    preparation period duration in ticks (must be positive, per GR-31)
 * @param waveClearBonus     gold awarded on wave completion (must be positive, per GR-30)
 * @param maxTicks           maximum tick limit for simulation (must be positive, per GR-6)
 * @param spawnJitterTicks   spawn jitter size in ticks (must be positive, per GR-29)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LevelConfig(
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
    public LevelConfig {
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
        segments = segments == null ? List.of() : List.copyOf(segments);
        allowedTowers = allowedTowers == null ? List.of() : List.copyOf(allowedTowers);
        waves = waves == null ? List.of() : List.copyOf(waves);
    }

    /**
     * Converts the waypoints into a list of PathSegments.
     */
    public List<PathSegment> waypointsToSegments() {
        List<PathSegment> result = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            result.add(new PathSegment(waypoints.get(i), waypoints.get(i + 1)));
        }
        return List.copyOf(result);
    }

    /**
     * Converts either explicit segments or waypoints into a unified list of PathSegments.
     *
     * @return list of path segments
     */
    public List<PathSegment> toSegments() {
        if (!segments.isEmpty()) {
            return segments;
        }
        return waypointsToSegments();
    }

    /**
     * Converts either explicit segments or waypoints into an ordered list of waypoints.
     *
     * @return ordered list of positions along the path
     */
    public List<Position> toWaypoints() {
        if (!waypoints.isEmpty()) {
            return waypoints;
        }
        List<Position> result = new ArrayList<>();
        if (!segments.isEmpty()) {
            result.add(segments.get(0).start());
            for (PathSegment seg : segments) {
                result.add(seg.end());
            }
        }
        return List.copyOf(result);
    }
}
