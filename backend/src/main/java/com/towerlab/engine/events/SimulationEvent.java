package com.towerlab.engine.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulationEvent {
    private int tick;
    private EventType type;

    // Tower properties
    private Integer towerId;
    private String towerType;
    private Integer x;
    private Integer y;
    private String targetingMode;

    // Creep properties
    private Integer creepId;
    private String creepType;
    private Double creepX;
    private Double creepY;
    private Double health;
    private Double remainingHealth;

    // Combat & Status
    private Integer damage;
    private Double slowFactor;
    private Integer slowDuration;

    // Economy & Base
    private Integer goldRemaining;
    private Integer goldBounty;
    private Integer baseHealth;

    // Wave & General
    private Integer waveNumber;
    private String outcome;
    private String message;

    private Map<String, Object> details;

    public SimulationEvent() {}

    public SimulationEvent(int tick, EventType type) {
        this.tick = tick;
        this.type = type;
    }

    public static Builder builder(int tick, EventType type) {
        return new Builder(tick, type);
    }

    public static class Builder {
        private final SimulationEvent event;

        public Builder(int tick, EventType type) {
            this.event = new SimulationEvent(tick, type);
        }

        public Builder tower(int id, String type, int x, int y, String targetingMode) {
            event.towerId = id;
            event.towerType = type;
            event.x = x;
            event.y = y;
            event.targetingMode = targetingMode;
            return this;
        }

        public Builder towerId(int id) {
            event.towerId = id;
            return this;
        }

        public Builder creep(int id, String type, double x, double y, double health) {
            event.creepId = id;
            event.creepType = type;
            event.creepX = x;
            event.creepY = y;
            event.health = health;
            return this;
        }

        public Builder creepId(int id) {
            event.creepId = id;
            return this;
        }

        public Builder combat(int damage, double remainingHealth) {
            event.damage = damage;
            event.remainingHealth = remainingHealth;
            return this;
        }

        public Builder damage(int damage) {
            event.damage = damage;
            return this;
        }

        public Builder slow(double factor, int duration) {
            event.slowFactor = factor;
            event.slowDuration = duration;
            return this;
        }

        public Builder gold(int goldRemaining) {
            event.goldRemaining = goldRemaining;
            return this;
        }

        public Builder bounty(int goldBounty, int goldRemaining) {
            event.goldBounty = goldBounty;
            event.goldRemaining = goldRemaining;
            return this;
        }

        public Builder baseHealth(int baseHealth) {
            event.baseHealth = baseHealth;
            return this;
        }

        public Builder wave(int waveNumber) {
            event.waveNumber = waveNumber;
            return this;
        }

        public Builder outcome(String outcome) {
            event.outcome = outcome;
            return this;
        }

        public Builder message(String message) {
            event.message = message;
            return this;
        }

        public Builder detail(String key, Object value) {
            if (event.details == null) {
                event.details = new HashMap<>();
            }
            event.details.put(key, value);
            return this;
        }

        public SimulationEvent build() {
            return event;
        }
    }

    // Getters and Setters
    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public Integer getTowerId() { return towerId; }
    public void setTowerId(Integer towerId) { this.towerId = towerId; }

    public String getTowerType() { return towerType; }
    public void setTowerType(String towerType) { this.towerType = towerType; }

    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }

    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }

    public String getTargetingMode() { return targetingMode; }
    public void setTargetingMode(String targetingMode) { this.targetingMode = targetingMode; }

    public Integer getCreepId() { return creepId; }
    public void setCreepId(Integer creepId) { this.creepId = creepId; }

    public String getCreepType() { return creepType; }
    public void setCreepType(String creepType) { this.creepType = creepType; }

    public Double getCreepX() { return creepX; }
    public void setCreepX(Double creepX) { this.creepX = creepX; }

    public Double getCreepY() { return creepY; }
    public void setCreepY(Double creepY) { this.creepY = creepY; }

    public Double getHealth() { return health; }
    public void setHealth(Double health) { this.health = health; }

    public Double getRemainingHealth() { return remainingHealth; }
    public void setRemainingHealth(Double remainingHealth) { this.remainingHealth = remainingHealth; }

    public Integer getDamage() { return damage; }
    public void setDamage(Integer damage) { this.damage = damage; }

    public Double getSlowFactor() { return slowFactor; }
    public void setSlowFactor(Double slowFactor) { this.slowFactor = slowFactor; }

    public Integer getSlowDuration() { return slowDuration; }
    public void setSlowDuration(Integer slowDuration) { this.slowDuration = slowDuration; }

    public Integer getGoldRemaining() { return goldRemaining; }
    public void setGoldRemaining(Integer goldRemaining) { this.goldRemaining = goldRemaining; }

    public Integer getGoldBounty() { return goldBounty; }
    public void setGoldBounty(Integer goldBounty) { this.goldBounty = goldBounty; }

    public Integer getBaseHealth() { return baseHealth; }
    public void setBaseHealth(Integer baseHealth) { this.baseHealth = baseHealth; }

    public Integer getWaveNumber() { return waveNumber; }
    public void setWaveNumber(Integer waveNumber) { this.waveNumber = waveNumber; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}

