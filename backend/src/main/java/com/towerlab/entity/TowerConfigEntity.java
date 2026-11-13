package com.towerlab.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tower_configs")
public class TowerConfigEntity {

    @Id
    @Column(name = "tower_type", length = 32, nullable = false)
    private String towerType;

    @Column(name = "cost", nullable = false)
    private int cost;

    @Column(name = "attack_range", nullable = false)
    private double attackRange;

    @Column(name = "fire_rate_ticks", nullable = false)
    private int fireRateTicks;

    @Column(name = "base_damage", nullable = false)
    private int baseDamage;

    @Column(name = "slow_factor", nullable = false)
    private double slowFactor;

    @Column(name = "slow_duration_ticks", nullable = false)
    private int slowDurationTicks;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public TowerConfigEntity() {}

    public TowerConfigEntity(String towerType, int cost, double attackRange, int fireRateTicks,
                             int baseDamage, double slowFactor, int slowDurationTicks, String description) {
        this.towerType = towerType;
        this.cost = cost;
        this.attackRange = attackRange;
        this.fireRateTicks = fireRateTicks;
        this.baseDamage = baseDamage;
        this.slowFactor = slowFactor;
        this.slowDurationTicks = slowDurationTicks;
        this.description = description;
    }

    public String getTowerType() { return towerType; }
    public void setTowerType(String towerType) { this.towerType = towerType; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double attackRange) { this.attackRange = attackRange; }

    public int getFireRateTicks() { return fireRateTicks; }
    public void setFireRateTicks(int fireRateTicks) { this.fireRateTicks = fireRateTicks; }

    public int getBaseDamage() { return baseDamage; }
    public void setBaseDamage(int baseDamage) { this.baseDamage = baseDamage; }

    public double getSlowFactor() { return slowFactor; }
    public void setSlowFactor(double slowFactor) { this.slowFactor = slowFactor; }

    public int getSlowDurationTicks() { return slowDurationTicks; }
    public void setSlowDurationTicks(int slowDurationTicks) { this.slowDurationTicks = slowDurationTicks; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

