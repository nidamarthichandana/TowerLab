package com.towerlab.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "simulation_runs")
public class SimulationRunEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "level_id", length = 64, nullable = false)
    private String levelId;

    @Column(name = "seed", nullable = false)
    private long seed;

    @Column(name = "outcome", length = 16, nullable = false)
    private String outcome;

    @Column(name = "base_health", nullable = false)
    private int baseHealth;

    @Column(name = "gold", nullable = false)
    private int gold;

    @Column(name = "total_ticks", nullable = false)
    private int totalTicks;

    @Column(name = "actions_json", columnDefinition = "TEXT", nullable = false)
    private String actionsJson;

    @Column(name = "events_json", columnDefinition = "TEXT", nullable = false)
    private String eventsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SimulationRunEntity() {}

    public SimulationRunEntity(String id, String levelId, long seed, String outcome,
                               int baseHealth, int gold, int totalTicks,
                               String actionsJson, String eventsJson) {
        this.id = id;
        this.levelId = levelId;
        this.seed = seed;
        this.outcome = outcome;
        this.baseHealth = baseHealth;
        this.gold = gold;
        this.totalTicks = totalTicks;
        this.actionsJson = actionsJson;
        this.eventsJson = eventsJson;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public int getBaseHealth() { return baseHealth; }
    public void setBaseHealth(int baseHealth) { this.baseHealth = baseHealth; }

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }

    public int getTotalTicks() { return totalTicks; }
    public void setTotalTicks(int totalTicks) { this.totalTicks = totalTicks; }

    public String getActionsJson() { return actionsJson; }
    public void setActionsJson(String actionsJson) { this.actionsJson = actionsJson; }

    public String getEventsJson() { return eventsJson; }
    public void setEventsJson(String eventsJson) { this.eventsJson = eventsJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

