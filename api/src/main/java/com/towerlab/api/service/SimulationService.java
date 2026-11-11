package com.towerlab.api.service;

import com.towerlab.api.dto.ActionDto;
import com.towerlab.api.dto.SimulationBatchRequest;
import com.towerlab.api.dto.SimulationPlayRequest;
import com.towerlab.api.error.LevelNotFoundException;
import com.towerlab.engine.analytics.BatchResult;
import com.towerlab.engine.analytics.BatchRunner;
import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.simulation.BuildTowerAction;
import com.towerlab.engine.simulation.PlayerAction;
import com.towerlab.engine.simulation.Simulation;
import com.towerlab.engine.simulation.SimulationResult;
import com.towerlab.engine.strategy.TargetingMode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service orchestrating single and batch simulation executions.
 */
@Service
public class SimulationService {

    private final GameConfig gameConfig;

    public SimulationService(GameConfig gameConfig) {
        this.gameConfig = Objects.requireNonNull(gameConfig, "gameConfig must not be null");
    }

    /**
     * Executes a single simulation run and returns the full result with events.
     *
     * @param request simulation play request parameters
     * @return SimulationResult
     */
    public SimulationResult runPlay(SimulationPlayRequest request) {
        validateLevelId(request.levelId());
        List<PlayerAction> actions = mapActions(request.actions());

        SimulationConfig config = new SimulationConfig(gameConfig, request.seed(), actions);
        Simulation simulation = new Simulation();
        return simulation.run(config);
    }

    /**
     * Executes a batch of simulation runs and returns aggregated statistical metrics.
     *
     * @param request simulation batch request parameters
     * @return BatchResult
     */
    public BatchResult runBatch(SimulationBatchRequest request) {
        validateLevelId(request.levelId());
        List<PlayerAction> actions = mapActions(request.actions());

        SimulationConfig template = new SimulationConfig(gameConfig, request.seed(), actions);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(1, Math.min(availableProcessors, 8));

        return BatchRunner.run(template, request.runs(), request.seed(), threads);
    }

    private void validateLevelId(String levelId) {
        if (!"level1".equals(levelId)) {
            throw new LevelNotFoundException("Level not found: " + levelId);
        }
    }

    private List<PlayerAction> mapActions(List<ActionDto> actionDtos) {
        if (actionDtos == null) {
            return List.of();
        }
        List<PlayerAction> actions = new ArrayList<>(actionDtos.size());
        for (ActionDto dto : actionDtos) {
            if (dto == null) {
                throw new IllegalArgumentException("Action item cannot be null");
            }
            if (dto.type() == null || !"BUILD_TOWER".equalsIgnoreCase(dto.type().trim())) {
                throw new IllegalArgumentException("Unsupported action type: " + dto.type());
            }
            if (dto.wave() == null) {
                throw new IllegalArgumentException("Action wave is required");
            }
            if (dto.towerType() == null || dto.towerType().isBlank()) {
                throw new IllegalArgumentException("Action towerType is required for BUILD_TOWER");
            }
            if (dto.x() == null || dto.y() == null) {
                throw new IllegalArgumentException("Action coordinates x and y are required");
            }
            if (dto.targetingMode() == null || dto.targetingMode().isBlank()) {
                throw new IllegalArgumentException("Action targetingMode is required for BUILD_TOWER");
            }
            TargetingMode mode = TargetingMode.fromString(dto.targetingMode());
            actions.add(new BuildTowerAction(dto.wave(), dto.towerType().trim(), dto.x(), dto.y(), mode));
        }
        return List.copyOf(actions);
    }
}
