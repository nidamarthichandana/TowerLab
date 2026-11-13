package com.towerlab.dto;

import com.towerlab.analytics.TowerMetrics;

import java.util.Map;

public class BatchSimulationResponse {
    private String experimentId;
    private String levelId;
    private int totalRuns;
    private int wins;
    private int losses;
    private double winRate;
    private double avgBaseHealth;
    private double avgTicks;
    private double avgGold;
    private Map<String, TowerMetrics> towerMetrics;

    public BatchSimulationResponse() {}

    public BatchSimulationResponse(String experimentId, String levelId, int totalRuns, int wins, int losses,
                                   double winRate, double avgBaseHealth, double avgTicks, double avgGold,
                                   Map<String, TowerMetrics> towerMetrics) {
        this.experimentId = experimentId;
        this.levelId = levelId;
        this.totalRuns = totalRuns;
        this.wins = wins;
        this.losses = losses;
        this.winRate = winRate;
        this.avgBaseHealth = avgBaseHealth;
        this.avgTicks = avgTicks;
        this.avgGold = avgGold;
        this.towerMetrics = towerMetrics;
    }

    public String getExperimentId() { return experimentId; }
    public void setExperimentId(String experimentId) { this.experimentId = experimentId; }

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public double getAvgBaseHealth() { return avgBaseHealth; }
    public void setAvgBaseHealth(double avgBaseHealth) { this.avgBaseHealth = avgBaseHealth; }

    public double getAvgTicks() { return avgTicks; }
    public void setAvgTicks(double avgTicks) { this.avgTicks = avgTicks; }

    public double getAvgGold() { return avgGold; }
    public void setAvgGold(double avgGold) { this.avgGold = avgGold; }

    public Map<String, TowerMetrics> getTowerMetrics() { return towerMetrics; }
    public void setTowerMetrics(Map<String, TowerMetrics> towerMetrics) { this.towerMetrics = towerMetrics; }
}

