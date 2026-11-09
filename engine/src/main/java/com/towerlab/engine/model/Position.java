package com.towerlab.engine.model;

/**
 * Represents immutable integer coordinates on the game grid.
 *
 * @param x the x-coordinate
 * @param y the y-coordinate
 */
public record Position(int x, int y) {

    /**
     * Calculates Euclidean distance to another position.
     *
     * @param other target position
     * @return Euclidean distance between positions
     */
    public double distanceTo(Position other) {
        if (other == null) {
            throw new IllegalArgumentException("Target position cannot be null");
        }
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.hypot(dx, dy);
    }
}
