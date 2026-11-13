package com.towerlab.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "levels")
public class LevelEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "grid_width", nullable = false)
    private int gridWidth;

    @Column(name = "grid_height", nullable = false)
    private int gridHeight;

    @Column(name = "initial_health", nullable = false)
    private int initialHealth;

    @Column(name = "initial_gold", nullable = false)
    private int initialGold;

    @Column(name = "path_waypoints", columnDefinition = "TEXT", nullable = false)
    private String pathWaypointsJson;

    @Column(name = "waves_config", columnDefinition = "TEXT", nullable = false)
    private String wavesConfigJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LevelEntity() {}

    public LevelEntity(String id, String name, String description, int gridWidth, int gridHeight,
                       int initialHealth, int initialGold, String pathWaypointsJson, String wavesConfigJson) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.initialHealth = initialHealth;
        this.initialGold = initialGold;
        this.pathWaypointsJson = pathWaypointsJson;
        this.wavesConfigJson = wavesConfigJson;
        this.createdAt = Instant.now();
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

    public String getPathWaypointsJson() { return pathWaypointsJson; }
    public void setPathWaypointsJson(String pathWaypointsJson) { this.pathWaypointsJson = pathWaypointsJson; }

    public String getWavesConfigJson() { return wavesConfigJson; }
    public void setWavesConfigJson(String wavesConfigJson) { this.wavesConfigJson = wavesConfigJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

