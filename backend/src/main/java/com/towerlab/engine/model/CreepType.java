package com.towerlab.engine.model;

public enum CreepType {
    STANDARD(80, 0.10, 15, 10, "Balanced standard unit"),
    RUNNER(45, 0.18, 20, 5, "Fast, low HP scout"),
    TANK(260, 0.05, 40, 25, "Slow, heavily armored unit"),
    BOSS(750, 0.04, 100, 50, "Massive endgame boss");

    private final int baseHealth;
    private final double speed; // tiles per tick
    private final int goldBounty;
    private final int baseDamage;
    private final String description;

    CreepType(int baseHealth, double speed, int goldBounty, int baseDamage, String description) {
        this.baseHealth = baseHealth;
        this.speed = speed;
        this.goldBounty = goldBounty;
        this.baseDamage = baseDamage;
        this.description = description;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public double getSpeed() {
        return speed;
    }

    public int getGoldBounty() {
        return goldBounty;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public String getDescription() {
        return description;
    }
}

