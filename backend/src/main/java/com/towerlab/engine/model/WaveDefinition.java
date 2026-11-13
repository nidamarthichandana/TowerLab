package com.towerlab.engine.model;

import java.util.ArrayList;
import java.util.List;

public class WaveDefinition {
    private int waveNumber;
    private int delayTicks;
    private List<CreepSpawnSchedule> creeps;

    public WaveDefinition() {
        this.creeps = new ArrayList<>();
    }

    public WaveDefinition(int waveNumber, int delayTicks, List<CreepSpawnSchedule> creeps) {
        this.waveNumber = waveNumber;
        this.delayTicks = delayTicks;
        this.creeps = creeps != null ? creeps : new ArrayList<>();
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber;
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = delayTicks;
    }

    public List<CreepSpawnSchedule> getCreeps() {
        return creeps;
    }

    public void setCreeps(List<CreepSpawnSchedule> creeps) {
        this.creeps = creeps;
    }

    public static class CreepSpawnSchedule {
        private CreepType type;
        private int count;
        private int intervalTicks;

        public CreepSpawnSchedule() {}

        public CreepSpawnSchedule(CreepType type, int count, int intervalTicks) {
            this.type = type;
            this.count = count;
            this.intervalTicks = intervalTicks;
        }

        public CreepType getType() {
            return type;
        }

        public void setType(CreepType type) {
            this.type = type;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getIntervalTicks() {
            return intervalTicks;
        }

        public void setIntervalTicks(int intervalTicks) {
            this.intervalTicks = intervalTicks;
        }
    }
}

