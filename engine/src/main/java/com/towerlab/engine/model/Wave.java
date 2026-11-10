package com.towerlab.engine.model;

import com.towerlab.engine.config.SpawnGroupConfig;
import com.towerlab.engine.config.WaveConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable domain model representing a single wave of enemy spawn groups.
 * Wraps WaveConfig without copying configuration data into new duplicate fields.
 */
public final class Wave {

    private final WaveConfig config;
    private final List<SpawnGroup> groups;
    private final List<ScheduledSpawn> nominalSchedule;
    private final int totalEnemies;

    private record IndexedSpawn(ScheduledSpawn spawn, int groupIndex) {}

    public Wave(WaveConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("WaveConfig cannot be null");
        }
        this.config = config;

        List<SpawnGroup> spawnGroups = new ArrayList<>();
        int total = 0;
        List<IndexedSpawn> allSpawns = new ArrayList<>();

        List<SpawnGroupConfig> groupConfigs = config.spawnGroups();
        for (int groupIdx = 0; groupIdx < groupConfigs.size(); groupIdx++) {
            SpawnGroup group = new SpawnGroup(groupConfigs.get(groupIdx));
            spawnGroups.add(group);
            total += group.count();

            for (int tick : group.nominalSpawnTicks()) {
                allSpawns.add(new IndexedSpawn(new ScheduledSpawn(tick, group.enemyType()), groupIdx));
            }
        }

        // Sort by tick, ties broken by group order
        allSpawns.sort(Comparator
                .comparingInt((IndexedSpawn s) -> s.spawn().relativeTick())
                .thenComparingInt(IndexedSpawn::groupIndex));

        List<ScheduledSpawn> schedule = new ArrayList<>(allSpawns.size());
        for (IndexedSpawn is : allSpawns) {
            schedule.add(is.spawn());
        }

        this.groups = List.copyOf(spawnGroups);
        this.totalEnemies = total;
        this.nominalSchedule = List.copyOf(schedule);
    }

    public int waveNumber() {
        return config.waveNumber();
    }

    public List<SpawnGroup> groups() {
        return groups;
    }

    public WaveConfig config() {
        return config;
    }

    public int totalEnemies() {
        return totalEnemies;
    }

    public List<ScheduledSpawn> nominalSchedule() {
        return nominalSchedule;
    }
}
