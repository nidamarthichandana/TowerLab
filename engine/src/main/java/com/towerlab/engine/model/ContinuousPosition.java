package com.towerlab.engine.model;

/**
 * Represents immutable continuous (floating-point) coordinates on the game grid.
 * Used for continuous entity movement and precise location tracking along paths.
 *
 * @param x the continuous x-coordinate
 * @param y the continuous y-coordinate
 */
public record ContinuousPosition(double x, double y) {

    /**
     * Calculates Euclidean distance to another continuous position.
     *
     * @param other target continuous position
     * @return Euclidean distance
     */
    public double distanceTo(ContinuousPosition other) {
        if (other == null) {
            throw new IllegalArgumentException("Target position cannot be null");
        }
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    /**
     * Calculates Euclidean distance to an integer grid position.
     *
     * @param other target grid position
     * @return Euclidean distance
     */
    public double distanceTo(Position other) {
        if (other == null) {
            throw new IllegalArgumentException("Target position cannot be null");
        }
        return Math.hypot(this.x - other.x(), this.y - other.y());
    }

    /**
     * Converts continuous coordinates to the nearest discrete grid position.
     *
     * @return rounded integer Position
     */
    public Position toGridPosition() {
        return new Position((int) Math.round(x), (int) Math.round(y));
    }
}
