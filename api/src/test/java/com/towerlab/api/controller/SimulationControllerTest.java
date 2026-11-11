package com.towerlab.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/simulations/play executes simulation and returns serialized result")
    void testPlaySimulationSuccess() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "actions": [
                    {
                      "type": "BUILD_TOWER",
                      "wave": 0,
                      "towerType": "Cannon",
                      "x": 2,
                      "y": 2,
                      "targetingMode": "FIRST"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").exists())
                .andExpect(jsonPath("$.ticks", greaterThan(0)))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].type", equalTo("TowerBuilt")))
                .andExpect(jsonPath("$.events[0].tick", equalTo(0)))
                .andExpect(jsonPath("$.events[*].type", hasItem("GameEnded")));
    }

    @Test
    @DisplayName("POST /api/simulations/play is deterministic across identical seeds")
    void testPlaySimulationDeterminism() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 12345,
                  "actions": [
                    {
                      "type": "BUILD_TOWER",
                      "wave": 0,
                      "towerType": "Cannon",
                      "x": 3,
                      "y": 2,
                      "targetingMode": "FIRST"
                    }
                  ]
                }
                """;

        MvcResult result1 = mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(result1.getResponse().getContentAsString(), result2.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("POST /api/simulations/play with invalid cell returns 200 with ActionRejected event")
    void testPlaySimulationInvalidPlacementReturnsActionRejected() throws Exception {
        // (0, 1) is a path cell on level 1, so building a tower on it must be rejected in-game
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "actions": [
                    {
                      "type": "BUILD_TOWER",
                      "wave": 0,
                      "towerType": "Cannon",
                      "x": 0,
                      "y": 1,
                      "targetingMode": "FIRST"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[*].type", hasItem("ActionRejected")));
    }

    @Test
    @DisplayName("POST /api/simulations/play with invalid wave index returns 400")
    void testPlaySimulationInvalidWaveReturns400() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "actions": [
                    {
                      "type": "BUILD_TOWER",
                      "wave": 10,
                      "towerType": "Cannon",
                      "x": 2,
                      "y": 2,
                      "targetingMode": "FIRST"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("InvalidConfigException")));
    }

    @Test
    @DisplayName("POST /api/simulations/play with invalid targeting mode returns 400")
    void testPlaySimulationInvalidTargetingModeReturns400() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "actions": [
                    {
                      "type": "BUILD_TOWER",
                      "wave": 0,
                      "towerType": "Cannon",
                      "x": 2,
                      "y": 2,
                      "targetingMode": "MAGIC"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("IllegalArgument")));
    }

    @Test
    @DisplayName("POST /api/simulations/play with unsupported action type returns 400")
    void testPlaySimulationUnsupportedActionTypeReturns400() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "actions": [
                    {
                      "type": "SELL_TOWER",
                      "wave": 0,
                      "towerType": "Cannon",
                      "x": 2,
                      "y": 2,
                      "targetingMode": "FIRST"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("IllegalArgument")));
    }

    @Test
    @DisplayName("POST /api/simulations/play with unknown levelId returns 404")
    void testPlaySimulationUnknownLevelReturns404() throws Exception {
        String requestJson = """
                {
                  "levelId": "level_unknown",
                  "seed": 42,
                  "actions": []
                }
                """;

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("LevelNotFound")));
    }

    @Test
    @DisplayName("POST /api/simulations/batch executes multiple runs and returns aggregated metrics")
    void testBatchSimulationSuccess() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "runs": 10,
                  "actions": [
                    {
                      "type": "BUILD_TOWER",
                      "wave": 0,
                      "towerType": "Cannon",
                      "x": 2,
                      "y": 2,
                      "targetingMode": "FIRST"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runs", equalTo(10)))
                .andExpect(jsonPath("$.winRate").isNumber())
                .andExpect(jsonPath("$.averages").exists())
                .andExpect(jsonPath("$.towerStats").isArray());
    }

    @Test
    @DisplayName("POST /api/simulations/batch with runs=0 returns 400 validation error")
    void testBatchSimulationRunsZeroReturns400() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "runs": 0,
                  "actions": []
                }
                """;

        mockMvc.perform(post("/api/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("ValidationError")));
    }

    @Test
    @DisplayName("POST /api/simulations/batch with runs=1001 returns 400 validation error")
    void testBatchSimulationRunsTooLargeReturns400() throws Exception {
        String requestJson = """
                {
                  "levelId": "level1",
                  "seed": 42,
                  "runs": 1001,
                  "actions": []
                }
                """;

        mockMvc.perform(post("/api/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("ValidationError")));
    }

    @Test
    @DisplayName("Error response never leaks server stack traces")
    void testErrorResponseContainsNoStackTrace() throws Exception {
        String malformedJson = "{ invalidJson: true ";

        mockMvc.perform(post("/api/simulations/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("at com.towerlab"))));
    }
}
