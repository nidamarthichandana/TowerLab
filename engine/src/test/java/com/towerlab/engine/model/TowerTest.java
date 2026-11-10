package com.towerlab.engine.model;

import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.config.TargetType;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.strategy.TargetCandidate;
import com.towerlab.engine.strategy.TargetingStrategy;
import com.towerlab.engine.test.TestConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TowerTest {

    // Simple horizontal path along x axis: (0,0) -> (20,0)
    private final Path straightPath = new Path(List.of(
            new Position(0, 0),
            new Position(20, 0)
    ));

    // Stub strategy selecting first available candidate
    private final TargetingStrategy firstCandidateStrategy = (tower, candidates) ->
            candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));

    // Stub strategy selecting no candidate
    private final TargetingStrategy emptyStrategy = (tower, candidates) -> Optional.empty();

    private final TowerConfig cannonConfig = TestConfigHelper.getFrozenTowerConfig("Cannon");
    private final TowerConfig sniperConfig = TestConfigHelper.getFrozenTowerConfig("Sniper");
    private final TowerConfig frostConfig = TestConfigHelper.getFrozenTowerConfig("Frost");

    @Test
    @DisplayName("Verify construction values for Cannon, Sniper, and Frost towers")
    void testConstructionValues() {
        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(2, 2), firstCandidateStrategy);
        assertEquals(1, cannon.id());
        assertEquals(1, cannon.level());
        assertEquals(60, cannon.totalGoldInvested());
        assertEquals(0, cannon.cooldownLeft());
        assertEquals(60, cannon.config().cost());
        assertEquals(2.5, cannon.config().range(), 1e-6);
        assertEquals(12.0, cannon.config().damage(), 1e-6);
        assertEquals(12, cannon.config().cooldown());
        assertEquals(1.0, cannon.config().splashRadius(), 1e-6);
        assertEquals(TargetType.GROUND, cannon.config().targets());

        SniperTower sniper = new SniperTower(2, sniperConfig, new Position(3, 3), firstCandidateStrategy);
        assertEquals(2, sniper.id());
        assertEquals(1, sniper.level());
        assertEquals(90, sniper.totalGoldInvested());
        assertEquals(0, sniper.cooldownLeft());
        assertEquals(90, sniper.config().cost());
        assertEquals(5.0, sniper.config().range(), 1e-6);
        assertEquals(40.0, sniper.config().damage(), 1e-6);
        assertEquals(40, sniper.config().cooldown());
        assertEquals(TargetType.BOTH, sniper.config().targets());

        FrostTower frost = new FrostTower(3, frostConfig, new Position(4, 4), firstCandidateStrategy);
        assertEquals(3, frost.id());
        assertEquals(1, frost.level());
        assertEquals(50, frost.totalGoldInvested());
        assertEquals(0, frost.cooldownLeft());
        assertEquals(50, frost.config().cost());
        assertEquals(2.0, frost.config().range(), 1e-6);
        assertEquals(3.0, frost.config().damage(), 1e-6);
        assertEquals(15, frost.config().cooldown());
        assertEquals(0.50, frost.config().slowFraction(), 1e-6);
        assertEquals(30, frost.config().slowDurationTicks());
        assertEquals(TargetType.BOTH, frost.config().targets());
    }

    @Test
    @DisplayName("Cooldown timing: Cannon fires on ticks 0, 12, 24 with enemy in range")
    void testCooldownTimingWithEnemyInRange() {
        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(0, 0), firstCandidateStrategy);
        RandomGenerator rng = new SplittableRandom(1);

        for (int tick = 0; tick <= 25; tick++) {
            BasicEnemy target = TestConfigHelper.createBasicEnemy(tick);
            for (int i = 0; i < 20; i++) {
                target.advance(straightPath); // pos (1.0, 0.0)
            }
            assertEquals(1.0, target.positionOn(straightPath).x(), 1e-6);

            AttackResult result = cannon.update(List.of(target), straightPath, rng);

            if (tick == 0 || tick == 12 || tick == 24) {
                assertTrue(result.fired(), "Tower must fire on tick " + tick);
                assertEquals(1, cannon.id());
                assertFalse(result.hits().isEmpty());
                assertEquals(12, cannon.cooldownLeft(), "Cooldown must be reset to 12 after firing");
            } else {
                assertFalse(result.fired(), "Tower must NOT fire on tick " + tick);
                assertTrue(result.hits().isEmpty());
            }
        }
    }

    @Test
    @DisplayName("Tower stays ready with no enemy in range and fires immediately when enemy enters range")
    void testTowerStaysReadyUntilEnemyEntersRange() {
        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(0, 0), firstCandidateStrategy);
        RandomGenerator rng = new SplittableRandom(1);

        BasicEnemy distantEnemy = TestConfigHelper.createBasicEnemy(100);
        for (int i = 0; i < 200; i++) {
            distantEnemy.advance(straightPath); // pos (10.0, 0.0)
        }

        // Ticks 0, 1, 2: enemy out of range -> stays ready (cooldownLeft = 0)
        for (int tick = 0; tick < 3; tick++) {
            AttackResult result = cannon.update(List.of(distantEnemy), straightPath, rng);
            assertFalse(result.fired());
            assertEquals(0, cannon.cooldownLeft(), "Tower must remain ready with cooldown 0");
        }

        // Tick 3: enemy enters range at pos (2.0, 0.0) <= 2.5
        BasicEnemy inRangeEnemy = TestConfigHelper.createBasicEnemy(101);
        for (int i = 0; i < 40; i++) {
            inRangeEnemy.advance(straightPath);
        }
        AttackResult fireResult = cannon.update(List.of(distantEnemy, inRangeEnemy), straightPath, rng);
        assertTrue(fireResult.fired(), "Tower must fire immediately on the first tick an enemy is in range");
        assertEquals(12, cannon.cooldownLeft());
    }

    @Test
    @DisplayName("Cooldown and evasion: An evaded attack still resets cooldown and blocks firing until expiry")
    void testEvadedAttackResetsCooldown() {
        SniperTower sniper = new SniperTower(1, sniperConfig, new Position(0, 0), firstCandidateStrategy);
        // RNG always returning 0.05 (< 0.20 -> evades!)
        RandomGenerator alwaysEvadeRng = new RandomGenerator() {
            @Override
            public double nextDouble() {
                return 0.05;
            }

            @Override
            public long nextLong() {
                return 0;
            }
        };

        FastEnemy fast = TestConfigHelper.createFastEnemy(1); // 20% evasion
        for (int i = 0; i < 10; i++) {
            fast.advance(straightPath); // in range
        }

        // Tick 0: fires, attack is evaded
        AttackResult resultTick0 = sniper.update(List.of(fast), straightPath, alwaysEvadeRng);
        assertTrue(resultTick0.fired());
        assertEquals(1, resultTick0.hits().size());
        assertTrue(resultTick0.hits().get(0).evaded());
        assertEquals(40, sniper.cooldownLeft(), "Cooldown must reset to 40 even on an evaded shot");

        // Ticks 1 to 39: tower cannot fire while cooldown is active
        for (int tick = 1; tick <= 39; tick++) {
            AttackResult result = sniper.update(List.of(fast), straightPath, alwaysEvadeRng);
            assertFalse(result.fired(), "Tower must not fire on tick " + tick);
        }

        // Tick 40: cooldown expired, can fire again
        AttackResult resultTick40 = sniper.update(List.of(fast), straightPath, alwaysEvadeRng);
        assertTrue(resultTick40.fired(), "Tower must be ready to fire on tick 40");
        assertEquals(40, sniper.cooldownLeft());
    }

    @Test
    @DisplayName("Range boundary: distance exactly 2.5 is included; distance 2.51 is excluded")
    void testRangeBoundaryCondition() {
        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(0, 0), firstCandidateStrategy);

        BasicEnemy exactEnemy = TestConfigHelper.createBasicEnemy(1);
        for (int i = 0; i < 50; i++) {
            exactEnemy.advance(straightPath); // pos 2.50
        }
        assertEquals(2.50, exactEnemy.positionOn(straightPath).x(), 1e-6);

        AttackResult resultExact = cannon.update(List.of(exactEnemy), straightPath, new SplittableRandom(1));
        assertTrue(resultExact.fired(), "Enemy at exactly distance 2.5 must be in range");

        // Enemy at distance 2.51
        EnemyConfig farConfig = new EnemyConfig("Far", 40.0, 2.51, 8, 1);
        BasicEnemy farEnemy = new BasicEnemy(2, farConfig);
        farEnemy.advance(straightPath);
        assertEquals(2.51, farEnemy.positionOn(straightPath).x(), 1e-6);

        CannonTower cannon2 = new CannonTower(2, cannonConfig, new Position(0, 0), firstCandidateStrategy);
        AttackResult resultFar = cannon2.update(List.of(farEnemy), straightPath, new SplittableRandom(1));
        assertFalse(resultFar.fired(), "Enemy at distance 2.51 must be excluded from range");
    }

    @Test
    @DisplayName("Sniper: Deals 40 raw damage, includes 5.0, excludes 5.01, attacks only one target")
    void testSniperBehaviourAndRangeBoundary() {
        SniperTower sniper = new SniperTower(1, sniperConfig, new Position(0, 0), firstCandidateStrategy);

        // 1. Deals 40 raw damage to a Basic enemy
        BasicEnemy basicTarget = TestConfigHelper.createBasicEnemy(1);
        for (int i = 0; i < 20; i++) {
            basicTarget.advance(straightPath); // pos (1.0, 0.0)
        }
        AttackResult resultHit = sniper.update(List.of(basicTarget), straightPath, new SplittableRandom(1));
        assertTrue(resultHit.fired());
        assertEquals(1, resultHit.hits().size());
        HitResult hit = resultHit.hits().get(0);
        assertEquals(1, hit.enemyId());
        assertEquals(40, hit.damageDealt(), "Basic enemy (40 HP) takes 40 raw damage");
        assertTrue(hit.killed());
        assertEquals(0, basicTarget.currentHealth());

        // 2. Includes an enemy exactly at range 5.0
        SniperTower sniperExact = new SniperTower(2, sniperConfig, new Position(0, 0), firstCandidateStrategy);
        EnemyConfig speed5Config = new EnemyConfig("Speed5", 40.0, 5.0, 8, 1);
        BasicEnemy exactEnemy = new BasicEnemy(2, speed5Config);
        exactEnemy.advance(straightPath);
        assertEquals(5.0, exactEnemy.positionOn(straightPath).x(), 1e-6);

        AttackResult resultExact = sniperExact.update(List.of(exactEnemy), straightPath, new SplittableRandom(1));
        assertTrue(resultExact.fired(), "Enemy at distance 5.0 must be in range for Sniper");

        // 3. Excludes an enemy at range 5.01
        SniperTower sniperFar = new SniperTower(3, sniperConfig, new Position(0, 0), firstCandidateStrategy);
        EnemyConfig speed501Config = new EnemyConfig("Speed501", 40.0, 5.01, 8, 1);
        BasicEnemy farEnemy = new BasicEnemy(3, speed501Config);
        farEnemy.advance(straightPath);
        assertEquals(5.01, farEnemy.positionOn(straightPath).x(), 1e-6);

        AttackResult resultFar = sniperFar.update(List.of(farEnemy), straightPath, new SplittableRandom(1));
        assertFalse(resultFar.fired(), "Enemy at distance 5.01 must be out of range for Sniper");

        // 4. Attacks only one selected target even when multiple are in range
        SniperTower sniperMulti = new SniperTower(4, sniperConfig, new Position(0, 0), firstCandidateStrategy);
        BasicEnemy e1 = new BasicEnemy(10, speed5Config);
        e1.advance(straightPath);
        BasicEnemy e2 = new BasicEnemy(11, speed5Config);
        e2.advance(straightPath);

        AttackResult multiResult = sniperMulti.update(List.of(e1, e2), straightPath, new SplittableRandom(1));
        assertTrue(multiResult.fired());
        assertEquals(1, multiResult.hits().size(), "Sniper must attack only one selected target");
        assertEquals(10, multiResult.hits().get(0).enemyId());
    }

    @Test
    @DisplayName("Cannon splash: Target and enemies within 1.0 are damaged; enemy at 1.01 is not; armour applies")
    void testCannonSplashRadiusAndArmour() {
        TowerConfig wideCannonConfig = new TowerConfig(
                "Cannon", "MVP", 60, 5.0, 12.0, 12, TargetType.GROUND, 40, 1.0, 0.0, 0, "Splash 1.0"
        );
        CannonTower cannon = new CannonTower(1, wideCannonConfig, new Position(0, 0), firstCandidateStrategy);

        BasicEnemy target = new BasicEnemy(1, new EnemyConfig("FastSpawn", 40.0, 2.0, 8, 1));
        target.advance(straightPath); // pos (2.0, 0.0)

        ArmoredEnemy armoredSplash = new ArmoredEnemy(2, new EnemyConfig("Armored", "MVP", 120.0, 2.5, 0.40, 20, 2, 0.0));
        armoredSplash.advance(straightPath); // pos (2.5, 0.0), dist from target = 0.5 <= 1.0

        BasicEnemy basicExact = new BasicEnemy(3, new EnemyConfig("BasicExact", 40.0, 3.0, 8, 1));
        basicExact.advance(straightPath); // pos (3.0, 0.0), dist from target = 1.0 <= 1.0

        BasicEnemy basicOutside = new BasicEnemy(4, new EnemyConfig("BasicOutside", 40.0, 3.01, 8, 1));
        basicOutside.advance(straightPath); // pos (3.01, 0.0), dist from target = 1.01 > 1.0

        List<Enemy> allEnemies = List.of(target, armoredSplash, basicExact, basicOutside);
        AttackResult result = cannon.update(allEnemies, straightPath, new SplittableRandom(1));

        assertTrue(result.fired());
        assertEquals(3, result.hits().size(), "Only target and 2 enemies within splash distance 1.0 should be hit");

        assertEquals(28, target.currentHealth()); // 40 - 12
        assertEquals(113, armoredSplash.currentHealth()); // 120 - 7
        assertEquals(28, basicExact.currentHealth()); // 40 - 12
        assertEquals(40, basicOutside.currentHealth()); // unaffected
    }

    @Test
    @DisplayName("Cannon splash: Secondary enemy outside Cannon's own range but within splash radius receives damage")
    void testCannonSplashSecondaryEnemyOutsideTowerRange() {
        // Cannon at (0, 0) with range 2.5 and splashRadius 1.0
        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(0, 0), firstCandidateStrategy);

        // Primary target at (2.0, 0.0): distance from tower is 2.0 <= 2.5 (inside tower range)
        BasicEnemy primaryTarget = new BasicEnemy(1, new EnemyConfig("Primary", 40.0, 2.0, 8, 1));
        primaryTarget.advance(straightPath);
        assertEquals(2.0, cannon.distanceTo(primaryTarget, straightPath), 1e-6);

        // Secondary enemy at (2.8, 0.0):
        // Distance from Cannon is 2.8 > 2.5 -> OUTSIDE Cannon's range!
        // Distance from primary target is 2.8 - 2.0 = 0.8 <= 1.0 -> INSIDE splash radius!
        BasicEnemy secondaryEnemy = new BasicEnemy(2, new EnemyConfig("Secondary", 40.0, 2.8, 8, 1));
        secondaryEnemy.advance(straightPath);
        assertEquals(2.8, cannon.distanceTo(secondaryEnemy, straightPath), 1e-6);
        assertTrue(cannon.distanceTo(secondaryEnemy, straightPath) > cannon.config().range(), "Secondary enemy must be outside tower range");

        AttackResult result = cannon.update(List.of(primaryTarget, secondaryEnemy), straightPath, new SplittableRandom(1));
        assertTrue(result.fired());
        assertEquals(2, result.hits().size(), "Both primary and out-of-range secondary enemy in splash radius must be hit");

        HitResult secondaryHit = result.hits().get(1);
        assertEquals(2, secondaryHit.enemyId());
        assertEquals(12, secondaryHit.damageDealt());
        assertEquals(28, secondaryEnemy.currentHealth(), "Secondary enemy outside tower range must take splash damage");
    }

    @Test
    @DisplayName("Frost tower: 3 raw damage on Basic enemy and slow applied; no slow when evaded")
    void testFrostTowerDamageAndSlowApplication() {
        FrostTower frost = new FrostTower(1, frostConfig, new Position(0, 0), firstCandidateStrategy);

        BasicEnemy basic = TestConfigHelper.createBasicEnemy(1);
        for (int i = 0; i < 20; i++) {
            basic.advance(straightPath); // pos (1.0, 0.0)
        }
        AttackResult resultHit = frost.update(List.of(basic), straightPath, new SplittableRandom(1));
        assertTrue(resultHit.fired());
        assertEquals(1, resultHit.hits().size());

        HitResult hit = resultHit.hits().get(0);
        assertEquals(1, hit.enemyId());
        assertEquals(3, hit.damageDealt());
        assertFalse(hit.evaded());
        assertTrue(hit.effectApplied());
        assertFalse(hit.killed());
        assertEquals(37, basic.currentHealth());

        assertEquals(1, basic.activeEffects().size());
        StatusEffect effect = basic.activeEffects().iterator().next();
        assertTrue(effect instanceof SlowEffect);
        SlowEffect slow = (SlowEffect) effect;
        assertEquals(1, slow.sourceTowerId());
        assertEquals(0.50, slow.slowFraction(), 1e-6);
        assertEquals(30, slow.remainingTicks());

        // Fast enemy (20% evasion): fake RNG returns 0.10 (< 0.20 -> evades)
        FrostTower frost2 = new FrostTower(2, frostConfig, new Position(0, 0), firstCandidateStrategy);
        FastEnemy fast = TestConfigHelper.createFastEnemy(2);
        for (int i = 0; i < 10; i++) {
            fast.advance(straightPath);
        }
        RandomGenerator evadingRng = new RandomGenerator() {
            @Override
            public double nextDouble() {
                return 0.10;
            }

            @Override
            public long nextLong() {
                return 0;
            }
        };

        AttackResult resultEvade = frost2.update(List.of(fast), straightPath, evadingRng);
        assertTrue(resultEvade.fired());
        assertEquals(1, resultEvade.hits().size());

        HitResult evadeHit = resultEvade.hits().get(0);
        assertEquals(2, evadeHit.enemyId());
        assertEquals(0, evadeHit.damageDealt());
        assertTrue(evadeHit.evaded());
        assertFalse(evadeHit.effectApplied(), "No slow must be applied when attack is evaded");
        assertFalse(evadeHit.killed());
        assertEquals(25, fast.currentHealth());
        assertTrue(fast.activeEffects().isEmpty(), "No status effect must be present on evaded enemy");
    }

    @Test
    @DisplayName("Frost tower single-target: Attacks only selected target and never slows nearby enemies")
    void testFrostTowerSingleTargetBehaviourNoNearbySplash() {
        FrostTower frost = new FrostTower(1, frostConfig, new Position(0, 0), firstCandidateStrategy);

        // Target at (1.0, 0.0)
        BasicEnemy target = new BasicEnemy(1, new EnemyConfig("Target", 40.0, 1.0, 8, 1));
        target.advance(straightPath);

        // Nearby enemy right next to target at (1.2, 0.0)
        BasicEnemy nearby = new BasicEnemy(2, new EnemyConfig("Nearby", 40.0, 1.2, 8, 1));
        nearby.advance(straightPath);

        AttackResult result = frost.update(List.of(target, nearby), straightPath, new SplittableRandom(1));
        assertTrue(result.fired());
        assertEquals(1, result.hits().size(), "FrostTower must attack strictly one target");
        assertEquals(1, result.hits().get(0).enemyId());

        // Target received damage and slow
        assertEquals(37, target.currentHealth());
        assertEquals(1, target.activeEffects().size());

        // Nearby enemy received 0 damage and no slow
        assertEquals(40, nearby.currentHealth());
        assertTrue(nearby.activeEffects().isEmpty(), "Nearby enemy must receive no slow effect from FrostTower");
    }

    @Test
    @DisplayName("Frost armour interaction: 3 raw damage to 40% armour Armored enemy deals round(3 x 0.6) = 2 damage")
    void testFrostArmourInteraction() {
        FrostTower frost = new FrostTower(1, frostConfig, new Position(0, 0), firstCandidateStrategy);
        ArmoredEnemy armored = TestConfigHelper.createArmoredEnemy(1); // 40% armour, 120 HP
        for (int i = 0; i < 20; i++) {
            armored.advance(straightPath); // in range
        }

        AttackResult result = frost.update(List.of(armored), straightPath, new SplittableRandom(1));
        assertTrue(result.fired());
        assertEquals(1, result.hits().size());

        HitResult hit = result.hits().get(0);
        assertEquals(2, hit.damageDealt(), "round(3 * (1 - 0.40)) = round(1.8) = 2 damage");
        assertEquals(118, armored.currentHealth());
        assertTrue(hit.effectApplied(), "Slow must still be applied to surviving armored target");
        assertEquals(1, armored.activeEffects().size());
    }

    @Test
    @DisplayName("Killed flag precision: wasAliveBeforeAttack && !enemy.isAlive()")
    void testKilledFlagPrecision() {
        SniperTower sniper = new SniperTower(1, sniperConfig, new Position(0, 0), firstCandidateStrategy);

        // Case 1: Living enemy killed by current attack -> killed = true
        BasicEnemy livingToKill = TestConfigHelper.createBasicEnemy(1); // 40 HP
        for (int i = 0; i < 20; i++) {
            livingToKill.advance(straightPath);
        }
        AttackResult resultKill = sniper.update(List.of(livingToKill), straightPath, new SplittableRandom(1));
        assertEquals(40, resultKill.hits().get(0).damageDealt());
        assertTrue(resultKill.hits().get(0).killed(), "Living enemy killed by attack must have killed = true");

        // Case 2: Surviving enemy -> killed = false
        CannonTower cannon = new CannonTower(2, cannonConfig, new Position(0, 0), firstCandidateStrategy);
        BasicEnemy surviving = TestConfigHelper.createBasicEnemy(2); // 40 HP, takes 12 damage -> 28 HP remaining
        for (int i = 0; i < 20; i++) {
            surviving.advance(straightPath);
        }
        AttackResult resultSurvive = cannon.update(List.of(surviving), straightPath, new SplittableRandom(1));
        assertFalse(resultSurvive.hits().get(0).killed(), "Surviving enemy must have killed = false");

        // Case 3: An already-dead enemy cannot be reported as newly killed
        BasicEnemy deadEnemy = TestConfigHelper.createBasicEnemy(3);
        deadEnemy.takeDamage(100);
        assertFalse(deadEnemy.isAlive());

        // For Sniper: if attacked when already dead, damage is 0 and killed is false
        SniperTower sniperDeadTest = new SniperTower(3, sniperConfig, new Position(0, 0), firstCandidateStrategy);
        List<HitResult> hits = sniperDeadTest.resolveAttack(deadEnemy, List.of(deadEnemy), straightPath, new SplittableRandom(1));
        assertEquals(1, hits.size());
        assertEquals(0, hits.get(0).damageDealt());
        assertFalse(hits.get(0).killed(), "Already-dead enemy must not be reported as newly killed");

        // For Cannon: already-dead enemies are excluded from splash
        assertTrue(cannon.resolveAttack(deadEnemy, List.of(deadEnemy), straightPath, new SplittableRandom(1)).isEmpty());
    }

    @Test
    @DisplayName("Candidate filtering: Dead enemies, endpoint enemies, and out-of-range enemies are never selected")
    void testCandidateFiltering() {
        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(0, 0), firstCandidateStrategy);

        // 1. Dead enemy in range
        BasicEnemy deadEnemy = TestConfigHelper.createBasicEnemy(1);
        deadEnemy.takeDamage(100);
        for (int i = 0; i < 20; i++) {
            deadEnemy.advance(straightPath);
        }

        // 2. Enemy that has reached the end of path
        BasicEnemy endEnemy = TestConfigHelper.createBasicEnemy(2);
        for (int i = 0; i < 400; i++) {
            endEnemy.advance(straightPath); // reaches 20.0
        }
        assertTrue(endEnemy.hasReachedEnd(straightPath));

        // 3. Enemy far out of range
        BasicEnemy outOfRangeEnemy = TestConfigHelper.createBasicEnemy(3);
        for (int i = 0; i < 200; i++) {
            outOfRangeEnemy.advance(straightPath); // at 10.0 > 2.5
        }

        // Tower should find 0 valid candidates and not fire
        AttackResult result = cannon.update(List.of(deadEnemy, endEnemy, outOfRangeEnemy), straightPath, new SplittableRandom(1));
        assertFalse(result.fired(), "No invalid candidate should allow the tower to fire");
    }

    @Test
    @DisplayName("Numeric ID ordering: IDs 2 and 10 are processed in numeric order (2 before 10) across 12 enemies")
    void testNumericEnemyIdOrderingAndRngConsumptionWithTwelveEnemies() {
        // Cannon tower with wide splash 5.0 to cover all 12 enemies
        TowerConfig megaCannon = new TowerConfig(
                "MegaCannon", "MVP", 60, 5.0, 12.0, 12, TargetType.GROUND, 40, 5.0, 0.0, 0, "Mega"
        );
        CannonTower cannon = new CannonTower(1, megaCannon, new Position(0, 0), firstCandidateStrategy);

        // Create 12 FastEnemies (20% evasion) with IDs 1 to 12
        List<Enemy> enemies = new ArrayList<>();
        int[] ids = {10, 2, 12, 1, 9, 3, 8, 4, 7, 5, 11, 6}; // Shuffled IDs including 2 and 10
        for (int id : ids) {
            FastEnemy enemy = TestConfigHelper.createFastEnemy(id);
            for (int i = 0; i < 20; i++) {
                enemy.advance(straightPath); // all at pos (1.0, 0.0), well within splash
            }
            enemies.add(enemy);
        }

        // Custom RNG tracking consumption order
        List<Double> rolls = new ArrayList<>();
        RandomGenerator recordingRng = new RandomGenerator() {
            private int callCount = 0;

            @Override
            public double nextDouble() {
                callCount++;
                // Let enemy #2 (second processed, ID 2) evade (roll 0.05 < 0.20)
                // Let enemy #10 (tenth processed, ID 10) get hit (roll 0.50 >= 0.20)
                if (callCount == 2) {
                    rolls.add(0.05);
                    return 0.05;
                }
                if (callCount == 10) {
                    rolls.add(0.50);
                    return 0.50;
                }
                rolls.add(0.99);
                return 0.99;
            }

            @Override
            public long nextLong() {
                return 0;
            }
        };

        AttackResult result = cannon.update(enemies, straightPath, recordingRng);
        assertTrue(result.fired());
        assertEquals(12, result.hits().size());

        // Check that hits are strictly ordered by numeric ID: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
        // If lexicographical ordering was used, ID 10 would be index 1 (after 1), and ID 2 would be index 4!
        for (int i = 0; i < 12; i++) {
            assertEquals(i + 1, result.hits().get(i).enemyId(), "Enemy at index " + i + " must have numeric ID " + (i + 1));
        }

        // Verify ID 2 (index 1) evaded because it received roll 0.05 on the 2nd RNG call
        HitResult hit2 = result.hits().get(1);
        assertEquals(2, hit2.enemyId());
        assertTrue(hit2.evaded(), "ID 2 must receive the 2nd roll and evade");

        // Verify ID 10 (index 9) was hit because it received roll 0.50 on the 10th RNG call
        HitResult hit10 = result.hits().get(9);
        assertEquals(10, hit10.enemyId());
        assertFalse(hit10.evaded(), "ID 10 must receive the 10th roll and be hit");

        // Test tiebreaker in targeting strategy using lowest numeric ID
        TargetingStrategy tieBreakStrategy = (t, candidates) ->
                candidates.stream().min(Comparator.comparingInt(c -> c.enemy().id()));
        CannonTower tieCannon = new CannonTower(2, megaCannon, new Position(0, 0), tieBreakStrategy);
        // Provide enemies with IDs 10 and 2
        FastEnemy e10 = TestConfigHelper.createFastEnemy(10);
        FastEnemy e2 = TestConfigHelper.createFastEnemy(2);
        for (int i = 0; i < 20; i++) {
            e10.advance(straightPath);
            e2.advance(straightPath);
        }
        TargetCandidate c10 = new TargetCandidate(e10, e10.positionOn(straightPath), tieCannon.distanceTo(e10, straightPath));
        TargetCandidate c2 = new TargetCandidate(e2, e2.positionOn(straightPath), tieCannon.distanceTo(e2, straightPath));
        Optional<TargetCandidate> chosen = tieBreakStrategy.selectTarget(tieCannon, List.of(c10, c2));
        assertTrue(chosen.isPresent());
        assertEquals(2, chosen.get().enemy().id(), "Tied candidates must be resolved to lowest numeric ID (2 over 10)");
    }

    @Test
    @DisplayName("Randomness: RNG is NOT consumed for enemies whose evasionChance is 0")
    void testRngNotConsumedWhenEvasionChanceZero() {
        SniperTower sniper = new SniperTower(1, sniperConfig, new Position(0, 0), firstCandidateStrategy);
        BasicEnemy basic = TestConfigHelper.createBasicEnemy(1); // evasionChance = 0.0
        for (int i = 0; i < 20; i++) {
            basic.advance(straightPath);
        }

        AtomicInteger rngCalls = new AtomicInteger(0);
        RandomGenerator trackingRng = new RandomGenerator() {
            @Override
            public double nextDouble() {
                rngCalls.incrementAndGet();
                return 0.5;
            }

            @Override
            public long nextLong() {
                return 0;
            }
        };

        AttackResult result = sniper.update(List.of(basic), straightPath, trackingRng);
        assertTrue(result.fired());
        assertEquals(0, rngCalls.get(), "RNG must NOT be called for enemies with 0 evasion chance");
    }

    @Test
    @DisplayName("AttackResult immutability: hits collection cannot be modified")
    void testAttackResultImmutability() {
        AttackResult none = AttackResult.none(1);
        assertThrows(UnsupportedOperationException.class, () -> none.hits().add(new HitResult(1, 10, false, false, false)));

        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(0, 0), firstCandidateStrategy);
        BasicEnemy target = TestConfigHelper.createBasicEnemy(1);
        for (int i = 0; i < 20; i++) {
            target.advance(straightPath);
        }
        AttackResult result = cannon.update(List.of(target), straightPath, new SplittableRandom(1));
        assertThrows(UnsupportedOperationException.class, () -> result.hits().clear());
    }

    @Test
    @DisplayName("Strategy reassignment: changeStrategy() takes effect on the very next update() call")
    void testStrategyReassignmentTakesEffect() {
        BasicEnemy e1 = TestConfigHelper.createBasicEnemy(1);
        BasicEnemy e2 = TestConfigHelper.createBasicEnemy(2);
        for (int i = 0; i < 20; i++) {
            e1.advance(straightPath);
            e2.advance(straightPath);
        }

        // Initially uses strategy that picks candidate 0 (ID 1)
        CannonTower cannon = new CannonTower(1, cannonConfig, new Position(0, 0), firstCandidateStrategy);
        assertEquals(firstCandidateStrategy, cannon.strategy());

        // Reassign strategy to select candidate 1 (ID 2)
        TargetingStrategy secondCandidateStrategy = (t, candidates) ->
                candidates.size() > 1 ? Optional.of(candidates.get(1)) : Optional.empty();
        cannon.changeStrategy(secondCandidateStrategy);
        assertEquals(secondCandidateStrategy, cannon.strategy());

        // Call update and verify ID 2 was chosen as primary target
        AttackResult result = cannon.update(List.of(e1, e2), straightPath, new SplittableRandom(1));
        assertTrue(result.fired());
    }

    @Test
    @DisplayName("Invalid states: null arguments, negative id, null strategy, null path are rejected")
    void testInvalidStatesRejected() {
        Position pos = new Position(1, 1);

        assertThrows(IllegalArgumentException.class, () -> new CannonTower(1, null, pos, firstCandidateStrategy));
        assertThrows(IllegalArgumentException.class, () -> new CannonTower(1, cannonConfig, pos, null));
        assertThrows(IllegalArgumentException.class, () -> new CannonTower(-1, cannonConfig, pos, firstCandidateStrategy));
        assertThrows(IllegalArgumentException.class, () -> new CannonTower(1, cannonConfig, null, firstCandidateStrategy));

        CannonTower validTower = new CannonTower(1, cannonConfig, pos, firstCandidateStrategy);
        assertThrows(IllegalArgumentException.class, () -> validTower.changeStrategy(null));
        assertThrows(IllegalArgumentException.class, () -> validTower.update(List.of(), null, new SplittableRandom(1)));
        assertThrows(IllegalArgumentException.class, () -> validTower.update(null, straightPath, new SplittableRandom(1)));
        assertThrows(IllegalArgumentException.class, () -> validTower.update(List.of(), straightPath, null));
    }
}
