package com.towerlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.towerlab.dto.BatchSimulationRequest;
import com.towerlab.dto.PlaySimulationRequest;
import com.towerlab.engine.model.PlayerAction;
import com.towerlab.engine.model.TargetingMode;
import com.towerlab.engine.model.TowerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/levels should return seeded levels")
    void testGetLevels() throws Exception {
        mockMvc.perform(get("/api/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("level-1"));
    }

    @Test
    @DisplayName("GET /api/levels/level-1 should return level details with waypoints")
    void testGetLevelDetail() throws Exception {
        mockMvc.perform(get("/api/levels/level-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("level-1"))
                .andExpect(jsonPath("$.pathWaypoints").isArray())
                .andExpect(jsonPath("$.waves").isArray());
    }

    @Test
    @DisplayName("POST /api/simulations/play should simulate game and return ordered event log")
    void testPlaySimulation() throws Exception {
        PlaySimulationRequest request = new PlaySimulationRequest(
                "level-1",
                12345L,
                List.of(
                        new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.CANNON, 2, 2, TargetingMode.FIRST),
                        new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.FROST, 3, 2, TargetingMode.CLOSEST)
                )
        );

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").exists())
                .andExpect(jsonPath("$.baseHealth").isNumber())
                .andExpect(jsonPath("$.gold").isNumber())
                .andExpect(jsonPath("$.ticks").isNumber())
                .andExpect(jsonPath("$.events").isArray());
    }

    @Test
    @DisplayName("POST /api/simulations/batch should calculate win rate and tower metrics")
    void testBatchSimulation() throws Exception {
        BatchSimulationRequest request = new BatchSimulationRequest(
                "level-1",
                15,
                9999L,
                List.of(
                        new PlayerAction(0, PlayerAction.ActionType.BUILD_TOWER, TowerType.CANNON, 2, 2, TargetingMode.FIRST)
                )
        );

        mockMvc.perform(post("/api/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRuns").value(15))
                .andExpect(jsonPath("$.winRate").isNumber())
                .andExpect(jsonPath("$.towerMetrics").isMap());
    }

    @Test
    @DisplayName("GET /api/tower-configs should return configs for Cannon, Sniper, Frost")
    void testGetTowerConfigs() throws Exception {
        mockMvc.perform(get("/api/tower-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].towerType").exists());
    }
}

