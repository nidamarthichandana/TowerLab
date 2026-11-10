package com.towerlab.engine.model;

import com.towerlab.engine.config.LevelConfig;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the immutable grid map for a game level per GR-7, GR-8, and GR-9.
 * Tracks grid bounds, path, and precomputes all discrete cells covered by path segments.
 * Has no awareness of towers or enemies.
 */
public final class GameMap {

    private final int width;
    private final int height;
    private final Path path;
    private final Set<Position> pathCells;

    /**
     * Constructs a GameMap from a validated LevelConfig.
     *
     * @param levelConfig the level configuration
     */
    public GameMap(LevelConfig levelConfig) {
        if (levelConfig == null) {
            throw new IllegalArgumentException("LevelConfig cannot be null");
        }
        if (levelConfig.width() <= 0 || levelConfig.height() <= 0) {
            throw new IllegalArgumentException("Map dimensions must be positive");
        }
        this.width = levelConfig.width();
        this.height = levelConfig.height();
        this.path = new Path(levelConfig);

        Set<Position> cells = new LinkedHashSet<>();
        List<Position> waypoints = levelConfig.toWaypoints();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Position p1 = waypoints.get(i);
            Position p2 = waypoints.get(i + 1);
            if (p1.x() == p2.x()) {
                int minY = Math.min(p1.y(), p2.y());
                int maxY = Math.max(p1.y(), p2.y());
                for (int y = minY; y <= maxY; y++) {
                    cells.add(new Position(p1.x(), y));
                }
            } else if (p1.y() == p2.y()) {
                int minX = Math.min(p1.x(), p2.x());
                int maxX = Math.max(p1.x(), p2.x());
                for (int x = minX; x <= maxX; x++) {
                    cells.add(new Position(x, p1.y()));
                }
            } else {
                throw new IllegalArgumentException("Non axis-aligned segment between " + p1 + " and " + p2);
            }
        }
        this.pathCells = Set.copyOf(cells);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Path path() {
        return path;
    }

    public Set<Position> pathCells() {
        return pathCells;
    }

    public boolean isInsideGrid(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        return position.x() >= 0 && position.x() < width && position.y() >= 0 && position.y() < height;
    }

    public boolean isOnPath(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        return pathCells.contains(position);
    }

    public boolean isBuildable(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        return isInsideGrid(position) && !isOnPath(position);
    }
}
