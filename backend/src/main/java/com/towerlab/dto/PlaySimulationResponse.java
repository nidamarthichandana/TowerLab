package com.towerlab.dto;

import com.towerlab.engine.events.SimulationEvent;

import java.util.List;

public class PlaySimulationResponse {
    private String id;
    private String levelId;
    private long seed;
    private String outcome;
    private int baseHealth;
    private int gold;
    private int ticks;
    private List<SimulationEvent> events;

    public PlaySimulationResponse() {}

    public PlaySimulationResponse(String id, String levelId, long seed, String outcome,
                                  int baseHealth, int gold, int ticks, List<SimulationEvent> events) {
        this.id = id;
        this.levelId = levelId;
        this.seed = seed;
        this.outcome = outcome;
        this.baseHealth = baseHealth;
        this.gold = gold;
        this.ticks = ticks;
        this.events = events;
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

    public int getTicks() { return ticks; }
    public void setTicks(int ticks) { this.ticks = ticks; }

    public List<SimulationEvent> getEvents() { return events; }
    public void setEvents(List<SimulationEvent> events) { this.events = events; }
}

