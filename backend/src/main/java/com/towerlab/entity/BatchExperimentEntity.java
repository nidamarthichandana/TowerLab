package com.towerlab.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "batch_experiments")
public class BatchExperimentEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "level_id", length = 64, nullable = false)
    private String levelId;

    @Column(name = "total_runs", nullable = false)
    private int totalRuns;

    @Column(name = "win_rate", nullable = false)
    private double winRate;

    @Column(name = "avg_base_health", nullable = false)
    private double avgBaseHealth;

    @Column(name = "avg_ticks", nullable = false)
    private double avgTicks;

    @Column(name = "avg_gold", nullable = false)
    private double avgGold;

    @Column(name = "tower_metrics_json", columnDefinition = "TEXT", nullable = false)
    private String towerMetricsJson;

    @Column(name = "actions_json", columnDefinition = "TEXT", nullable = false)
    private String actionsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BatchExperimentEntity() {}

    public BatchExperimentEntity(String id, String levelId, int totalRuns, double winRate,
                                 double avgBaseHealth, double avgTicks, double avgGold,
                                 String towerMetricsJson, String actionsJson) {
        this.id = id;
        this.levelId = levelId;
        this.totalRuns = totalRuns;
        this.winRate = winRate;
        this.avgBaseHealth = avgBaseHealth;
        this.avgTicks = avgTicks;
        this.avgGold = avgGold;
        this.towerMetricsJson = towerMetricsJson;
        this.actionsJson = actionsJson;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public double getAvgBaseHealth() { return avgBaseHealth; }
    public void setAvgBaseHealth(double avgBaseHealth) { this.avgBaseHealth = avgBaseHealth; }

    public double getAvgTicks() { return avgTicks; }
    public void setAvgTicks(double avgTicks) { this.avgTicks = avgTicks; }

    public double getAvgGold() { return avgGold; }
    public void setAvgGold(double avgGold) { this.avgGold = avgGold; }

    public String getTowerMetricsJson() { return towerMetricsJson; }
    public void setTowerMetricsJson(String towerMetricsJson) { this.towerMetricsJson = towerMetricsJson; }

    public String getActionsJson() { return actionsJson; }
    public void setActionsJson(String actionsJson) { this.actionsJson = actionsJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

