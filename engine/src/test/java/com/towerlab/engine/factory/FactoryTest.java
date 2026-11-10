package com.towerlab.engine.factory;

import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.config.TargetType;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.model.ArmoredEnemy;
import com.towerlab.engine.model.AttackResult;
import com.towerlab.engine.model.BasicEnemy;
import com.towerlab.engine.model.CannonTower;
import com.towerlab.engine.model.Enemy;
import com.towerlab.engine.model.FastEnemy;
import com.towerlab.engine.model.FrostTower;
import com.towerlab.engine.model.Path;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.model.SniperTower;
import com.towerlab.engine.model.Tower;
import com.towerlab.engine.strategy.TargetingMode;
import com.towerlab.engine.test.TestConfigHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactoryTest {

    private EnemyFactory enemyFactory;
    private TowerFactory towerFactory;

    @BeforeEach
    void setUp() {
        enemyFactory = new EnemyFactory(TestConfigHelper.getFrozenEnemies());

        Map<String, TowerConfig> towerConfigs = new HashMap<>(TestConfigHelper.getFrozenTowers());
        towerConfigs.put("Rapid", new TowerConfig("Rapid", "Stretch", 70, 2.0, 4.0, 3, TargetType.BOTH, 0, 0.0, 0.0, 0, null));
        towerFactory = new TowerFactory(towerConfigs);
    }

    @Test
    @DisplayName("EnemyFactory creates Basic, Fast, and Armored enemies correctly")
    void testEnemyFactoryCreatesValidTypes() {
        Enemy basic = enemyFactory.create("Basic", 1);
        assertInstanceOf(BasicEnemy.class, basic);
        assertEquals(1, basic.id());
        assertEquals("Basic", basic.config().type());
        assertEquals(40, basic.currentHealth());

        Enemy fast = enemyFactory.create("Fast", 2);
        assertInstanceOf(FastEnemy.class, fast);
        assertEquals(2, fast.id());
        assertEquals("Fast", fast.config().type());
        assertEquals(0.20, fast.evasionChance(), 1e-6);

        Enemy armored = enemyFactory.create("Armored", 3);
        assertInstanceOf(ArmoredEnemy.class, armored);
        assertEquals(3, armored.id());
        assertEquals("Armored", armored.config().type());
        assertEquals(0.40, armored.config().armour(), 1e-6);
    }

    @Test
    @DisplayName("EnemyFactory rejects unknown, null, and unconfigured types")
    void testEnemyFactoryRejectsUnknownAndNull() {
        assertThrows(InvalidConfigException.class, () -> enemyFactory.create("Unknown", 1));
        assertThrows(InvalidConfigException.class, () -> enemyFactory.create("Boss", 1));
        assertThrows(InvalidConfigException.class, () -> enemyFactory.create(null, 1));
        assertThrows(InvalidConfigException.class, () -> enemyFactory.config("Unknown"));
        assertThrows(InvalidConfigException.class, () -> enemyFactory.config(null));

        EnemyFactory emptyFactory = new EnemyFactory(Map.of());
        assertThrows(InvalidConfigException.class, () -> emptyFactory.create("Basic", 1));
    }

    @Test
    @DisplayName("TowerFactory creates Cannon, Sniper, and Frost towers correctly")
    void testTowerFactoryCreatesValidTypes() {
        Position pos = new Position(2, 3);

        Tower cannon = towerFactory.create("Cannon", 10, pos, TargetingMode.FIRST.strategy());
        assertInstanceOf(CannonTower.class, cannon);
        assertEquals(10, cannon.id());
        assertEquals("Cannon", cannon.config().type());
        assertEquals(pos, cannon.position());

        Tower sniper = towerFactory.create("Sniper", 11, pos, TargetingMode.STRONGEST.strategy());
        assertInstanceOf(SniperTower.class, sniper);
        assertEquals(11, sniper.id());
        assertEquals("Sniper", sniper.config().type());

        Tower frost = towerFactory.create("Frost", 12, pos, TargetingMode.CLOSEST.strategy());
        assertInstanceOf(FrostTower.class, frost);
        assertEquals(12, frost.id());
        assertEquals("Frost", frost.config().type());
    }

    @Test
    @DisplayName("TowerFactory config method returns valid TowerConfig")
    void testTowerFactoryConfig() {
        TowerConfig cannonConfig = towerFactory.config("Cannon");
        assertNotNull(cannonConfig);
        assertEquals(60, cannonConfig.cost());
        assertEquals("Cannon", cannonConfig.type());
    }

    @Test
    @DisplayName("TowerFactory rejects Rapid stretch tower and unknown types with InvalidConfigException")
    void testTowerFactoryRejectsRapidAndUnknown() {
        Position pos = new Position(1, 1);

        assertThrows(InvalidConfigException.class, () ->
                towerFactory.create("Rapid", 1, pos, TargetingMode.FIRST.strategy()));
        assertThrows(InvalidConfigException.class, () ->
                towerFactory.config("Rapid"));

        assertThrows(InvalidConfigException.class, () ->
                towerFactory.create("Laser", 2, pos, TargetingMode.FIRST.strategy()));
        assertThrows(InvalidConfigException.class, () ->
                towerFactory.config("Laser"));

        assertThrows(InvalidConfigException.class, () ->
                towerFactory.create(null, 3, pos, TargetingMode.FIRST.strategy()));
        assertThrows(InvalidConfigException.class, () ->
                towerFactory.config(null));
    }

    @Test
    @DisplayName("TowerFactory rejects null position or null strategy with IllegalArgumentException")
    void testTowerFactoryRejectsNullPositionOrStrategy() {
        assertThrows(IllegalArgumentException.class, () ->
                towerFactory.create("Cannon", 1, null, TargetingMode.FIRST.strategy()));
        assertThrows(IllegalArgumentException.class, () ->
                towerFactory.create("Cannon", 1, new Position(0, 0), null));
    }

    @Test
    @DisplayName("Proves config drives behaviour: modified Basic enemy health and Cannon damage")
    void testConfigDrivesBehaviour() {
        // 1. Load a modified copy of enemy config with Basic health 80
        EnemyConfig originalEnemy = TestConfigHelper.getFrozenEnemyConfig("Basic");
        EnemyConfig modifiedEnemy = new EnemyConfig(
                originalEnemy.type(),
                originalEnemy.tier(),
                80.0,
                originalEnemy.speed(),
                originalEnemy.armour(),
                originalEnemy.reward(),
                originalEnemy.baseDamage(),
                originalEnemy.evasionChance()
        );
        EnemyFactory customEnemyFactory = new EnemyFactory(Map.of("Basic", modifiedEnemy));
        Enemy basic = customEnemyFactory.create("Basic", 1);
        assertEquals(80, basic.currentHealth());
        assertEquals(80, basic.maxHealth());

        // 2. Load a modified copy of tower config with Cannon damage 99.0
        TowerConfig originalTower = TestConfigHelper.getFrozenTowerConfig("Cannon");
        TowerConfig modifiedTower = new TowerConfig(
                originalTower.type(),
                originalTower.tier(),
                originalTower.cost(),
                originalTower.range(),
                99.0,
                originalTower.cooldown(),
                originalTower.targets(),
                originalTower.upgradeCost(),
                originalTower.splashRadius(),
                originalTower.slowFraction(),
                originalTower.slowDurationTicks(),
                originalTower.special()
        );
        TowerFactory customTowerFactory = new TowerFactory(Map.of("Cannon", modifiedTower));
        Tower cannon = customTowerFactory.create("Cannon", 100, new Position(0, 0), TargetingMode.FIRST.strategy());
        assertEquals(99.0, cannon.config().damage());

        // Strengthened verification: Cannon with modified damage 99 attacks a Basic enemy with enough health
        EnemyConfig tankyConfig = new EnemyConfig(
                originalEnemy.type(),
                originalEnemy.tier(),
                200.0,
                originalEnemy.speed(),
                0.0,
                originalEnemy.reward(),
                originalEnemy.baseDamage(),
                0.0
        );
        BasicEnemy target = new BasicEnemy(2, tankyConfig);
        Path path = new Path(List.of(new Position(0, 0), new Position(10, 0)));
        target.advance(path);

        AttackResult result = cannon.update(List.of(target), path, new SplittableRandom(1));
        assertTrue(result.fired());
        assertEquals(1, result.hits().size());
        assertEquals(99, result.hits().get(0).damageDealt(), "Attack damageDealt must be 99 from modified config");
        assertEquals(101, target.currentHealth(), "Target health lost must be 99 (200 - 99 = 101)");
    }
}
