package com.towerlab.engine.model;

import java.util.List;

public class Creep {
    private final int id;
    private final CreepType type;
    private final int spawnTick;
    private double currentHealth;
    private double x;
    private double y;
    private int waypointIndex; // index of next target waypoint (from 1 to waypoints.size() - 1)
    private double pathProgress; // total distance traveled along path
    private double slowFactor;
    private int slowDurationTicks;
    private boolean alive;
    private boolean leaked;

    public Creep(int id, CreepType type, int spawnTick, PathWaypoint startWaypoint) {
        this.id = id;
        this.type = type;
        this.spawnTick = spawnTick;
        this.currentHealth = type.getBaseHealth();
        this.x = startWaypoint.getX();
        this.y = startWaypoint.getY();
        this.waypointIndex = 1;
        this.pathProgress = 0.0;
        this.slowFactor = 0.0;
        this.slowDurationTicks = 0;
        this.alive = true;
        this.leaked = false;
    }

    public void updateStatusEffects() {
        if (slowDurationTicks > 0) {
            slowDurationTicks--;
            if (slowDurationTicks == 0) {
                slowFactor = 0.0;
            }
        }
    }

    public void applySlow(double factor, int duration) {
        // Non-stacking slow, applies strongest factor and refreshes duration
        if (factor >= this.slowFactor || this.slowDurationTicks == 0) {
            this.slowFactor = factor;
        }
        this.slowDurationTicks = Math.max(this.slowDurationTicks, duration);
    }

    public void advanceAlongPath(List<PathWaypoint> waypoints) {
        if (!alive || leaked || waypointIndex >= waypoints.size()) {
            return;
        }

        double effectiveSpeed = type.getSpeed() * (1.0 - slowFactor);
        double distanceToTravel = effectiveSpeed;

        while (distanceToTravel > 0 && waypointIndex < waypoints.size()) {
            PathWaypoint targetWp = waypoints.get(waypointIndex);
            double dx = targetWp.getX() - this.x;
            double dy = targetWp.getY() - this.y;
            double distToTarget = Math.hypot(dx, dy);

            if (distToTarget <= distanceToTravel) {
                // Reached target waypoint
                this.x = targetWp.getX();
                this.y = targetWp.getY();
                distanceToTravel -= distToTarget;
                pathProgress += distToTarget;
                waypointIndex++;

                if (waypointIndex >= waypoints.size()) {
                    // Reached the base!
                    this.leaked = true;
                    this.alive = false;
                    return;
                }
            } else {
                // Move partially towards target waypoint
                double ratio = distanceToTravel / distToTarget;
                this.x += dx * ratio;
                this.y += dy * ratio;
                pathProgress += distanceToTravel;
                distanceToTravel = 0;
            }
        }
    }

    public void takeDamage(int amount) {
        this.currentHealth = Math.max(0, this.currentHealth - amount);
        if (this.currentHealth <= 0) {
            this.alive = false;
        }
    }

    public int getId() {
        return id;
    }

    public CreepType getType() {
        return type;
    }

    public int getSpawnTick() {
        return spawnTick;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWaypointIndex() {
        return waypointIndex;
    }

    public double getPathProgress() {
        return pathProgress;
    }

    public double getSlowFactor() {
        return slowFactor;
    }

    public int getSlowDurationTicks() {
        return slowDurationTicks;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isLeaked() {
        return leaked;
    }
}

