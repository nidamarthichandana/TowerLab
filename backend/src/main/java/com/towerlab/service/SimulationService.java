package com.towerlab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towerlab.dto.PlaySimulationRequest;
import com.towerlab.dto.PlaySimulationResponse;
import com.towerlab.engine.GameSimulation;
import com.towerlab.engine.model.LevelConfig;
import com.towerlab.engine.model.PlayerAction;
import com.towerlab.entity.SimulationRunEntity;
import com.towerlab.repository.SimulationRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SimulationService {

    private final LevelService levelService;
    private final SimulationRunRepository simulationRunRepository;
    private final ObjectMapper objectMapper;

    public SimulationService(LevelService levelService,
                             SimulationRunRepository simulationRunRepository,
                             ObjectMapper objectMapper) {
        this.levelService = levelService;
        this.simulationRunRepository = simulationRunRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlaySimulationResponse play(PlaySimulationRequest request) {
        LevelConfig levelConfig = levelService.getLevelConfig(request.getLevelId());

        long seed = request.getSeed() != null ? request.getSeed() : ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        List<PlayerAction> actions = request.getActions() != null ? request.getActions() : new ArrayList<>();

        GameSimulation simulation = new GameSimulation(levelConfig, seed, actions);
        GameSimulation.SimulationResult result = simulation.run();

        String simulationId = UUID.randomUUID().toString();
        String actionsJson = serializeToJson(actions);
        String eventsJson = serializeToJson(result.getEvents());

        SimulationRunEntity runEntity = new SimulationRunEntity(
                simulationId,
                levelConfig.getId(),
                seed,
                result.getOutcome().name(),
                result.getBaseHealth(),
                result.getGold(),
                result.getTotalTicks(),
                actionsJson,
                eventsJson
        );
        simulationRunRepository.save(runEntity);

        return new PlaySimulationResponse(
                simulationId,
                levelConfig.getId(),
                seed,
                result.getOutcome().name(),
                result.getBaseHealth(),
                result.getGold(),
                result.getTotalTicks(),
                result.getEvents()
        );
    }

    public SimulationRunEntity getSimulationRun(String id) {
        return simulationRunRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Simulation run not found with ID: " + id));
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization failure", e);
        }
    }
}

