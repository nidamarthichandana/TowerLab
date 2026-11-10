package com.towerlab.engine.factory;

import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.model.ArmoredEnemy;
import com.towerlab.engine.model.BasicEnemy;
import com.towerlab.engine.model.CannonTower;
import com.towerlab.engine.model.Enemy;
import com.towerlab.engine.model.FastEnemy;
import com.towerlab.engine.model.FrostTower;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.model.SniperTower;
import com.towerlab.engine.model.Tower;
import com.towerlab.engine.strategy.TargetingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FactoryTest {

    private EnemyFactory enemyFactory;
    private TowerFactory towerFactory;

    @BeforeEach
    void setUp() {
        Map<String, EnemyConfig> enemyConfigs = Map.of(
                "Basic", new EnemyConfig("Basic", 40.0, 0.05, 8, 1),
                "Fast", new EnemyConfig("Fast", "MVP", 25.0, 0.10, 0.0, 7, 1, 0.20),
                "Armored", new EnemyConfig("Armored", "MVP", 120.0, 0.035, 0.40, 20, 2, 0.0)
        );
        enemyFactory = new EnemyFactory(enemyConfigs);

        Map<String, TowerConfig> towerConfigs = Map.of(
                "Cannon", new TowerConfig("Cannon", "MVP", 60, 2.5, 12.0, 12, "GROUND", 40, 1.0, 0.0, 0, null),
                "Sniper", new TowerConfig("Sniper", "MVP", 90, 5.0, 40.0, 40, "BOTH", 60, 0.0, 0.0, 0, null),
                "Frost", new TowerConfig("Frost", "MVP", 50, 2.0, 3.0, 15, "BOTH", 35, 0.0, 0.50, 30, null),
                "Rapid", new TowerConfig("Rapid", "Stretch", 70, 2.0, 4.0, 3, "BOTH", 0, 0.0, 0.0, 0, null)
        );
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
}
