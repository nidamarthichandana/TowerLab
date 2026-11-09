package com.towerlab.engine.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable configuration record for an enemy spawn group within a wave.
 *
 * @param enemyType  the referenced enemy type (must exist in enemies.json)
 * @param count      number of enemies to spawn (must be positive)
 * @param interval   interval in ticks between consecutive enemy spawns (must be positive)
 * @param startDelay delay in ticks before spawning the first enemy of this group (must be non-negative)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpawnGroupConfig(
        @JsonAlias({"enemyType", "type", "id"}) String enemyType,
        @JsonAlias({"count", "amount"}) int count,
        @JsonAlias({"intervalTicks", "interval"}) int interval,
        @JsonAlias({"startDelayTicks", "startDelay", "delay"}) int startDelay
) {
    public SpawnGroupConfig(String enemyType, int count, int interval) {
        this(enemyType, count, interval, 0);
    }
}
