package com.towerlab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towerlab.analytics.BatchSimulationResult;
import com.towerlab.analytics.BatchSimulationRunner;
import com.towerlab.dto.BatchSimulationRequest;
import com.towerlab.dto.BatchSimulationResponse;
import com.towerlab.engine.model.LevelConfig;
import com.towerlab.engine.model.PlayerAction;
import com.towerlab.entity.BatchExperimentEntity;
import com.towerlab.repository.BatchExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AnalyticsService {

    private final LevelService levelService;
    private final BatchSimulationRunner batchRunner;
    private final BatchExperimentRepository experimentRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(LevelService levelService,
                            BatchSimulationRunner batchRunner,
                            BatchExperimentRepository experimentRepository,
                            ObjectMapper objectMapper) {
        this.levelService = levelService;
        this.batchRunner = batchRunner;
        this.experimentRepository = experimentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BatchSimulationResponse runBatchExperiment(BatchSimulationRequest request) {
        LevelConfig levelConfig = levelService.getLevelConfig(request.getLevelId());

        int runs = request.getRuns() != null ? request.getRuns() : 100;
        long baseSeed = request.getBaseSeed() != null ? request.getBaseSeed() : ThreadLocalRandom.current().nextLong(1, 1_000_000);
        List<PlayerAction> actions = request.getActions() != null ? request.getActions() : new ArrayList<>();

        BatchSimulationResult batchResult = batchRunner.runBatch(levelConfig, actions, runs, baseSeed);

        String experimentId = UUID.randomUUID().toString();
        String towerMetricsJson = serializeToJson(batchResult.getTowerMetrics());
        String actionsJson = serializeToJson(actions);

        BatchExperimentEntity entity = new BatchExperimentEntity(
                experimentId,
                levelConfig.getId(),
                batchResult.getTotalRuns(),
                batchResult.getWinRate(),
                batchResult.getAvgBaseHealth(),
                batchResult.getAvgTicks(),
                batchResult.getAvgGold(),
                towerMetricsJson,
                actionsJson
        );
        experimentRepository.save(entity);

        return new BatchSimulationResponse(
                experimentId,
                levelConfig.getId(),
                batchResult.getTotalRuns(),
                batchResult.getWins(),
                batchResult.getLosses(),
                batchResult.getWinRate(),
                batchResult.getAvgBaseHealth(),
                batchResult.getAvgTicks(),
                batchResult.getAvgGold(),
                batchResult.getTowerMetrics()
        );
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization failure", e);
        }
    }
}

