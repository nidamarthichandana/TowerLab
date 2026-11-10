package com.towerlab.engine.simulation;

import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.config.WaveConfig;
import com.towerlab.engine.event.ActionRejected;
import com.towerlab.engine.event.EffectApplied;
import com.towerlab.engine.event.EnemyKilled;
import com.towerlab.engine.event.EnemyMoved;
import com.towerlab.engine.event.EnemySpawned;
import com.towerlab.engine.event.EventBus;
import com.towerlab.engine.event.EventLog;
import com.towerlab.engine.event.GameEnded;
import com.towerlab.engine.event.LifeLost;
import com.towerlab.engine.event.TowerFired;
import com.towerlab.engine.event.TowerMissed;
import com.towerlab.engine.event.WaveCompleted;
import com.towerlab.engine.event.WaveStarted;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.exception.TowerLabException;
import com.towerlab.engine.factory.EnemyFactory;
import com.towerlab.engine.factory.TowerFactory;
import com.towerlab.engine.model.AttackResult;
import com.towerlab.engine.model.ContinuousPosition;
import com.towerlab.engine.model.EffectDamage;
import com.towerlab.engine.model.Enemy;
import com.towerlab.engine.model.GameMap;
import com.towerlab.engine.model.GameState;
import com.towerlab.engine.model.GameStatus;
import com.towerlab.engine.model.HitResult;
import com.towerlab.engine.model.ScheduledSpawn;
import com.towerlab.engine.model.Tower;
import com.towerlab.engine.model.Wave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * Headless simulation engine executing deterministic game runs per GAME_RULES.md.
 */
public class Simulation {

    private record QueuedSpawn(
            int absoluteTick,
            int jitteredRelative,
            int originalIndex,
            String enemyType
    ) {}

    /**
     * Runs a complete simulation from configuration, returning the result and full event log.
     *
     * @param config the simulation configuration
     * @return immutable SimulationResult
     */
    public SimulationResult run(SimulationConfig config) {
        Objects.requireNonNull(config, "SimulationConfig must not be null");
        GameConfig gameConfig = config.gameConfig();
        LevelConfig levelConfig = gameConfig.level();

        // Validate action wave indices at run start
        int waveCount = levelConfig.waves().size();
        for (PlayerAction action : config.actions()) {
            if (action.wave() < 0 || action.wave() >= waveCount) {
                throw new InvalidConfigException(
                        "Action specifies invalid wave index: " + action.wave()
                                + ", must be in range [0, " + (waveCount - 1) + "]"
                );
            }
        }

        // Each run builds its own GameState, EventBus, EventLog, factories and ONE SplittableRandom
        GameMap map = new GameMap(levelConfig);
        GameState state = new GameState(map, levelConfig);
        EventBus eventBus = new EventBus();
        EventLog eventLog = new EventLog();
        eventBus.register(eventLog);

        EnemyFactory enemyFactory = new EnemyFactory(gameConfig.enemies());
        TowerFactory towerFactory = new TowerFactory(gameConfig.towers());
        RandomGenerator rng = new SplittableRandom(config.seed());

        // Group actions by wave index
        Map<Integer, List<PlayerAction>> actionsByWave = new HashMap<>();
        for (PlayerAction action : config.actions()) {
            actionsByWave.computeIfAbsent(action.wave(), k -> new ArrayList<>()).add(action);
        }

        List<Wave> waves = new ArrayList<>();
        for (WaveConfig wc : levelConfig.waves()) {
            waves.add(new Wave(wc));
        }
        int totalWaves = waves.size();

        // Tick 0: status READY. Execute actions with wave index 0 in list order, then start wave 1
        executeActions(actionsByWave.getOrDefault(0, List.of()), state, towerFactory, eventBus);

        List<QueuedSpawn> currentWaveSpawns = new ArrayList<>();
        startWave(1, state, waves, currentWaveSpawns, rng, levelConfig.spawnJitterTicks(), eventBus);

        int prepPeriodRemaining = 0;

        while (state.status() == GameStatus.PLAYING || state.status() == GameStatus.WAVE_COMPLETED) {
            if (state.status() == GameStatus.WAVE_COMPLETED) {
                prepPeriodRemaining--;
                if (prepPeriodRemaining <= 0) {
                    int nextWave = state.currentWaveNumber() + 1;
                    startWave(nextWave, state, waves, currentWaveSpawns, rng, levelConfig.spawnJitterTicks(), eventBus);
                } else {
                    state.advanceTick();
                    if (state.tick() >= levelConfig.maxTicks()) {
                        state.changeStatus(GameStatus.LOST);
                        eventBus.publish(new GameEnded(state.tick(), "LOSS"));
                        break;
                    }
                    continue;
                }
            }

            // Tick order while PLAYING per section 11
            // 1. Spawn due enemies
            spawnDueEnemies(state, currentWaveSpawns, enemyFactory, eventBus);

            // 2. Status effects
            Map<Integer, Integer> queuedKills = new HashMap<>();
            tickEffects(state, queuedKills);

            // 3. Move enemies
            moveEnemies(state, eventBus);

            // 4. Towers update
            updateTowers(state, rng, queuedKills, eventBus);

            // 5. Process queued kills
            processQueuedKills(state, queuedKills, eventBus);

            // 6. Check win/loss/completion and advance tick
            if (state.baseHealth() <= 0) {
                state.changeStatus(GameStatus.LOST);
                eventBus.publish(new GameEnded(state.tick(), "LOSS"));
                break;
            }

            if (currentWaveSpawns.isEmpty() && state.enemies().isEmpty()) {
                int clearedWave = state.currentWaveNumber();
                state.changeStatus(GameStatus.WAVE_COMPLETED);
                state.addGold(levelConfig.waveClearBonus());
                eventBus.publish(new WaveCompleted(state.tick(), clearedWave, levelConfig.waveClearBonus(), state.gold()));

                if (clearedWave == totalWaves) {
                    state.changeStatus(GameStatus.WON);
                    eventBus.publish(new GameEnded(state.tick(), "WIN"));
                    break;
                } else {
                    executeActions(actionsByWave.getOrDefault(clearedWave, List.of()), state, towerFactory, eventBus);
                    prepPeriodRemaining = levelConfig.prepPeriodTicks();
                }
            }

            state.advanceTick();
            if (state.tick() >= levelConfig.maxTicks()) {
                state.changeStatus(GameStatus.LOST);
                eventBus.publish(new GameEnded(state.tick(), "LOSS"));
                break;
            }
        }

        Outcome outcome = state.status() == GameStatus.WON ? Outcome.WIN : Outcome.LOSS;
        return new SimulationResult(outcome, state.baseHealth(), state.gold(), state.tick(), eventLog.events());
    }

    private void executeActions(
            List<PlayerAction> actions,
            GameState state,
            TowerFactory towerFactory,
            EventBus eventBus
    ) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (PlayerAction action : actions) {
            try {
                action.execute(state, towerFactory, eventBus, state.tick());
            } catch (TowerLabException e) {
                eventBus.publish(new ActionRejected(state.tick(), action.type(), e.getMessage()));
            }
        }
    }

    private void startWave(
            int waveNumber,
            GameState state,
            List<Wave> waves,
            List<QueuedSpawn> currentWaveSpawns,
            RandomGenerator rng,
            int spawnJitterTicks,
            EventBus eventBus
    ) {
        state.startNextWave();
        state.changeStatus(GameStatus.PLAYING);
        eventBus.publish(new WaveStarted(state.tick(), state.currentWaveNumber()));

        Wave wave = waves.get(waveNumber - 1);
        List<ScheduledSpawn> nominalSchedule = wave.nominalSchedule();

        List<QueuedSpawn> spawns = new ArrayList<>(nominalSchedule.size());
        for (int i = 0; i < nominalSchedule.size(); i++) {
            ScheduledSpawn ss = nominalSchedule.get(i);
            int offset = rng.nextInt(2 * spawnJitterTicks + 1) - spawnJitterTicks;
            int jitteredRelative = Math.max(0, ss.relativeTick() + offset);
            int absoluteTick = state.tick() + jitteredRelative;
            spawns.add(new QueuedSpawn(absoluteTick, jitteredRelative, i, ss.enemyType()));
        }

        // Sort by (jittered relative tick, original schedule index)
        spawns.sort(Comparator
                .comparingInt(QueuedSpawn::jitteredRelative)
                .thenComparingInt(QueuedSpawn::originalIndex));

        currentWaveSpawns.clear();
        currentWaveSpawns.addAll(spawns);
    }

    private void spawnDueEnemies(
            GameState state,
            List<QueuedSpawn> currentWaveSpawns,
            EnemyFactory enemyFactory,
            EventBus eventBus
    ) {
        while (!currentWaveSpawns.isEmpty() && currentWaveSpawns.get(0).absoluteTick() <= state.tick()) {
            QueuedSpawn spawn = currentWaveSpawns.remove(0);
            int enemyId = state.nextEnemyId();
            Enemy enemy = enemyFactory.create(spawn.enemyType(), enemyId);
            state.addEnemy(enemy);
            eventBus.publish(new EnemySpawned(state.tick(), enemyId, enemy.config().type(), state.currentWaveNumber()));
        }
    }

    private void tickEffects(GameState state, Map<Integer, Integer> queuedKills) {
        for (Enemy enemy : state.enemies()) {
            if (enemy.isAlive()) {
                List<EffectDamage> damages = enemy.tickEffects();
                if (!enemy.isAlive()) {
                    int killerTowerId = damages.isEmpty() ? -1 : damages.get(damages.size() - 1).sourceTowerId();
                    queuedKills.putIfAbsent(enemy.id(), killerTowerId);
                }
            }
        }
    }

    private void moveEnemies(GameState state, EventBus eventBus) {
        for (Enemy enemy : state.enemies()) {
            if (enemy.isAlive()) {
                enemy.advance(state.map().path());
                ContinuousPosition pos = enemy.positionOn(state.map().path());
                eventBus.publish(new EnemyMoved(state.tick(), enemy.id(), pos.x(), pos.y()));
                if (enemy.hasReachedEnd(state.map().path())) {
                    state.removeEnemy(enemy.id());
                    int damage = state.damageBase(enemy.config().baseDamage());
                    eventBus.publish(new LifeLost(state.tick(), enemy.id(), damage, state.baseHealth()));
                }
            }
        }
    }

    private void updateTowers(
            GameState state,
            RandomGenerator rng,
            Map<Integer, Integer> queuedKills,
            EventBus eventBus
    ) {
        for (Tower tower : state.towers()) {
            AttackResult result = tower.update(state.enemies(), state.map().path(), rng);
            if (result.fired()) {
                for (HitResult hit : result.hits()) {
                    if (hit.evaded()) {
                        eventBus.publish(new TowerMissed(state.tick(), tower.id(), hit.enemyId()));
                    } else {
                        eventBus.publish(new TowerFired(
                                state.tick(),
                                tower.id(),
                                tower.config().type(),
                                hit.enemyId(),
                                hit.damageDealt()
                        ));
                        if (hit.effectApplied()) {
                            eventBus.publish(new EffectApplied(
                                    state.tick(),
                                    hit.enemyId(),
                                    "Slow",
                                    tower.config().slowFraction(),
                                    tower.config().slowDurationTicks()
                            ));
                        }
                        if (hit.killed()) {
                            queuedKills.putIfAbsent(hit.enemyId(), tower.id());
                        }
                    }
                }
            }
        }
    }

    private void processQueuedKills(
            GameState state,
            Map<Integer, Integer> queuedKills,
            EventBus eventBus
    ) {
        if (queuedKills.isEmpty()) {
            return;
        }
        List<Integer> sortedEnemyIds = new ArrayList<>(queuedKills.keySet());
        Collections.sort(sortedEnemyIds);

        for (int enemyId : sortedEnemyIds) {
            Optional<Enemy> enemyOpt = state.enemy(enemyId);
            if (enemyOpt.isPresent()) {
                Enemy enemy = enemyOpt.get();
                int reward = enemy.config().reward();
                state.addGold(reward);
                int killerTowerId = queuedKills.get(enemyId);
                eventBus.publish(new EnemyKilled(
                        state.tick(),
                        enemyId,
                        enemy.config().type(),
                        killerTowerId,
                        reward,
                        state.gold()
                ));
                state.removeEnemy(enemyId);
            }
        }
    }
}
