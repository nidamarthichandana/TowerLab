package com.towerlab.engine.config;

import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.test.TestLevelHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    private ConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ConfigLoader();
    }

    @Test
    @DisplayName("Load valid configuration from classpath resources and verify GAME_RULES.md values")
    void testLoadValidConfigurationFromClasspath() {
        GameConfig config = loader.loadFromClasspath("");

        assertNotNull(config, "GameConfig must not be null");
        assertNotNull(config.towers(), "Towers map must not be null");
        assertNotNull(config.enemies(), "Enemies map must not be null");
        assertNotNull(config.level(), "LevelConfig must not be null");

        // Verify MVP towers
        assertTrue(config.towers().containsKey("Cannon"));
        assertTrue(config.towers().containsKey("Sniper"));
        assertTrue(config.towers().containsKey("Frost"));
        TowerConfig cannon = config.towers().get("Cannon");
        assertEquals("MVP", cannon.tier());
        assertTrue(cannon.isMvp());
        assertEquals(60, cannon.cost());
        assertEquals(2.5, cannon.range());
        assertEquals(12.0, cannon.damage());
        assertEquals(12, cannon.cooldown());
        assertEquals(40, cannon.upgradeCost());
        assertEquals(TargetType.GROUND, cannon.targets());

        TowerConfig sniper = config.towers().get("Sniper");
        assertEquals(TargetType.BOTH, sniper.targets());

        TowerConfig frost = config.towers().get("Frost");
        assertEquals(35, frost.upgradeCost(), "Frost upgrade cost must be 35 per GR-25");
        assertEquals(0.50, frost.slowFraction(), 1e-6);
        assertEquals(30, frost.slowDurationTicks());
        assertEquals(TargetType.BOTH, frost.targets());

        // Verify Stretch towers
        assertTrue(config.towers().containsKey("Rapid"));
        TowerConfig rapid = config.towers().get("Rapid");
        assertEquals("Stretch", rapid.tier());
        assertTrue(rapid.isStretch());

        // Verify MVP enemies
        assertTrue(config.enemies().containsKey("Basic"));
        assertTrue(config.enemies().containsKey("Fast"));
        assertTrue(config.enemies().containsKey("Armored"));
        EnemyConfig basic = config.enemies().get("Basic");
        assertEquals("MVP", basic.tier());
        assertTrue(basic.isMvp());
        assertEquals(40.0, basic.health());
        assertEquals(0.05, basic.speed());
        assertEquals(8, basic.reward());
        assertEquals(1, basic.baseDamage());

        // Verify Stretch enemies
        assertTrue(config.enemies().containsKey("Flying"));
        assertTrue(config.enemies().containsKey("Boss"));
        assertTrue(config.enemies().get("Flying").isStretch());
        assertTrue(config.enemies().get("Boss").isStretch());

        // Verify MVP level contains only MVP tower types
        assertEquals("Level 1", config.level().name());
        assertEquals(12, config.level().width());
        assertEquals(8, config.level().height());
        assertEquals(List.of("Cannon", "Sniper", "Frost"), config.level().allowedTowers());
        for (String towerType : config.level().allowedTowers()) {
            assertTrue(config.towers().get(towerType).isMvp(), "Level 1 must only allow MVP towers");
        }

        // Verify waypoints and waves match GR-8 & GR-28
        assertEquals(6, config.level().waypoints().size());
        assertEquals(5, config.level().waves().size());

        // Verify starting economy, base health, preparation period, bonus, max ticks, and jitter
        assertEquals(150, config.level().startingGold());
        assertEquals(20, config.level().startingBaseHealth());
        assertEquals(100, config.level().prepPeriodTicks());
        assertEquals(20, config.level().waveClearBonus());
        assertEquals(20000, config.level().maxTicks());
        assertEquals(3, config.level().spawnJitterTicks());
    }

    @Test
    @DisplayName("Load valid configuration from a directory with temporary test files")
    void testLoadValidConfigurationFromDirectory(@TempDir Path tempDir) throws IOException {
        String towersJson = """
                [
                  {"type": "Cannon", "tier": "MVP", "cost": 60, "range": 2.5, "damage": 12.0, "cooldown": 12}
                ]
                """;
        String enemiesJson = """
                [
                  {"type": "Basic", "tier": "MVP", "health": 40.0, "speed": 0.05, "reward": 8, "baseDamage": 1}
                ]
                """;
        String levelJson = """
                {
                  "name": "Test Level",
                  "width": 10,
                  "height": 10,
                  "startingGold": 150,
                  "startingBaseHealth": 20,
                  "prepPeriodTicks": 100,
                  "waveClearBonus": 20,
                  "maxTicks": 20000,
                  "spawnJitterTicks": 3,
                  "waypoints": [
                    {"x": 0, "y": 2},
                    {"x": 5, "y": 2},
                    {"x": 5, "y": 8}
                  ],
                  "allowedTowers": ["Cannon"],
                  "waves": [
                    {
                      "waveNumber": 1,
                      "spawnGroups": [
                        {"enemyType": "Basic", "count": 5, "interval": 10, "startDelay": 0}
                      ]
                    }
                  ]
                }
                """;

        Files.writeString(tempDir.resolve("towers.json"), towersJson);
        Files.writeString(tempDir.resolve("enemies.json"), enemiesJson);
        Files.writeString(tempDir.resolve("level1.json"), levelJson);

        GameConfig config = loader.loadFromDirectory(tempDir);
        assertNotNull(config);
        assertEquals(1, config.towers().size());
        assertEquals(1, config.enemies().size());
        assertEquals("Test Level", config.level().name());
    }

    @Test
    @DisplayName("Validation fails when tower statistics are non-positive")
    void testInvalidNonPositiveTowerStatistics() {
        TowerConfig negativeCost = new TowerConfig("BadCost", -10, 2.5, 10.0, 15);
        InvalidConfigException ex1 = assertThrows(InvalidConfigException.class, () ->
                loader.validateTowers(Map.of("BadCost", negativeCost)));
        assertTrue(ex1.getMessage().contains("positive cost"));

        TowerConfig zeroRange = new TowerConfig("ZeroRange", 50, 0.0, 10.0, 15);
        InvalidConfigException ex2 = assertThrows(InvalidConfigException.class, () ->
                loader.validateTowers(Map.of("ZeroRange", zeroRange)));
        assertTrue(ex2.getMessage().contains("positive range"));

        TowerConfig zeroDamage = new TowerConfig("ZeroDamage", 50, 2.5, 0.0, 15);
        InvalidConfigException ex3 = assertThrows(InvalidConfigException.class, () ->
                loader.validateTowers(Map.of("ZeroDamage", zeroDamage)));
        assertTrue(ex3.getMessage().contains("positive damage"));

        TowerConfig negativeCooldown = new TowerConfig("NegativeCooldown", 50, 2.5, 10.0, -5);
        InvalidConfigException ex4 = assertThrows(InvalidConfigException.class, () ->
                loader.validateTowers(Map.of("NegativeCooldown", negativeCooldown)));
        assertTrue(ex4.getMessage().contains("positive cooldown"));

        TowerConfig invalidSlow = new TowerConfig("BadSlow", "MVP", 50, 2.0, 3.0, 15, "BOTH", 35, 0.0, 1.5, 30, null);
        assertThrows(InvalidConfigException.class, () -> loader.validateTowers(Map.of("BadSlow", invalidSlow)));

        TowerConfig invalidSlowTicks = new TowerConfig("BadSlowTicks", "MVP", 50, 2.0, 3.0, 15, "BOTH", 35, 0.0, 0.5, 0, null);
        assertThrows(InvalidConfigException.class, () -> loader.validateTowers(Map.of("BadSlowTicks", invalidSlowTicks)));
    }

    @Test
    @DisplayName("Validation fails when enemy statistics are non-positive")
    void testInvalidNonPositiveEnemyStatistics() {
        EnemyConfig zeroHealth = new EnemyConfig("ZeroHealth", 0.0, 0.05, 10, 1);
        InvalidConfigException ex1 = assertThrows(InvalidConfigException.class, () ->
                loader.validateEnemies(Map.of("ZeroHealth", zeroHealth)));
        assertTrue(ex1.getMessage().contains("positive health"));

        EnemyConfig negativeSpeed = new EnemyConfig("NegativeSpeed", 50.0, -0.1, 10, 1);
        InvalidConfigException ex2 = assertThrows(InvalidConfigException.class, () ->
                loader.validateEnemies(Map.of("NegativeSpeed", negativeSpeed)));
        assertTrue(ex2.getMessage().contains("positive speed"));

        EnemyConfig zeroReward = new EnemyConfig("ZeroReward", 50.0, 0.05, 0, 1);
        InvalidConfigException ex3 = assertThrows(InvalidConfigException.class, () ->
                loader.validateEnemies(Map.of("ZeroReward", zeroReward)));
        assertTrue(ex3.getMessage().contains("positive reward"));

        EnemyConfig zeroDamage = new EnemyConfig("ZeroDamage", 50.0, 0.05, 10, 0);
        InvalidConfigException ex4 = assertThrows(InvalidConfigException.class, () ->
                loader.validateEnemies(Map.of("ZeroDamage", zeroDamage)));
        assertTrue(ex4.getMessage().contains("positive base damage"));
    }

    @Test
    @DisplayName("Validation fails when armour or evasion values are outside [0.0, 1.0]")
    void testInvalidArmourAndEvasion() {
        EnemyConfig negativeArmour = new EnemyConfig("BadArmour", "MVP", 50.0, 0.05, -0.1, 10, 1, 0.0);
        InvalidConfigException ex1 = assertThrows(InvalidConfigException.class, () ->
                loader.validateEnemies(Map.of("BadArmour", negativeArmour)));
        assertTrue(ex1.getMessage().contains("armour must be between 0.0 and 1.0"));

        EnemyConfig excessiveArmour = new EnemyConfig("TooMuchArmour", "MVP", 50.0, 0.05, 1.5, 10, 1, 0.0);
        InvalidConfigException ex2 = assertThrows(InvalidConfigException.class, () ->
                loader.validateEnemies(Map.of("TooMuchArmour", excessiveArmour)));
        assertTrue(ex2.getMessage().contains("armour must be between 0.0 and 1.0"));

        EnemyConfig excessiveEvasion = new EnemyConfig("TooMuchEvasion", "MVP", 50.0, 0.05, 0.2, 10, 1, 1.2);
        InvalidConfigException ex3 = assertThrows(InvalidConfigException.class, () ->
                loader.validateEnemies(Map.of("TooMuchEvasion", excessiveEvasion)));
        assertTrue(ex3.getMessage().contains("evasion chance must be between 0.0 and 1.0"));
    }

    @Test
    @DisplayName("Validation fails when waypoint lies outside grid dimensions")
    void testWaypointOutsideGrid() {
        List<Position> waypoints = List.of(
                new Position(0, 1),
                new Position(15, 1) // 15 >= width (12)
        );
        LevelConfig level = TestLevelHelper.createStandardLevel(
                "OutOfBounds", 12, 8, waypoints,
                List.of(), List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
        );

        InvalidConfigException ex = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(level));
        assertTrue(ex.getMessage().contains("outside grid bounds"));
    }

    @Test
    @DisplayName("Validation fails when consecutive path segment is diagonal (not axis-aligned)")
    void testDiagonalPathSegment() {
        List<Position> waypoints = List.of(
                new Position(0, 1),
                new Position(4, 5) // Diagonal: both dx and dy != 0
        );
        LevelConfig level = TestLevelHelper.createStandardLevel(
                "DiagonalPath", 12, 8, waypoints,
                List.of(), List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
        );

        InvalidConfigException ex = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(level));
        assertTrue(ex.getMessage().contains("axis-aligned"));
    }

    @Test
    @DisplayName("Validation fails when consecutive path segments are disconnected")
    void testDisconnectedPathSegment() {
        PathSegment seg1 = new PathSegment(new Position(0, 1), new Position(5, 1));
        PathSegment seg2 = new PathSegment(new Position(6, 1), new Position(6, 5)); // Gap from (5,1) to (6,1)

        LevelConfig level = TestLevelHelper.createStandardLevelWithSegments(
                "DisconnectedPath", 12, 8,
                List.of(),
                List.of(seg1, seg2),
                List.of(),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
        );

        InvalidConfigException ex = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(level));
        assertTrue(ex.getMessage().contains("connected"));
    }

    @Test
    @DisplayName("Validation fails when level provides contradictory waypoints and segments")
    void testContradictoryPathConfiguration() {
        List<Position> waypoints = List.of(
                new Position(0, 1),
                new Position(6, 1),
                new Position(6, 5)
        );
        List<PathSegment> segments = List.of(
                new PathSegment(new Position(0, 1), new Position(4, 1)), // Different endpoint!
                new PathSegment(new Position(4, 1), new Position(4, 5))
        );

        LevelConfig level = TestLevelHelper.createStandardLevelWithSegments(
                "ContradictoryPath", 12, 8,
                waypoints,
                segments,
                List.of(),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
        );

        InvalidConfigException ex = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(level));
        assertTrue(ex.getMessage().contains("Contradictory path configuration"));
    }

    @Test
    @DisplayName("Validation fails when level references an unknown tower type")
    void testUnknownTowerTypeReferencedInLevel() {
        Map<String, TowerConfig> towers = Map.of(
                "Cannon", new TowerConfig("Cannon", 60, 2.5, 12.0, 12)
        );
        Map<String, EnemyConfig> enemies = Map.of(
                "Basic", new EnemyConfig("Basic", 40.0, 0.05, 8, 1)
        );
        LevelConfig level = TestLevelHelper.createStandardLevel(
                "UnknownTowerLevel", 12, 8,
                List.of(new Position(0, 1), new Position(5, 1)),
                List.of("Cannon", "LaserTower"),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
        );

        InvalidConfigException ex = assertThrows(InvalidConfigException.class, () ->
                loader.validate(towers, enemies, level));
        assertTrue(ex.getMessage().contains("unknown tower type"));
        assertTrue(ex.getMessage().contains("LaserTower"));
    }

    @Test
    @DisplayName("Validation fails when wave spawn group references an unknown enemy type")
    void testUnknownEnemyTypeReferencedInWave() {
        Map<String, TowerConfig> towers = Map.of(
                "Cannon", new TowerConfig("Cannon", 60, 2.5, 12.0, 12)
        );
        Map<String, EnemyConfig> enemies = Map.of(
                "Basic", new EnemyConfig("Basic", 40.0, 0.05, 8, 1)
        );
        LevelConfig level = TestLevelHelper.createStandardLevel(
                "UnknownEnemyLevel", 12, 8,
                List.of(new Position(0, 1), new Position(5, 1)),
                List.of("Cannon"),
                List.of(new WaveConfig(List.of(
                        new SpawnGroupConfig("AlienDragon", 5, 10, 0)
                )))
        );

        InvalidConfigException ex = assertThrows(InvalidConfigException.class, () ->
                loader.validate(towers, enemies, level));
        assertTrue(ex.getMessage().contains("unknown enemy type"));
        assertTrue(ex.getMessage().contains("AlienDragon"));
    }

    @Test
    @DisplayName("Validation fails when wave spawn groups have invalid counts, intervals, or delays")
    void testInvalidWaveSpawnGroupParameters() {
        LevelConfig zeroCountLevel = TestLevelHelper.createStandardLevel(
                "ZeroCount", 10, 10,
                List.of(new Position(0, 0), new Position(5, 0)),
                List.of(),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 0, 10, 0))))
        );
        InvalidConfigException ex1 = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroCountLevel));
        assertTrue(ex1.getMessage().contains("positive count"));

        LevelConfig zeroIntervalLevel = TestLevelHelper.createStandardLevel(
                "ZeroInterval", 10, 10,
                List.of(new Position(0, 0), new Position(5, 0)),
                List.of(),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 0, 0))))
        );
        InvalidConfigException ex2 = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroIntervalLevel));
        assertTrue(ex2.getMessage().contains("positive interval"));

        LevelConfig negativeDelayLevel = TestLevelHelper.createStandardLevel(
                "NegativeDelay", 10, 10,
                List.of(new Position(0, 0), new Position(5, 0)),
                List.of(),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, -5))))
        );
        InvalidConfigException ex3 = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(negativeDelayLevel));
        assertTrue(ex3.getMessage().contains("start delay cannot be negative"));
    }

    @Test
    @DisplayName("Validation fails when map dimensions are non-positive")
    void testInvalidMapDimensions() {
        LevelConfig invalidWidth = TestLevelHelper.createStandardLevel(
                "InvalidWidth", 0, 8,
                List.of(new Position(0, 1), new Position(5, 1)),
                List.of(),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
        );
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(invalidWidth));

        LevelConfig invalidHeight = TestLevelHelper.createStandardLevel(
                "InvalidHeight", 10, -5,
                List.of(new Position(0, 1), new Position(5, 1)),
                List.of(),
                List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))))
        );
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(invalidHeight));
    }

    @Test
    @DisplayName("Validation fails when tower specifies an invalid target capability")
    void testInvalidTargetTypeRejected(@TempDir Path tempDir) throws IOException {
        String badTowersJson = """
                [
                  {"type": "Laser", "tier": "MVP", "cost": 50, "range": 2.0, "damage": 10.0, "cooldown": 10, "targets": "SPACE"}
                ]
                """;
        Files.writeString(tempDir.resolve("towers.json"), badTowersJson);
        assertThrows(InvalidConfigException.class, () -> loader.loadTowers(tempDir.resolve("towers.json")));
    }

    @Test
    @DisplayName("Validation fails when starting gold, starting base health, prep period, bonus, max ticks, or jitter are non-positive or null")
    void testInvalidStartingStatsRejected() {
        List<Position> waypoints = List.of(new Position(0, 1), new Position(5, 1));
        List<WaveConfig> waves = List.of(new WaveConfig(List.of(new SpawnGroupConfig("Basic", 5, 10, 0))));

        LevelConfig zeroGold = TestLevelHelper.createLevel("ZeroGold", 10, 8, waypoints, List.of(), waves, 0, 20, 100);
        InvalidConfigException ex1 = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroGold));
        assertTrue(ex1.getMessage().contains("Starting gold must be positive"));

        LevelConfig zeroHealth = TestLevelHelper.createLevel("ZeroHealth", 10, 8, waypoints, List.of(), waves, 150, -1, 100);
        InvalidConfigException ex2 = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroHealth));
        assertTrue(ex2.getMessage().contains("Starting base health must be positive"));

        LevelConfig zeroPrep = TestLevelHelper.createLevel("ZeroPrep", 10, 8, waypoints, List.of(), waves, 150, 20, 0);
        InvalidConfigException ex3 = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroPrep));
        assertTrue(ex3.getMessage().contains("Preparation period ticks must be positive"));

        LevelConfig zeroBonus = TestLevelHelper.createLevel("ZeroBonus", 10, 8, waypoints, List.of(), List.of(), waves, 150, 20, 100, 0, 20000, 3);
        InvalidConfigException exBonus = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroBonus));
        assertTrue(exBonus.getMessage().contains("Wave clear bonus must be positive"));

        LevelConfig zeroMaxTicks = TestLevelHelper.createLevel("ZeroMaxTicks", 10, 8, waypoints, List.of(), List.of(), waves, 150, 20, 100, 20, 0, 3);
        InvalidConfigException exMax = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroMaxTicks));
        assertTrue(exMax.getMessage().contains("Max ticks must be positive"));

        LevelConfig zeroJitter = TestLevelHelper.createLevel("ZeroJitter", 10, 8, waypoints, List.of(), List.of(), waves, 150, 20, 100, 20, 20000, 0);
        InvalidConfigException exJitter = assertThrows(InvalidConfigException.class, () -> loader.validateLevel(zeroJitter));
        assertTrue(exJitter.getMessage().contains("Spawn jitter ticks must be positive"));

        LevelConfig nullGold = TestLevelHelper.createLevel("NullGold", 10, 8, waypoints, List.of(), waves, null, 20, 100);
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(nullGold));

        LevelConfig nullHealth = TestLevelHelper.createLevel("NullHealth", 10, 8, waypoints, List.of(), waves, 150, null, 100);
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(nullHealth));

        LevelConfig nullPrep = TestLevelHelper.createLevel("NullPrep", 10, 8, waypoints, List.of(), waves, 150, 20, null);
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(nullPrep));

        LevelConfig nullBonus = TestLevelHelper.createLevel("NullBonus", 10, 8, waypoints, List.of(), List.of(), waves, 150, 20, 100, null, 20000, 3);
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(nullBonus));

        LevelConfig nullMax = TestLevelHelper.createLevel("NullMax", 10, 8, waypoints, List.of(), List.of(), waves, 150, 20, 100, 20, null, 3);
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(nullMax));

        LevelConfig nullJitter = TestLevelHelper.createLevel("NullJitter", 10, 8, waypoints, List.of(), List.of(), waves, 150, 20, 100, 20, 20000, null);
        assertThrows(InvalidConfigException.class, () -> loader.validateLevel(nullJitter));
    }

    @Test
    @DisplayName("Validation fails when level JSON lacks startingGold, startingBaseHealth, prepPeriodTicks, waveClearBonus, maxTicks, or spawnJitterTicks")
    void testMissingRequiredStatsInJsonRejected(@TempDir Path tempDir) throws IOException {
        String baseJson = """
                {
                  "name": "MissingFieldLevel",
                  "width": 10,
                  "height": 10,
                  "waypoints": [{"x": 0, "y": 1}, {"x": 5, "y": 1}],
                  "waves": [{"waveNumber": 1, "spawnGroups": [{"enemyType": "Basic", "count": 1, "interval": 10}]}]
                """;

        // Missing startingGold
        String missingGoldJson = baseJson + """
                  ,"startingBaseHealth": 20, "prepPeriodTicks": 100, "waveClearBonus": 20, "maxTicks": 20000, "spawnJitterTicks": 3
                }
                """;
        Path goldFile = tempDir.resolve("missingGold.json");
        Files.writeString(goldFile, missingGoldJson);
        InvalidConfigException exGold = assertThrows(InvalidConfigException.class, () -> loader.loadLevel(goldFile));
        assertTrue(exGold.getMessage().contains("startingGold"));

        // Missing startingBaseHealth
        String missingHealthJson = baseJson + """
                  ,"startingGold": 150, "prepPeriodTicks": 100, "waveClearBonus": 20, "maxTicks": 20000, "spawnJitterTicks": 3
                }
                """;
        Path healthFile = tempDir.resolve("missingHealth.json");
        Files.writeString(healthFile, missingHealthJson);
        InvalidConfigException exHealth = assertThrows(InvalidConfigException.class, () -> loader.loadLevel(healthFile));
        assertTrue(exHealth.getMessage().contains("startingBaseHealth"));

        // Missing prepPeriodTicks
        String missingPrepJson = baseJson + """
                  ,"startingGold": 150, "startingBaseHealth": 20, "waveClearBonus": 20, "maxTicks": 20000, "spawnJitterTicks": 3
                }
                """;
        Path prepFile = tempDir.resolve("missingPrep.json");
        Files.writeString(prepFile, missingPrepJson);
        InvalidConfigException exPrep = assertThrows(InvalidConfigException.class, () -> loader.loadLevel(prepFile));
        assertTrue(exPrep.getMessage().contains("prepPeriodTicks"));

        // Missing waveClearBonus
        String missingBonusJson = baseJson + """
                  ,"startingGold": 150, "startingBaseHealth": 20, "prepPeriodTicks": 100, "maxTicks": 20000, "spawnJitterTicks": 3
                }
                """;
        Path bonusFile = tempDir.resolve("missingBonus.json");
        Files.writeString(bonusFile, missingBonusJson);
        InvalidConfigException exBonus = assertThrows(InvalidConfigException.class, () -> loader.loadLevel(bonusFile));
        assertTrue(exBonus.getMessage().contains("waveClearBonus"));

        // Missing maxTicks
        String missingMaxTicksJson = baseJson + """
                  ,"startingGold": 150, "startingBaseHealth": 20, "prepPeriodTicks": 100, "waveClearBonus": 20, "spawnJitterTicks": 3
                }
                """;
        Path maxTicksFile = tempDir.resolve("missingMaxTicks.json");
        Files.writeString(maxTicksFile, missingMaxTicksJson);
        InvalidConfigException exMaxTicks = assertThrows(InvalidConfigException.class, () -> loader.loadLevel(maxTicksFile));
        assertTrue(exMaxTicks.getMessage().contains("maxTicks"));

        // Missing spawnJitterTicks
        String missingJitterJson = baseJson + """
                  ,"startingGold": 150, "startingBaseHealth": 20, "prepPeriodTicks": 100, "waveClearBonus": 20, "maxTicks": 20000
                }
                """;
        Path jitterFile = tempDir.resolve("missingJitter.json");
        Files.writeString(jitterFile, missingJitterJson);
        InvalidConfigException exJitter = assertThrows(InvalidConfigException.class, () -> loader.loadLevel(jitterFile));
        assertTrue(exJitter.getMessage().contains("spawnJitterTicks"));
    }
}
