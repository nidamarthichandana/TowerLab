package com.towerlab.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/catalog returns MVP towers and enemies only")
    void testGetCatalogMvpOnly() throws Exception {
        mockMvc.perform(get("/api/catalog").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.towers", hasSize(3)))
                .andExpect(jsonPath("$.towers[*].type", hasItems("Cannon", "Sniper", "Frost")))
                .andExpect(jsonPath("$.towers[*].type", not(hasItems("Rapid"))))
                .andExpect(jsonPath("$.enemies", hasSize(3)))
                .andExpect(jsonPath("$.enemies[*].type", hasItems("Basic", "Fast", "Armored")))
                .andExpect(jsonPath("$.enemies[*].type", not(hasItems("Flying", "Boss"))));
    }

    @Test
    @DisplayName("GET /api/levels/level1 returns level metadata and path coordinates")
    void testGetLevel1Success() throws Exception {
        mockMvc.perform(get("/api/levels/level1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo("level1")))
                .andExpect(jsonPath("$.gridWidth", equalTo(12)))
                .andExpect(jsonPath("$.gridHeight", equalTo(8)))
                .andExpect(jsonPath("$.startGold", equalTo(150)))
                .andExpect(jsonPath("$.startBaseHealth", equalTo(20)))
                .andExpect(jsonPath("$.waveCount", equalTo(5)))
                .andExpect(jsonPath("$.path", hasSize(6)))
                .andExpect(jsonPath("$.path[0].x", equalTo(0)))
                .andExpect(jsonPath("$.path[0].y", equalTo(1)));
    }

    @Test
    @DisplayName("GET /api/levels/unknown returns 404")
    void testGetUnknownLevelReturns404() throws Exception {
        mockMvc.perform(get("/api/levels/unknown_level").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("LevelNotFound")));
    }
}
