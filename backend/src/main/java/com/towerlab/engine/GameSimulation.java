package com.towerlab.engine;

import com.towerlab.engine.events.EventType;
import com.towerlab.engine.events.SimulationEvent;
import com.towerlab.engine.model.*;
import com.towerlab.engine.targeting.TargetingStrategy;
import com.towerlab.engine.targeting.TargetingStrategyFactory;

import java.util.*;

public class GameSimulation {
    public static final int MAX_TICKS_LIMIT = 10000;

    private final LevelConfig levelConfig;
    private final long seed;
    private final List<PlayerAction> plannedActions;
    private final Random random;

    private final Grid grid;
    private final List<PathWaypoint> waypoints;
    private final List<Tower> deployedTowers;
    private final List<Creep> activeCreeps;
    private final List<SimulationEvent> events;

    private int currentTick;
    private int baseHealth;
    private int gold;
    private SimulationOutcome outcome;
    private int nextTowerId = 1;
    private int nextCreepId = 1;

    // Wave management
    private final List<ScheduledSpawn> spawnSchedule;

    public GameSimulation(LevelConfig levelConfig, long seed, List<PlayerAction> actions) {
        this.levelConfig = levelConfig;
        this.seed = seed;
        this.plannedActions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
        this.random = new Random(seed);

        this.waypoints = levelConfig.getPathWaypoints();
        this.grid = new Grid(levelConfig.getGridWidth(), levelConfig.getGridHeight(), this.waypoints);
        this.deployedTowers = new ArrayList<>();
        this.activeCreeps = new ArrayList<>();
        this.events = new ArrayList<>();

        this.currentTick = 0;
        this.baseHealth = levelConfig.getInitialHealth();
        this.gold = levelConfig.getInitialGold();
        this.outcome = null;

        this.spawnSchedule = buildSpawnSchedule(levelConfig.getWaves());
    }

    private static class ScheduledSpawn {
        final int tick;
        final int waveNumber;
        final CreepType type;

        ScheduledSpawn(int tick, int waveNumber, CreepType type) {
            this.tick = tick;
            this.waveNumber = waveNumber;
            this.type = type;
        }
    }

    private List<ScheduledSpawn> buildSpawnSchedule(List<WaveDefinition> waves) {
        List<ScheduledSpawn> list = new ArrayList<>();
        if (waves == null) return list;

        int currentWaveStartTick = 0;
        for (WaveDefinition wave : waves) {
            currentWaveStartTick += wave.getDelayTicks();
            int waveNumber = wave.getWaveNumber();

            int creepOffset = 0;
            if (wave.getCreeps() != null) {
                for (WaveDefinition.CreepSpawnSchedule schedule : wave.getCreeps()) {
                    for (int i = 0; i < schedule.getCount(); i++) {
                        int spawnTick = currentWaveStartTick + creepOffset;
                        list.add(new ScheduledSpawn(spawnTick, waveNumber, schedule.getType()));
                        creepOffset += Math.max(1, schedule.getIntervalTicks());
                    }
                }
            }
            // Next wave starts after the last creep of this wave spawns + wave delay
            currentWaveStartTick += creepOffset;
        }

        list.sort(Comparator.comparingInt(s -> s.tick));
        return list;
    }

    public SimulationResult run() {
        // Group actions by tick for fast lookup
        Map<Integer, List<PlayerAction>> actionsByTick = new HashMap<>();
        for (PlayerAction action : plannedActions) {
            actionsByTick.computeIfAbsent(action.getTick(), k -> new ArrayList<>()).add(action);
        }

        // Map spawns by tick
        Map<Integer, List<ScheduledSpawn>> spawnsByTick = new HashMap<>();
        Set<Integer> waveStartTicks = new HashSet<>();
        for (ScheduledSpawn spawn : spawnSchedule) {
            spawnsByTick.computeIfAbsent(spawn.tick, k -> new ArrayList<>()).add(spawn);
        }

        int lastScheduledSpawnTick = spawnSchedule.isEmpty() ? 0 : spawnSchedule.get(spawnSchedule.size() - 1).tick;
        int activeWave = 0;

        while (currentTick <= MAX_TICKS_LIMIT) {
            // 1. Process player actions for this tick
            List<PlayerAction> actionsThisTick = actionsByTick.get(currentTick);
            if (actionsThisTick != null) {
                for (PlayerAction action : actionsThisTick) {
                    processPlayerAction(action);
                }
            }

            // 2. Wave spawns
            List<ScheduledSpawn> spawnsThisTick = spawnsByTick.get(currentTick);
            if (spawnsThisTick != null) {
                for (ScheduledSpawn spawn : spawnsThisTick) {
                    if (spawn.waveNumber > activeWave) {
                        activeWave = spawn.waveNumber;
                        events.add(SimulationEvent.builder(currentTick, EventType.WAVE_STARTED)
                                .wave(activeWave)
                                .build());
                    }

                    if (!waypoints.isEmpty()) {
                        PathWaypoint startWp = waypoints.get(0);
                        Creep creep = new Creep(nextCreepId++, spawn.type, currentTick, startWp);
                        activeCreeps.add(creep);

                        events.add(SimulationEvent.builder(currentTick, EventType.CREEP_SPAWNED)
                                .creep(creep.getId(), creep.getType().name(), creep.getX(), creep.getY(), creep.getCurrentHealth())
                                .build());
                    }
                }
            }

            // 3. Status effect updates on creeps
            for (Creep creep : activeCreeps) {
                creep.updateStatusEffects();
            }

            // 4. Creep movement
            List<Creep> leakedCreeps = new ArrayList<>();
            for (Creep creep : activeCreeps) {
                creep.advanceAlongPath(waypoints);
                if (creep.isLeaked()) {
                    leakedCreeps.add(creep);
                    int penalty = creep.getType().getBaseDamage();
                    baseHealth = Math.max(0, baseHealth - penalty);

                    events.add(SimulationEvent.builder(currentTick, EventType.CREEP_LEAKED)
                            .creepId(creep.getId())
                            .damage(penalty)
                            .build());

                    events.add(SimulationEvent.builder(currentTick, EventType.BASE_DAMAGED)
                            .baseHealth(baseHealth)
                            .damage(penalty)
                            .build());

                    if (baseHealth <= 0) {
                        outcome = SimulationOutcome.DEFEAT;
                        break;
                    }
                }
            }
            activeCreeps.removeAll(leakedCreeps);

            if (outcome == SimulationOutcome.DEFEAT) {
                events.add(SimulationEvent.builder(currentTick, EventType.SIMULATION_COMPLETED)
                        .outcome(outcome.name())
                        .baseHealth(baseHealth)
                        .gold(gold)
                        .build());
                break;
            }

            // 5. Tower combat
            for (Tower tower : deployedTowers) {
                tower.tickCooldown();

                if (tower.canFire()) {
                    List<Creep> inRangeCreeps = new ArrayList<>();
                    for (Creep creep : activeCreeps) {
                        if (creep.isAlive() && !creep.isLeaked() && tower.isInRange(creep.getX(), creep.getY())) {
                            inRangeCreeps.add(creep);
                        }
                    }

                    if (!inRangeCreeps.isEmpty()) {
                        TargetingStrategy strategy = TargetingStrategyFactory.getStrategy(tower.getTargetingMode());
                        Creep target = strategy.selectTarget(tower, inRangeCreeps);

                        if (target != null) {
                            int damage = tower.getType().getBaseDamage();

                            // Apply Frost slow if applicable
                            if (tower.getType() == TowerType.FROST && tower.getType().getSlowFactor() > 0) {
                                target.applySlow(tower.getType().getSlowFactor(), tower.getType().getSlowDurationTicks());
                                events.add(SimulationEvent.builder(currentTick, EventType.CREEP_SLOWED)
                                        .creepId(target.getId())
                                        .slow(tower.getType().getSlowFactor(), tower.getType().getSlowDurationTicks())
                                        .build());
                            }

                            target.takeDamage(damage);

                            events.add(SimulationEvent.builder(currentTick, EventType.TOWER_FIRED)
                                    .towerId(tower.getId())
                                    .creepId(target.getId())
                                    .damage(damage)
                                    .build());

                            events.add(SimulationEvent.builder(currentTick, EventType.CREEP_DAMAGED)
                                    .creepId(target.getId())
                                    .combat(damage, target.getCurrentHealth())
                                    .build());

                            boolean killed = !target.isAlive();
                            tower.fire(damage, killed);

                            if (killed) {
                                int bounty = target.getType().getGoldBounty();
                                gold += bounty;

                                events.add(SimulationEvent.builder(currentTick, EventType.CREEP_KILLED)
                                        .creepId(target.getId())
                                        .bounty(bounty, gold)
                                        .build());
                            }
                        }
                    }
                }
            }

            // Remove killed creeps
            activeCreeps.removeIf(c -> !c.isAlive());

            // 6. Check Victory Condition
            boolean allSpawnsCompleted = currentTick >= lastScheduledSpawnTick;
            if (allSpawnsCompleted && activeCreeps.isEmpty() && baseHealth > 0) {
                outcome = SimulationOutcome.VICTORY;
                events.add(SimulationEvent.builder(currentTick, EventType.SIMULATION_COMPLETED)
                        .outcome(outcome.name())
                        .baseHealth(baseHealth)
                        .gold(gold)
                        .build());
                break;
            }

            currentTick++;
        }

        // Safety fallback if tick limit reached
        if (outcome == null) {
            outcome = (baseHealth > 0) ? SimulationOutcome.VICTORY : SimulationOutcome.DEFEAT;
            events.add(SimulationEvent.builder(currentTick, EventType.SIMULATION_COMPLETED)
                    .outcome(outcome.name())
                    .baseHealth(baseHealth)
                    .gold(gold)
                    .build());
        }

        return new SimulationResult(outcome, baseHealth, gold, currentTick, events, deployedTowers);
    }

    private void processPlayerAction(PlayerAction action) {
        if (action.getType() == PlayerAction.ActionType.BUILD_TOWER) {
            TowerType type = action.getTowerType();
            if (type == null) {
                events.add(SimulationEvent.builder(currentTick, EventType.ACTION_REJECTED)
                        .message("Missing tower type in build action.")
                        .build());
                return;
            }

            int x = action.getX();
            int y = action.getY();

            if (!grid.canPlaceTower(x, y)) {
                String reason = !grid.isWithinBounds(x, y) ? "Placement out of bounds" :
                        grid.isPathTile(x, y) ? "Cannot build on path tile" : "Tile already occupied";
                events.add(SimulationEvent.builder(currentTick, EventType.ACTION_REJECTED)
                        .tower(-1, type.name(), x, y, null)
                        .message(reason)
                        .build());
                return;
            }

            if (gold < type.getCost()) {
                events.add(SimulationEvent.builder(currentTick, EventType.ACTION_REJECTED)
                        .tower(-1, type.name(), x, y, null)
                        .message("Insufficient gold to build " + type.name() + " (Required: " + type.getCost() + ", Current: " + gold + ")")
                        .build());
                return;
            }

            // Deduct gold and place tower
            gold -= type.getCost();
            Tower tower = new Tower(nextTowerId++, type, x, y, action.getTargetingMode());
            grid.placeTower(tower);
            deployedTowers.add(tower);

            events.add(SimulationEvent.builder(currentTick, EventType.TOWER_BUILT)
                    .tower(tower.getId(), type.name(), x, y, tower.getTargetingMode().name())
                    .gold(gold)
                    .build());

        } else if (action.getType() == PlayerAction.ActionType.CHANGE_TARGETING) {
            Tower tower = grid.getTowerAt(action.getX(), action.getY());
            if (tower != null && action.getTargetingMode() != null) {
                tower.setTargetingMode(action.getTargetingMode());
            }
        }
    }

    public static class SimulationResult {
        private final SimulationOutcome outcome;
        private final int baseHealth;
        private final int gold;
        private final int totalTicks;
        private final List<SimulationEvent> events;
        private final List<Tower> deployedTowers;

        public SimulationResult(SimulationOutcome outcome, int baseHealth, int gold, int totalTicks,
                                List<SimulationEvent> events, List<Tower> deployedTowers) {
            this.outcome = outcome;
            this.baseHealth = baseHealth;
            this.gold = gold;
            this.totalTicks = totalTicks;
            this.events = events;
            this.deployedTowers = deployedTowers;
        }

        public SimulationOutcome getOutcome() { return outcome; }
        public int getBaseHealth() { return baseHealth; }
        public int getGold() { return gold; }
        public int getTotalTicks() { return totalTicks; }
        public List<SimulationEvent> getEvents() { return events; }
        public List<Tower> getDeployedTowers() { return deployedTowers; }
    }
}

