package com.towerlab.dto;

import com.towerlab.engine.model.PlayerAction;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class BatchSimulationRequest {

    @NotBlank(message = "levelId must not be blank")
    private String levelId;

    private Integer runs;
    private Long baseSeed;
    private List<PlayerAction> actions;

    public BatchSimulationRequest() {}

    public BatchSimulationRequest(String levelId, Integer runs, Long baseSeed, List<PlayerAction> actions) {
        this.levelId = levelId;
        this.runs = runs;
        this.baseSeed = baseSeed;
        this.actions = actions;
    }

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    public Integer getRuns() { return runs; }
    public void setRuns(Integer runs) { this.runs = runs; }

    public Long getBaseSeed() { return baseSeed; }
    public void setBaseSeed(Long baseSeed) { this.baseSeed = baseSeed; }

    public List<PlayerAction> getActions() { return actions; }
    public void setActions(List<PlayerAction> actions) { this.actions = actions; }
}

