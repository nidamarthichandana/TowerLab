package com.towerlab.engine.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.towerlab.engine.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable configuration record for a level, including map dimensions, path layout,
 * allowed towers, and waves.
 *
 * @param name          the level name
 * @param width         grid width in cells (must be positive, per GR-7)
 * @param height        grid height in cells (must be positive, per GR-7)
 * @param waypoints     ordered list of path waypoints (per GR-7, GR-8)
 * @param segments      explicit list of path segments (optional alternative or companion to waypoints)
 * @param allowedTowers tower types available to build in this level
 * @param waves         ordered waves of enemy spawn groups (per GR-28)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LevelConfig(
        @JsonAlias({"name", "levelName", "id"}) String name,
        @JsonAlias({"width", "mapWidth", "gridWidth"}) int width,
        @JsonAlias({"height", "mapHeight", "gridHeight"}) int height,
        @JsonAlias({"waypoints", "path"}) List<Position> waypoints,
        @JsonAlias({"segments", "pathSegments"}) List<PathSegment> segments,
        @JsonAlias({"allowedTowers", "availableTowers", "towers"}) List<String> allowedTowers,
        @JsonAlias({"waves", "waveConfigurations"}) List<WaveConfig> waves
) {
    public LevelConfig {
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
        segments = segments == null ? List.of() : List.copyOf(segments);
        allowedTowers = allowedTowers == null ? List.of() : List.copyOf(allowedTowers);
        waves = waves == null ? List.of() : List.copyOf(waves);
    }

    public LevelConfig(String name, int width, int height, List<Position> waypoints, List<String> allowedTowers, List<WaveConfig> waves) {
        this(name, width, height, waypoints, List.of(), allowedTowers, waves);
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
