package com.towerlab.engine.model;

import java.util.Objects;

public class PathWaypoint {
    private final int x;
    private final int y;

    public PathWaypoint() {
        this.x = 0;
        this.y = 0;
    }

    public PathWaypoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double distanceTo(PathWaypoint other) {
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathWaypoint that)) return false;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Waypoint(" + x + ", " + y + ")";
    }
}

