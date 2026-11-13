package com.towerlab.dto;

import com.towerlab.engine.model.PlayerAction;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class PlaySimulationRequest {

    @NotBlank(message = "levelId must not be blank")
    private String levelId;

    private Long seed;

    private List<PlayerAction> actions;

    public PlaySimulationRequest() {}

    public PlaySimulationRequest(String levelId, Long seed, List<PlayerAction> actions) {
        this.levelId = levelId;
        this.seed = seed;
        this.actions = actions;
    }

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    public Long getSeed() { return seed; }
    public void setSeed(Long seed) { this.seed = seed; }

    public List<PlayerAction> getActions() { return actions; }
    public void setActions(List<PlayerAction> actions) { this.actions = actions; }
}

