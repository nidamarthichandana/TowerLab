package com.towerlab.engine.model;

import com.towerlab.engine.config.LevelConfig;

import java.util.List;

/**
 * Represents an immutable predefined path built from level waypoints.
 * Calculates total path length and continuous positions at any progress distance.
 */
public class Path {

    private final List<Position> waypoints;
    private final double[] segmentLengths;
    private final double[] cumulativeLengths;
    private final double totalLength;

    /**
     * Constructs a Path from an ordered list of waypoints.
     *
     * @param waypoints ordered grid waypoints (must contain at least 2 points)
     */
    public Path(List<Position> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("Path must contain at least 2 waypoints");
        }
        this.waypoints = List.copyOf(waypoints);
        int numSegments = this.waypoints.size() - 1;
        this.segmentLengths = new double[numSegments];
        this.cumulativeLengths = new double[numSegments];

        double runningTotal = 0.0;
        for (int i = 0; i < numSegments; i++) {
            Position p1 = this.waypoints.get(i);
            Position p2 = this.waypoints.get(i + 1);
            double length = p1.distanceTo(p2);
            segmentLengths[i] = length;
            runningTotal += length;
            cumulativeLengths[i] = runningTotal;
        }
        this.totalLength = runningTotal;
    }

    /**
     * Constructs a Path from a LevelConfig instance.
     *
     * @param levelConfig the level configuration containing waypoints
     */
    public Path(LevelConfig levelConfig) {
        this(levelConfig == null ? null : levelConfig.toWaypoints());
    }

    /**
     * Returns the total length of the path in grid cells.
     *
     * @return total distance along the path
     */
    public double totalLength() {
        return totalLength;
    }

    /**
     * Calculates the continuous coordinates at a given cumulative distance along the path.
     *
     * @param distance cumulative distance from the path start
     * @return ContinuousPosition representing the coordinates at that distance
     */
    public ContinuousPosition pointAt(double distance) {
        if (distance <= 0.0) {
            Position start = waypoints.get(0);
            return new ContinuousPosition(start.x(), start.y());
        }
        if (distance >= totalLength) {
            Position end = waypoints.get(waypoints.size() - 1);
            return new ContinuousPosition(end.x(), end.y());
        }

        for (int i = 0; i < segmentLengths.length; i++) {
            if (distance <= cumulativeLengths[i]) {
                double prevCumulative = i == 0 ? 0.0 : cumulativeLengths[i - 1];
                double distanceIntoSegment = distance - prevCumulative;
                double segLen = segmentLengths[i];
                double t = segLen <= 0.0 ? 0.0 : distanceIntoSegment / segLen;

                Position p1 = waypoints.get(i);
                Position p2 = waypoints.get(i + 1);
                double x = p1.x() + t * (p2.x() - p1.x());
                double y = p1.y() + t * (p2.y() - p1.y());
                return new ContinuousPosition(x, y);
            }
        }

        Position end = waypoints.get(waypoints.size() - 1);
        return new ContinuousPosition(end.x(), end.y());
    }

    /**
     * Returns the ordered immutable list of waypoints.
     *
     * @return list of waypoints
     */
    public List<Position> waypoints() {
        return waypoints;
    }
}
