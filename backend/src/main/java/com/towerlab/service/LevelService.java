package com.towerlab.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towerlab.dto.LevelDetailDto;
import com.towerlab.dto.LevelSummaryDto;
import com.towerlab.engine.model.CreepType;
import com.towerlab.engine.model.LevelConfig;
import com.towerlab.engine.model.PathWaypoint;
import com.towerlab.engine.model.WaveDefinition;
import com.towerlab.entity.LevelEntity;
import com.towerlab.repository.LevelRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @PostConstruct
    @Transactional
    public void initDefaultLevels() {
        if (levelRepository.count() == 0) {
            seedDefaultLevels();
        }
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

    private void seedDefaultLevels() {
        try {
            // Level 1: The Serpentine Pass (12x8)
            List<PathWaypoint> level1Waypoints = List.of(
                    new PathWaypoint(0, 3),
                    new PathWaypoint(4, 3),
                    new PathWaypoint(4, 1),
                    new PathWaypoint(8, 1),
                    new PathWaypoint(8, 6),
                    new PathWaypoint(11, 6)
            );

            List<WaveDefinition> level1Waves = List.of(
                    new WaveDefinition(1, 20, List.of(
                            new WaveDefinition.CreepSpawnSchedule(CreepType.STANDARD, 6, 15)
                    )),
                    new WaveDefinition(2, 35, List.of(
                            new WaveDefinition.CreepSpawnSchedule(CreepType.STANDARD, 8, 12),
                            new WaveDefinition.CreepSpawnSchedule(CreepType.RUNNER, 4, 10)
                    )),
                    new WaveDefinition(3, 40, List.of(
                            new WaveDefinition.CreepSpawnSchedule(CreepType.TANK, 3, 25),
                            new WaveDefinition.CreepSpawnSchedule(CreepType.RUNNER, 6, 8),
                            new WaveDefinition.CreepSpawnSchedule(CreepType.BOSS, 1, 50)
                    ))
            );

            LevelEntity level1 = new LevelEntity(
                    "level-1",
                    "The Serpentine Pass",
                    "Standard winding canyon path suitable for Cannon chokepoints and Frost slowing fields.",
                    12, 8,
                    100, 350,
                    objectMapper.writeValueAsString(level1Waypoints),
                    objectMapper.writeValueAsString(level1Waves)
            );
            levelRepository.save(level1);

            // Level 2: The Double Horseshoe (12x8)
            List<PathWaypoint> level2Waypoints = List.of(
                    new PathWaypoint(0, 1),
                    new PathWaypoint(10, 1),
                    new PathWaypoint(10, 4),
                    new PathWaypoint(2, 4),
                    new PathWaypoint(2, 6),
                    new PathWaypoint(11, 6)
            );

            List<WaveDefinition> level2Waves = List.of(
                    new WaveDefinition(1, 15, List.of(
                            new WaveDefinition.CreepSpawnSchedule(CreepType.STANDARD, 8, 12)
                    )),
                    new WaveDefinition(2, 30, List.of(
                            new WaveDefinition.CreepSpawnSchedule(CreepType.RUNNER, 8, 8),
                            new WaveDefinition.CreepSpawnSchedule(CreepType.TANK, 2, 20)
                    )),
                    new WaveDefinition(3, 40, List.of(
                            new WaveDefinition.CreepSpawnSchedule(CreepType.TANK, 5, 18),
                            new WaveDefinition.CreepSpawnSchedule(CreepType.BOSS, 2, 40)
                    ))
            );

            LevelEntity level2 = new LevelEntity(
                    "level-2",
                    "The Double Horseshoe",
                    "A longer, switchback path with two tight loops ideal for high-range Sniper towers.",
                    12, 8,
                    100, 400,
                    objectMapper.writeValueAsString(level2Waypoints),
                    objectMapper.writeValueAsString(level2Waves)
            );
            levelRepository.save(level2);

        } catch (Exception e) {
            throw new RuntimeException("Failed to seed default levels", e);
        }
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

