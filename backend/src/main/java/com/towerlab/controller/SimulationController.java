package com.towerlab.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towerlab.dto.BatchSimulationRequest;
import com.towerlab.dto.BatchSimulationResponse;
import com.towerlab.dto.PlaySimulationRequest;
import com.towerlab.dto.PlaySimulationResponse;
import com.towerlab.engine.events.SimulationEvent;
import com.towerlab.entity.SimulationRunEntity;
import com.towerlab.service.AnalyticsService;
import com.towerlab.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationService simulationService;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public SimulationController(SimulationService simulationService,
                                AnalyticsService analyticsService,
                                ObjectMapper objectMapper) {
        this.simulationService = simulationService;
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/play")
    public ResponseEntity<PlaySimulationResponse> play(@Valid @RequestBody PlaySimulationRequest request) {
        PlaySimulationResponse response = simulationService.play(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchSimulationResponse> runBatch(@Valid @RequestBody BatchSimulationRequest request) {
        BatchSimulationResponse response = analyticsService.runBatchExperiment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaySimulationResponse> getSimulationById(@PathVariable String id) {
        SimulationRunEntity entity = simulationService.getSimulationRun(id);
        List<SimulationEvent> events;
        try {
            events = objectMapper.readValue(entity.getEventsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            events = List.of();
        }

        PlaySimulationResponse response = new PlaySimulationResponse(
                entity.getId(),
                entity.getLevelId(),
                entity.getSeed(),
                entity.getOutcome(),
                entity.getBaseHealth(),
                entity.getGold(),
                entity.getTotalTicks(),
                events
        );
        return ResponseEntity.ok(response);
    }
}

