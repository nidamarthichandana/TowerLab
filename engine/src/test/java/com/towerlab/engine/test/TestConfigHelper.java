package com.towerlab.engine.test;

import com.towerlab.engine.config.ConfigLoader;
import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.model.ArmoredEnemy;
import com.towerlab.engine.model.BasicEnemy;
import com.towerlab.engine.model.CannonTower;
import com.towerlab.engine.model.Enemy;
import com.towerlab.engine.model.FastEnemy;
import com.towerlab.engine.model.FrostTower;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.model.SniperTower;
import com.towerlab.engine.strategy.TargetingStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Test helper that loads frozen v2 rule configurations placed in src/test/resources:
 * - level1-rules-v2.json
 * - towers-rules-v2.json
 * - enemies-rules-v2.json
 *
 * Tests asserting specific rule numbers (40 HP, wave 2 ticks, and so on) use these frozen copies
 * so tuning src/main/resources later cannot break them.
 */
public final class TestConfigHelper {

    private static final Map<String, TowerConfig> FROZEN_TOWERS;
    private static final Map<String, EnemyConfig> FROZEN_ENEMIES;
    private static final LevelConfig FROZEN_LEVEL;

    static {
        ConfigLoader loader = new ConfigLoader();
        try (InputStream towersIn = TestConfigHelper.class.getClassLoader().getResourceAsStream("towers-rules-v2.json");
             InputStream enemiesIn = TestConfigHelper.class.getClassLoader().getResourceAsStream("enemies-rules-v2.json");
             InputStream levelIn = TestConfigHelper.class.getClassLoader().getResourceAsStream("level1-rules-v2.json")) {

            if (towersIn == null) {
                throw new IllegalStateException("Classpath resource towers-rules-v2.json not found");
            }
            if (enemiesIn == null) {
                throw new IllegalStateException("Classpath resource enemies-rules-v2.json not found");
            }
            if (levelIn == null) {
                throw new IllegalStateException("Classpath resource level1-rules-v2.json not found");
            }

            FROZEN_TOWERS = Map.copyOf(loader.loadTowers(towersIn));
            FROZEN_ENEMIES = Map.copyOf(loader.loadEnemies(enemiesIn));
            FROZEN_LEVEL = loader.loadLevel(levelIn);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TestConfigHelper() {}

    public static Map<String, TowerConfig> getFrozenTowers() {
        return FROZEN_TOWERS;
    }

    public static TowerConfig getFrozenTowerConfig(String type) {
        TowerConfig config = FROZEN_TOWERS.get(type);
        if (config == null) {
            throw new IllegalArgumentException("Unknown tower type in frozen configs: " + type);
        }
        return config;
    }

    public static Map<String, EnemyConfig> getFrozenEnemies() {
        return FROZEN_ENEMIES;
    }

    public static EnemyConfig getFrozenEnemyConfig(String type) {
        EnemyConfig config = FROZEN_ENEMIES.get(type);
        if (config == null) {
            throw new IllegalArgumentException("Unknown enemy type in frozen configs: " + type);
        }
        return config;
    }

    public static LevelConfig getFrozenLevel() {
        return FROZEN_LEVEL;
    }

    public static GameConfig getFrozenGameConfig() {
        return new GameConfig(FROZEN_TOWERS, FROZEN_ENEMIES, FROZEN_LEVEL);
    }

    public static BasicEnemy createBasicEnemy(int id) {
        return new BasicEnemy(id, getFrozenEnemyConfig("Basic"));
    }

    public static FastEnemy createFastEnemy(int id) {
        return new FastEnemy(id, getFrozenEnemyConfig("Fast"));
    }

    public static ArmoredEnemy createArmoredEnemy(int id) {
        return new ArmoredEnemy(id, getFrozenEnemyConfig("Armored"));
    }

    public static Enemy createEnemy(String type, int id) {
        return switch (type) {
            case "Basic" -> createBasicEnemy(id);
            case "Fast" -> createFastEnemy(id);
            case "Armored" -> createArmoredEnemy(id);
            default -> throw new IllegalArgumentException("Unsupported enemy type: " + type);
        };
    }

    public static CannonTower createCannon(int id, Position position, TargetingStrategy strategy) {
        return new CannonTower(id, getFrozenTowerConfig("Cannon"), position, strategy);
    }

    public static SniperTower createSniper(int id, Position position, TargetingStrategy strategy) {
        return new SniperTower(id, getFrozenTowerConfig("Sniper"), position, strategy);
    }

    public static FrostTower createFrost(int id, Position position, TargetingStrategy strategy) {
        return new FrostTower(id, getFrozenTowerConfig("Frost"), position, strategy);
    }
}
