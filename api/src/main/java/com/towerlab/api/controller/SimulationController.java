package com.towerlab.api.controller;

import com.towerlab.api.dto.SimulationBatchRequest;
import com.towerlab.api.dto.SimulationPlayRequest;
import com.towerlab.api.service.SimulationService;
import com.towerlab.engine.analytics.BatchResult;
import com.towerlab.engine.simulation.SimulationResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Controller exposing endpoints for single and batch simulation execution.
 */
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = Objects.requireNonNull(simulationService, "simulationService must not be null");
    }

    @PostMapping("/play")
    public ResponseEntity<SimulationResult> play(@Valid @RequestBody SimulationPlayRequest request) {
        return ResponseEntity.ok(simulationService.runPlay(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchResult> batch(@Valid @RequestBody SimulationBatchRequest request) {
        return ResponseEntity.ok(simulationService.runBatch(request));
    }
}
