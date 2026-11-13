package com.towerlab.engine.model;

public enum TowerType {
    CANNON(100, 2.5, 10, 45, 0.0, 0, "Reliable mid-range physical projectile with steady DPS."),
    SNIPER(150, 6.0, 25, 140, 0.0, 0, "Long-range precision rifle delivering massive burst damage."),
    FROST(125, 2.0, 15, 15, 0.40, 20, "Frost emitter that slows enemy advance by 40% for 20 ticks.");

    private final int cost;
    private final double range;
    private final int fireRateTicks;
    private final int baseDamage;
    private final double slowFactor;
    private final int slowDurationTicks;
    private final String description;

    TowerType(int cost, double range, int fireRateTicks, int baseDamage, double slowFactor, int slowDurationTicks, String description) {
        this.cost = cost;
        this.range = range;
        this.fireRateTicks = fireRateTicks;
        this.baseDamage = baseDamage;
        this.slowFactor = slowFactor;
        this.slowDurationTicks = slowDurationTicks;
        this.description = description;
    }

    public int getCost() {
        return cost;
    }

    public double getRange() {
        return range;
    }

    public int getFireRateTicks() {
        return fireRateTicks;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public double getSlowFactor() {
        return slowFactor;
    }

    public int getSlowDurationTicks() {
        return slowDurationTicks;
    }

    public String getDescription() {
        return description;
    }
}

