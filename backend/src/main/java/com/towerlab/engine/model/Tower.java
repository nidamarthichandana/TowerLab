package com.towerlab.engine.model;

public class Tower {
    private final int id;
    private final TowerType type;
    private final int x;
    private final int y;
    private TargetingMode targetingMode;
    private int cooldownTicks;

    // Analytics counters
    private int totalDamageDealt;
    private int totalKills;
    private int activeFiringTicks;

    public Tower(int id, TowerType type, int x, int y, TargetingMode targetingMode) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.targetingMode = targetingMode != null ? targetingMode : TargetingMode.FIRST;
        this.cooldownTicks = 0;
        this.totalDamageDealt = 0;
        this.totalKills = 0;
        this.activeFiringTicks = 0;
    }

    public boolean canFire() {
        return cooldownTicks <= 0;
    }

    public void tickCooldown() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    public void fire(int damage, boolean killedTarget) {
        this.cooldownTicks = type.getFireRateTicks();
        this.totalDamageDealt += damage;
        if (killedTarget) {
            this.totalKills++;
        }
        this.activeFiringTicks++;
    }

    public double distanceTo(double targetX, double targetY) {
        return Math.hypot(this.x - targetX, this.y - targetY);
    }

    public boolean isInRange(double targetX, double targetY) {
        return distanceTo(targetX, targetY) <= type.getRange();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public TowerType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TargetingMode getTargetingMode() {
        return targetingMode;
    }

    public void setTargetingMode(TargetingMode targetingMode) {
        this.targetingMode = targetingMode;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getTotalDamageDealt() {
        return totalDamageDealt;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getActiveFiringTicks() {
        return activeFiringTicks;
    }
}

