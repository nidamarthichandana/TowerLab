package com.towerlab.analytics;

public class TowerMetrics {
    private String towerType;
    private int count;
    private long totalDamageDealt;
    private double damageSharePercentage;
    private int totalKills;
    private double killSharePercentage;
    private double costEfficiency;
    private double utilizationPercentage;

    public TowerMetrics() {}

    public TowerMetrics(String towerType, int count, long totalDamageDealt, double damageSharePercentage,
                        int totalKills, double killSharePercentage, double costEfficiency, double utilizationPercentage) {
        this.towerType = towerType;
        this.count = count;
        this.totalDamageDealt = totalDamageDealt;
        this.damageSharePercentage = damageSharePercentage;
        this.totalKills = totalKills;
        this.killSharePercentage = killSharePercentage;
        this.costEfficiency = costEfficiency;
        this.utilizationPercentage = utilizationPercentage;
    }

    public String getTowerType() { return towerType; }
    public void setTowerType(String towerType) { this.towerType = towerType; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public long getTotalDamageDealt() { return totalDamageDealt; }
    public void setTotalDamageDealt(long totalDamageDealt) { this.totalDamageDealt = totalDamageDealt; }

    public double getDamageSharePercentage() { return damageSharePercentage; }
    public void setDamageSharePercentage(double damageSharePercentage) { this.damageSharePercentage = damageSharePercentage; }

    public int getTotalKills() { return totalKills; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }

    public double getKillSharePercentage() { return killSharePercentage; }
    public void setKillSharePercentage(double killSharePercentage) { this.killSharePercentage = killSharePercentage; }

    public double getCostEfficiency() { return costEfficiency; }
    public void setCostEfficiency(double costEfficiency) { this.costEfficiency = costEfficiency; }

    public double getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
}

