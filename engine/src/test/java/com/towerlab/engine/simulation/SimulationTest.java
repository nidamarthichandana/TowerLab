package com.towerlab.engine.simulation;

import com.towerlab.engine.config.ConfigLoader;
import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.config.SpawnGroupConfig;
import com.towerlab.engine.config.WaveConfig;
import com.towerlab.engine.event.ActionRejected;
import com.towerlab.engine.event.EnemyKilled;
import com.towerlab.engine.event.EnemySpawned;
import com.towerlab.engine.event.GameEnded;
import com.towerlab.engine.event.GameEvent;
import com.towerlab.engine.event.LifeLost;
import com.towerlab.engine.event.TowerBuilt;
import com.towerlab.engine.event.TowerFired;
import com.towerlab.engine.event.WaveCompleted;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.model.ScheduledSpawn;
import com.towerlab.engine.model.Wave;
import com.towerlab.engine.strategy.TargetingMode;
import com.towerlab.engine.test.TestConfigHelper;
import com.towerlab.engine.test.TestLevelHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTest {

    private GameConfig level1Config;

    @BeforeEach
    void setUp() {
        level1Config = TestConfigHelper.getFrozenGameConfig();
    }

    // ==========================================
    // A. Outcomes and termination
    // ==========================================

    @Test
    @DisplayName("1. Level 1 with no player actions ends in LOSS with base health 0")
    void testLevel1NoActionsGivesLossWithBaseHealthZero() {
        Simulation simulation = new Simulation();
        SimulationConfig config = new SimulationConfig(level1Config, 42L, List.of());

        SimulationResult result = simulation.run(config);

        assertNotNull(result);
        assertEquals(Outcome.LOSS, result.outcome());
        assertEquals(0, result.baseHealth());
        assertTrue(result.ticks() < level1Config.level().maxTicks(),
                "Simulation should stop early when base health reaches 0, elapsed: " + result.ticks());
        assertTrue(result.ticks() > 0);

        GameEvent lastEvent = result.events().get(result.events().size() - 1);
        assertInstanceOf(GameEnded.class, lastEvent);
        assertEquals("LOSS", ((GameEnded) lastEvent).outcome());
    }

    @Test
    @DisplayName("2. A tiny custom level can be won")
    void testTinyCustomLevelCanBeWon() {
        List<Position> waypoints = List.of(new Position(0, 1), new Position(6, 1));
        WaveConfig waveConfig = new WaveConfig(List.of(new SpawnGroupConfig("Basic", 3, 20, 0)));
        LevelConfig customLevel = TestLevelHelper.createLevel(
                "TinyLevel", 8, 4, waypoints, List.of(),
                List.of("Cannon", "Sniper", "Frost"),
                List.of(waveConfig),
                150, 20, 10, 20, 500, 1
        );
        GameConfig customGameConfig = new GameConfig(level1Config.towers(), level1Config.enemies(), customLevel);

        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 2, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Cannon", 4, 2, TargetingMode.FIRST)
        );

        Simulation simulation = new Simulation();
        SimulationResult result = simulation.run(new SimulationConfig(customGameConfig, 100L, actions));

        assertEquals(Outcome.WIN, result.outcome());
        assertTrue(result.baseHealth() > 0);

        List<GameEvent> events = result.events();
        assertTrue(events.size() >= 2);

        GameEvent secondToLast = events.get(events.size() - 2);
        assertInstanceOf(WaveCompleted.class, secondToLast);
        WaveCompleted wc = (WaveCompleted) secondToLast;
        assertEquals(1, wc.wave());
        assertEquals(20, wc.bonusGold());

        GameEvent last = events.get(events.size() - 1);
        assertInstanceOf(GameEnded.class, last);
        assertEquals("WIN", ((GameEnded) last).outcome());
    }

    @Test
    @DisplayName("3. With maxTicks = 50, simulation ends in LOSS with exactly ticks == 50")
    void testTimeoutWithMaxTicksFiftyEndsInLossWithTicksFifty() {
        LevelConfig baseLevel = level1Config.level();
        LevelConfig lowMaxTicksLevel = new LevelConfig(
                baseLevel.name(), baseLevel.width(), baseLevel.height(),
                baseLevel.waypoints(), baseLevel.segments(), baseLevel.allowedTowers(),
                baseLevel.waves(), baseLevel.startingGold(), baseLevel.startingBaseHealth(),
                baseLevel.prepPeriodTicks(), baseLevel.waveClearBonus(), 50, baseLevel.spawnJitterTicks()
        );
        GameConfig timeoutGameConfig = new GameConfig(level1Config.towers(), level1Config.enemies(), lowMaxTicksLevel);

        Simulation simulation = new Simulation();
        SimulationResult result = simulation.run(new SimulationConfig(timeoutGameConfig, 42L, List.of()));

        assertEquals(Outcome.LOSS, result.outcome());
        assertEquals(50, result.ticks(), "Ticks must equal maxTicks on timeout per GR-6");

        GameEvent lastEvent = result.events().get(result.events().size() - 1);
        assertInstanceOf(GameEnded.class, lastEvent);
        assertEquals("LOSS", ((GameEnded) lastEvent).outcome());
        assertEquals(50, lastEvent.tick());
    }

    @Test
    @DisplayName("4. If loss and win conditions would occur on the same tick, LOSS takes precedence")
    void testLossTakesPrecedenceOverWinOnSameTick() {
        // Base health starts at 1, path length 1 from (0, 1) to (1, 1)
        List<Position> waypoints = List.of(new Position(0, 1), new Position(1, 1));
        WaveConfig waveConfig = new WaveConfig(List.of(new SpawnGroupConfig("Basic", 1, 10, 0)));
        LevelConfig level = TestLevelHelper.createLevel(
                "SimultaneousLevel", 6, 4, waypoints, List.of(),
                List.of("Cannon"), List.of(waveConfig),
                150, 1, 10, 20, 100, 0
        );
        GameConfig gameConfig = new GameConfig(level1Config.towers(), level1Config.enemies(), level);

        // No towers built. The single enemy reaches base, drops baseHealth from 1 to 0,
        // and leaves the map empty on the very same tick.
        Simulation simulation = new Simulation();
        SimulationResult result = simulation.run(new SimulationConfig(gameConfig, 1L, List.of()));

        assertEquals(Outcome.LOSS, result.outcome(), "LOSS must take precedence over WIN when both occur on same tick");
        assertEquals(0, result.baseHealth());

        GameEvent lastEvent = result.events().get(result.events().size() - 1);
        assertInstanceOf(GameEnded.class, lastEvent);
        assertEquals("LOSS", ((GameEnded) lastEvent).outcome());

        boolean hasWaveCompleted = result.events().stream().anyMatch(e -> e instanceof WaveCompleted);
        assertFalse(hasWaveCompleted, "WaveCompleted must not be emitted when base health reaches 0");
    }

    // ==========================================
    // B. Determinism
    // ==========================================

    @Test
    @DisplayName("5. Two runs using the same configuration and seed produce identical results and event sequences")
    void testDeterministicTwoRunsSameConfigAndSeedProduceIdenticalResultsAndEvents() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST)
        );
        SimulationConfig config = new SimulationConfig(level1Config, 42L, actions);

        Simulation simulation = new Simulation();
        SimulationResult run1 = simulation.run(config);
        SimulationResult run2 = simulation.run(config);

        assertEquals(run1.outcome(), run2.outcome());
        assertEquals(run1.ticks(), run2.ticks());
        assertEquals(run1.baseHealth(), run2.baseHealth());
        assertEquals(run1.gold(), run2.gold());
        assertEquals(run1.events(), run2.events(), "Sequential runs on same Simulation instance must be identical");
    }

    @Test
    @DisplayName("6. Two separate Simulation instances using the same configuration and seed produce identical results and event sequences")
    void testDeterministicTwoSeparateSimulationInstancesProduceIdenticalResultsAndEvents() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST)
        );
        SimulationConfig config = new SimulationConfig(level1Config, 42L, actions);

        Simulation simInstance1 = new Simulation();
        SimulationResult res1 = simInstance1.run(config);

        Simulation simInstance2 = new Simulation();
        SimulationResult res2 = simInstance2.run(config);

        assertEquals(res1.outcome(), res2.outcome());
        assertEquals(res1.ticks(), res2.ticks());
        assertEquals(res1.baseHealth(), res2.baseHealth());
        assertEquals(res1.gold(), res2.gold());
        assertEquals(res1.events(), res2.events(), "Runs across separate Simulation instances must be identical");
    }

    @Test
    @DisplayName("7. Seeds 1 and 2 produce different simulation event sequences or results")
    void testSeedsOneAndTwoProduceDifferentSimulationResults() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST)
        );

        SimulationConfig configSeed1 = new SimulationConfig(level1Config, 1L, actions);
        SimulationConfig configSeed2 = new SimulationConfig(level1Config, 2L, actions);

        SimulationResult res1 = new Simulation().run(configSeed1);
        SimulationResult res2 = new Simulation().run(configSeed2);

        assertNotEquals(res1.events(), res2.events(),
                "Seeds 1 and 2 must produce observable differences in jitter / evasion event traces");
    }

    // ==========================================
    // C. Player actions
    // ==========================================

    @Test
    @DisplayName("8. A rejected build publishes an ActionRejected event, and the simulation continues")
    void testRejectedBuildPublishesActionRejectedAndSimulationContinues() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 0, 1, TargetingMode.FIRST), // on path!
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST)  // valid buildable cell
        );
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 1L, actions));

        boolean hasActionRejected = result.events().stream()
                .anyMatch(e -> e instanceof ActionRejected ar
                        && ar.actionType().equals("BuildTower")
                        && ar.reason().contains("not buildable"));
        assertTrue(hasActionRejected, "ActionRejected event must be published for invalid action");

        boolean hasTowerBuilt = result.events().stream()
                .anyMatch(e -> e instanceof TowerBuilt tb && tb.towerType().equals("Cannon") && tb.x() == 1 && tb.y() == 2);
        assertTrue(hasTowerBuilt, "Valid action following rejected action must still execute");
        assertEquals(Outcome.LOSS, result.outcome());
    }

    @Test
    @DisplayName("9. An action with a wave index outside the valid range is rejected before simulation execution begins")
    void testActionWithWaveIndexOutsideValidRangeRejectedBeforeSimulationBegins() {
        int totalWaves = level1Config.level().waves().size();

        assertThrows(InvalidConfigException.class, () ->
                new Simulation().run(new SimulationConfig(level1Config, 1L, List.of(
                        new BuildTowerAction(-1, "Cannon", 1, 2, TargetingMode.FIRST)
                ))));

        assertThrows(InvalidConfigException.class, () ->
                new Simulation().run(new SimulationConfig(level1Config, 1L, List.of(
                        new BuildTowerAction(totalWaves, "Cannon", 1, 2, TargetingMode.FIRST)
                ))));
    }

    // ==========================================
    // D. Spawn behaviour
    // ==========================================

    @Test
    @DisplayName("10. Every actual spawn tick lies within the nominal spawn tick ± J, where J is the configured spawn jitter")
    void testActualSpawnTickWithinNominalPlusMinusJitter() {
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 777L, List.of()));
        int J = level1Config.level().spawnJitterTicks();

        List<EnemySpawned> spawns = result.events().stream()
                .filter(e -> e instanceof EnemySpawned)
                .map(e -> (EnemySpawned) e)
                .toList();

        assertFalse(spawns.isEmpty());

        List<EnemySpawned> wave1Spawns = spawns.stream().filter(s -> s.wave() == 1).toList();
        List<ScheduledSpawn> nominalWave1 = new Wave(level1Config.level().waves().get(0)).nominalSchedule();
        assertEquals(nominalWave1.size(), wave1Spawns.size());

        for (EnemySpawned spawn : wave1Spawns) {
            assertTrue(nominalWave1.stream().anyMatch(ns ->
                    spawn.tick() >= Math.max(0, ns.relativeTick() - J)
                            && spawn.tick() <= ns.relativeTick() + J
            ), "Spawn tick " + spawn.tick() + " must lie within nominal +/- J of a scheduled spawn");
        }
    }

    @Test
    @DisplayName("11. Enemy IDs are assigned sequentially from 0 to n-1 in actual spawn order")
    void testEnemyIdsAssignedSequentiallyInActualSpawnOrder() {
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 888L, List.of()));

        List<EnemySpawned> spawns = result.events().stream()
                .filter(e -> e instanceof EnemySpawned)
                .map(e -> (EnemySpawned) e)
                .toList();

        assertFalse(spawns.isEmpty());

        for (int i = 0; i < spawns.size(); i++) {
            assertEquals(i, spawns.get(i).enemyId(), "Enemy ID must be sequential from 0 to n-1 in spawn order");
        }
    }

    // ==========================================
    // E. Event and tick invariants
    // ==========================================

    @Test
    @DisplayName("12. Event ticks never decrease")
    void testEventTicksNeverDecrease() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST),
                new BuildTowerAction(1, "Cannon", 5, 3, TargetingMode.STRONGEST)
        );
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 42L, actions));

        int prevTick = 0;
        for (GameEvent e : result.events()) {
            assertTrue(e.tick() >= prevTick, "Ticks must never decrease: " + e.tick() + " < " + prevTick);
            prevTick = e.tick();
        }
    }

    @Test
    @DisplayName("13. Exactly one GameEnded event exists, and it is the final event")
    void testExactlyOneGameEndedEventAndIsFinalEvent() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST)
        );
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 42L, actions));

        List<GameEnded> gameEndedEvents = result.events().stream()
                .filter(e -> e instanceof GameEnded)
                .map(e -> (GameEnded) e)
                .toList();

        assertEquals(1, gameEndedEvents.size(), "Exactly one GameEnded event must exist");
        assertEquals(result.events().get(result.events().size() - 1), gameEndedEvents.get(0),
                "GameEnded must be the very last event in the event log");
    }

    @Test
    @DisplayName("14. Every spawned enemy ends with exactly one terminal event: either EnemyKilled or LifeLost, never both or neither")
    void testEverySpawnedEnemyHasExactlyOneTerminalEvent() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST),
                new BuildTowerAction(1, "Cannon", 5, 3, TargetingMode.STRONGEST)
        );
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 42L, actions));

        List<GameEvent> events = result.events();
        List<EnemySpawned> spawned = events.stream().filter(e -> e instanceof EnemySpawned).map(e -> (EnemySpawned) e).toList();
        List<EnemyKilled> killed = events.stream().filter(e -> e instanceof EnemyKilled).map(e -> (EnemyKilled) e).toList();
        List<LifeLost> lifeLost = events.stream().filter(e -> e instanceof LifeLost).map(e -> (LifeLost) e).toList();

        Set<Integer> killedIds = killed.stream().map(EnemyKilled::enemyId).collect(Collectors.toSet());
        Set<Integer> lifeLostIds = lifeLost.stream().map(LifeLost::enemyId).collect(Collectors.toSet());

        assertEquals(killed.size(), killedIds.size(), "No enemy killed more than once");
        assertEquals(lifeLost.size(), lifeLostIds.size(), "No enemy causes life loss more than once");
        assertTrue(Collections.disjoint(killedIds, lifeLostIds), "Enemy cannot be both killed and cause life loss");

        Set<Integer> removedIds = new HashSet<>(killedIds);
        removedIds.addAll(lifeLostIds);
        Set<Integer> spawnedIds = spawned.stream().map(EnemySpawned::enemyId).collect(Collectors.toSet());
        assertEquals(spawnedIds, removedIds, "Every spawned enemy must end in either EnemyKilled or LifeLost, never neither");
    }

    @Test
    @DisplayName("15. baseHealthAfter never increases across events")
    void testBaseHealthAfterNeverIncreasesAcrossEvents() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST)
        );
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 42L, actions));

        int currentHealth = level1Config.level().startingBaseHealth();
        for (GameEvent e : result.events()) {
            if (e instanceof LifeLost ll) {
                assertTrue(ll.baseHealthAfter() <= currentHealth,
                        "baseHealthAfter must never increase: " + ll.baseHealthAfter() + " > " + currentHealth);
                currentHealth = ll.baseHealthAfter();
            }
        }
        assertEquals(result.baseHealth(), currentHealth, "Final base health must equal result baseHealth");
    }

    @Test
    @DisplayName("16. Gold accounting matches the final goldAfter value and SimulationResult.gold")
    void testGoldAccountingMatchesFinalGoldAfterAndSimulationResultGold() {
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST),
                new BuildTowerAction(1, "Cannon", 5, 3, TargetingMode.STRONGEST)
        );
        SimulationResult result = new Simulation().run(new SimulationConfig(level1Config, 42L, actions));

        List<GameEvent> events = result.events();
        List<EnemyKilled> killed = events.stream().filter(e -> e instanceof EnemyKilled).map(e -> (EnemyKilled) e).toList();

        int buildCosts = 60 + 50 + 60;
        int rewards = killed.stream().mapToInt(EnemyKilled::reward).sum();
        int bonuses = events.stream().filter(e -> e instanceof WaveCompleted).mapToInt(e -> ((WaveCompleted) e).bonusGold()).sum();
        int expectedGold = level1Config.level().startingGold() - buildCosts + rewards + bonuses;

        assertEquals(expectedGold, result.gold());

        int lastGoldAfter = -1;
        for (GameEvent e : events) {
            if (e instanceof TowerBuilt tb) lastGoldAfter = tb.goldAfter();
            else if (e instanceof EnemyKilled ek) lastGoldAfter = ek.goldAfter();
            else if (e instanceof WaveCompleted wc) lastGoldAfter = wc.goldAfter();
        }
        if (lastGoldAfter != -1) {
            assertEquals(expectedGold, lastGoldAfter);
        }
    }

    @Test
    @DisplayName("17. Gold increases by exactly waveClearBonus at each WaveCompleted event, accounting for other gold-changing events in the correct event order")
    void testGoldIncreasesByWaveClearBonusAtEachWaveCompleted() {
        List<Position> waypoints = List.of(new Position(0, 1), new Position(6, 1));
        WaveConfig wave1 = new WaveConfig(List.of(new SpawnGroupConfig("Basic", 1, 10, 0)));
        WaveConfig wave2 = new WaveConfig(List.of(new SpawnGroupConfig("Basic", 1, 10, 0)));
        LevelConfig customLevel = TestLevelHelper.createLevel(
                "BonusLevel", 8, 4, waypoints, List.of(),
                List.of("Cannon"), List.of(wave1, wave2),
                150, 20, 5, 25, 500, 1
        );
        GameConfig customConfig = new GameConfig(level1Config.towers(), level1Config.enemies(), customLevel);

        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 2, 2, TargetingMode.FIRST)
        );

        SimulationResult result = new Simulation().run(new SimulationConfig(customConfig, 10L, actions));

        int runningGold = customLevel.startingGold();
        int waveClearBonus = customLevel.waveClearBonus();

        for (GameEvent event : result.events()) {
            if (event instanceof TowerBuilt tb) {
                runningGold -= customConfig.towers().get(tb.towerType()).cost();
                assertEquals(runningGold, tb.goldAfter());
            } else if (event instanceof EnemyKilled ek) {
                runningGold += ek.reward();
                assertEquals(runningGold, ek.goldAfter());
            } else if (event instanceof WaveCompleted wc) {
                int goldBefore = runningGold;
                runningGold += wc.bonusGold();
                assertEquals(waveClearBonus, wc.bonusGold());
                assertEquals(goldBefore + waveClearBonus, wc.goldAfter());
                assertEquals(runningGold, wc.goldAfter());
            }
        }
        assertEquals(runningGold, result.gold());
    }

    @Test
    @DisplayName("Cannon with several enemies inside splash radius produces several TowerFired events on the same tick respecting enemy armour")
    void testCannonSplashProducesMultipleTowerFiredEventsRespectingArmour() {
        List<Position> waypoints = List.of(
                new Position(0, 1),
                new Position(10, 1)
        );
        // Wave with 1 Basic and 1 Armored spawning at tick 0
        WaveConfig wave = new WaveConfig(List.of(
                new SpawnGroupConfig("Basic", 1, 10, 0),
                new SpawnGroupConfig("Armored", 1, 10, 0)
        ));
        LevelConfig customLevel = TestLevelHelper.createLevel(
                "SplashTestLevel", 12, 4, waypoints, List.of(),
                List.of("Cannon"), List.of(wave),
                150, 20, 0, 20, 500, 0
        );
        GameConfig customConfig = new GameConfig(level1Config.towers(), level1Config.enemies(), customLevel);

        // Build Cannon at (2, 2). At x=2.0 along y=1, distance is 1.0 <= range 2.5
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 2, 2, TargetingMode.FIRST)
        );

        Simulation simulation = new Simulation();
        SimulationResult result = simulation.run(new SimulationConfig(customConfig, 42L, actions));

        // Group TowerFired events by tick
        Map<Integer, List<TowerFired>> firedByTick = result.events().stream()
                .filter(e -> e instanceof TowerFired)
                .map(e -> (TowerFired) e)
                .collect(Collectors.groupingBy(TowerFired::tick));

        // Find a tick where multiple TowerFired events occurred (splash hit)
        List<TowerFired> splashHits = firedByTick.values().stream()
                .filter(list -> list.size() > 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected at least one tick with multiple TowerFired events from Cannon splash"));

        assertTrue(splashHits.size() >= 2, "Cannon splash must produce several TowerFired events on the same tick");

        // Verify each damaged enemy's armour was respected:
        // Basic enemy (0% armour) takes full 12 damage
        // Armored enemy (40% armour) takes round(12 * (1 - 0.40)) = round(7.2) = 7 damage per GR-18
        boolean foundBasicDamage = splashHits.stream().anyMatch(h -> h.damageDealt() == 12);
        boolean foundArmoredDamage = splashHits.stream().anyMatch(h -> h.damageDealt() == 7);

        assertTrue(foundBasicDamage, "Basic enemy must take full 12 damage with 0% armour");
        assertTrue(foundArmoredDamage, "Armored enemy must take 7 damage with 40% armour per GR-18");
    }

    @Test
    @DisplayName("Action scheduled for wave index 1 that cannot be afforded produces ActionRejected during prep period and run continues")
    void testUnaffordableActionWave1ProducesActionRejectedAndRunContinues() {
        List<Position> waypoints = List.of(
                new Position(0, 1),
                new Position(10, 1)
        );
        WaveConfig wave1 = new WaveConfig(List.of(new SpawnGroupConfig("Basic", 1, 10, 0)));
        WaveConfig wave2 = new WaveConfig(List.of(new SpawnGroupConfig("Basic", 1, 10, 0)));

        // Starting gold 65.
        // Wave index 0 action: build Cannon (cost 60) -> 5 gold remaining.
        // Wave 1: kill Basic enemy gives 8 reward -> 13 gold.
        // Wave clear bonus 20 -> 33 gold.
        // Wave index 1 action: build Sniper (cost 90). Available 33 < 90 -> unaffordable!
        // Prep period ticks: 5. Run continues and completes.
        LevelConfig customLevel = TestLevelHelper.createLevel(
                "UnaffordableActionLevel", 12, 4, waypoints, List.of(),
                List.of("Cannon", "Sniper"), List.of(wave1, wave2),
                65, 20, 5, 20, 1000, 0
        );
        GameConfig customConfig = new GameConfig(level1Config.towers(), level1Config.enemies(), customLevel);

        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(1, "Sniper", 3, 2, TargetingMode.FIRST)
        );

        Simulation simulation = new Simulation();
        SimulationResult result = simulation.run(new SimulationConfig(customConfig, 42L, actions));

        // Find ActionRejected event
        List<ActionRejected> rejectedEvents = result.events().stream()
                .filter(e -> e instanceof ActionRejected)
                .map(e -> (ActionRejected) e)
                .toList();

        assertEquals(1, rejectedEvents.size(), "Exactly one ActionRejected event must be produced");
        ActionRejected rejected = rejectedEvents.get(0);
        assertEquals("BuildTower", rejected.actionType());
        assertTrue(rejected.reason().toLowerCase().contains("gold"),
                "Rejection reason should indicate insufficient gold: " + rejected.reason());

        // Verify rejection happened during prep period after wave 1 (at the wave completion tick)
        List<WaveCompleted> wave1Completed = result.events().stream()
                .filter(e -> e instanceof WaveCompleted wc && wc.wave() == 1)
                .map(e -> (WaveCompleted) e)
                .toList();
        assertFalse(wave1Completed.isEmpty(), "Wave 1 must complete");
        assertEquals(wave1Completed.get(0).tick(), rejected.tick(),
                "ActionRejected must occur at the wave 1 completion tick during the prep period");

        // Verify simulation continued and completed
        boolean gameEnded = result.events().stream().anyMatch(e -> e instanceof GameEnded);
        assertTrue(gameEnded, "Simulation run must continue to completion after ActionRejected");
        assertNotNull(result.outcome());
    }
}
