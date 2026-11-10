package com.towerlab.engine.model;

import com.towerlab.engine.config.SpawnGroupConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable domain representation of an enemy spawn group within a wave.
 * Wraps SpawnGroupConfig without copying its configuration data into new fields.
 */
public final class SpawnGroup {

    private final SpawnGroupConfig config;
    private final List<Integer> nominalSpawnTicks;

    public SpawnGroup(SpawnGroupConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("SpawnGroupConfig cannot be null");
        }
        this.config = config;
        List<Integer> ticks = new ArrayList<>(config.count());
        for (int i = 0; i < config.count(); i++) {
            ticks.add(config.startDelay() + i * config.interval());
        }
        this.nominalSpawnTicks = List.copyOf(ticks);
    }

    public String enemyType() {
        return config.enemyType();
    }

    public int count() {
        return config.count();
    }

    public int interval() {
        return config.interval();
    }

    public int startDelay() {
        return config.startDelay();
    }

    public SpawnGroupConfig config() {
        return config;
    }

    /**
     * Returns the nominal spawn ticks relative to wave start:
     * (startDelay + i * interval for i = 0..count-1).
     *
     * @return unmodifiable list of relative tick offsets
     */
    public List<Integer> nominalSpawnTicks() {
        return nominalSpawnTicks;
    }

    public List<Integer> nominalTicks() {
        return nominalSpawnTicks;
    }
}
