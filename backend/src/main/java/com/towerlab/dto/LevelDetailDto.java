package com.towerlab.dto;

import com.towerlab.engine.model.PathWaypoint;
import com.towerlab.engine.model.WaveDefinition;

import java.util.List;

public class LevelDetailDto {
    private String id;
    private String name;
    private String description;
    private int gridWidth;
    private int gridHeight;
    private int initialHealth;
    private int initialGold;
    private List<PathWaypoint> pathWaypoints;
    private List<WaveDefinition> waves;

    public LevelDetailDto() {}

    public LevelDetailDto(String id, String name, String description, int gridWidth, int gridHeight,
                          int initialHealth, int initialGold, List<PathWaypoint> pathWaypoints, List<WaveDefinition> waves) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.initialHealth = initialHealth;
        this.initialGold = initialGold;
        this.pathWaypoints = pathWaypoints;
        this.waves = waves;
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

    public List<PathWaypoint> getPathWaypoints() { return pathWaypoints; }
    public void setPathWaypoints(List<PathWaypoint> pathWaypoints) { this.pathWaypoints = pathWaypoints; }

    public List<WaveDefinition> getWaves() { return waves; }
    public void setWaves(List<WaveDefinition> waves) { this.waves = waves; }
}

