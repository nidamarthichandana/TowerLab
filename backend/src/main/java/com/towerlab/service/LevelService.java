package com.towerlab.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towerlab.dto.LevelDetailDto;
import com.towerlab.dto.LevelSummaryDto;
import com.towerlab.engine.model.LevelConfig;
import com.towerlab.engine.model.PathWaypoint;
import com.towerlab.engine.model.WaveDefinition;
import com.towerlab.entity.LevelEntity;
import com.towerlab.repository.LevelRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LevelService {

    private final LevelRepository levelRepository;
    private final ObjectMapper objectMapper;

    public LevelService(LevelRepository levelRepository, ObjectMapper objectMapper) {
        this.levelRepository = levelRepository;
        this.objectMapper = objectMapper;
    }

    public List<LevelSummaryDto> getAllLevels() {
        return levelRepository.findAll().stream().map(entity -> {
            List<WaveDefinition> waves = deserializeWaves(entity.getWavesConfigJson());
            return new LevelSummaryDto(
                    entity.getId(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.getGridWidth(),
                    entity.getGridHeight(),
                    entity.getInitialHealth(),
                    entity.getInitialGold(),
                    waves.size()
            );
        }).collect(Collectors.toList());
    }

    public LevelDetailDto getLevelDetail(String id) {
        LevelEntity entity = levelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Level not found with ID: " + id));

        List<PathWaypoint> waypoints = deserializeWaypoints(entity.getPathWaypointsJson());
        List<WaveDefinition> waves = deserializeWaves(entity.getWavesConfigJson());

        return new LevelDetailDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getGridWidth(),
                entity.getGridHeight(),
                entity.getInitialHealth(),
                entity.getInitialGold(),
                waypoints,
                waves
        );
    }

    public LevelConfig getLevelConfig(String id) {
        LevelEntity entity = levelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Level not found with ID: " + id));

        List<PathWaypoint> waypoints = deserializeWaypoints(entity.getPathWaypointsJson());
        List<WaveDefinition> waves = deserializeWaves(entity.getWavesConfigJson());

        return new LevelConfig(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getGridWidth(),
                entity.getGridHeight(),
                entity.getInitialHealth(),
                entity.getInitialGold(),
                waypoints,
                waves
        );
    }

    private List<PathWaypoint> deserializeWaypoints(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<WaveDefinition> deserializeWaves(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
