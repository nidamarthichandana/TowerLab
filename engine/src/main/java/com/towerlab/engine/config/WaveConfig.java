package com.towerlab.engine.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Immutable configuration for a single wave of enemy spawn groups.
 *
 * @param waveNumber  the 1-based index or identifier of the wave
 * @param spawnGroups list of enemy spawn groups in this wave
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WaveConfig(
        @JsonAlias({"wave", "waveNumber", "id"}) int waveNumber,
        @JsonAlias({"spawnGroups", "spawns", "groups"}) List<SpawnGroupConfig> spawnGroups
) {
    public WaveConfig {
        spawnGroups = spawnGroups == null ? List.of() : List.copyOf(spawnGroups);
    }

    public WaveConfig(List<SpawnGroupConfig> spawnGroups) {
        this(1, spawnGroups);
    }
}
