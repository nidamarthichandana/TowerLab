package com.towerlab.dto;

public class LevelSummaryDto {
    private String id;
    private String name;
    private String description;
    private int gridWidth;
    private int gridHeight;
    private int initialHealth;
    private int initialGold;
    private int totalWaves;

    public LevelSummaryDto() {}

    public LevelSummaryDto(String id, String name, String description, int gridWidth, int gridHeight,
                           int initialHealth, int initialGold, int totalWaves) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.initialHealth = initialHealth;
        this.initialGold = initialGold;
        this.totalWaves = totalWaves;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getGridWidth() { return gridWidth; }
    public void setGridWidth(int gridWidth) { this.gridWidth = gridWidth; }

    public int getGridHeight() { return gridHeight; }
    public void setGridHeight(int gridHeight) { this.gridHeight = gridHeight; }

    public int getInitialHealth() { return initialHealth; }
    public void setInitialHealth(int initialHealth) { this.initialHealth = initialHealth; }

    public int getInitialGold() { return initialGold; }
    public void setInitialGold(int initialGold) { this.initialGold = initialGold; }

    public int getTotalWaves() { return totalWaves; }
    public void setTotalWaves(int totalWaves) { this.totalWaves = totalWaves; }
}

