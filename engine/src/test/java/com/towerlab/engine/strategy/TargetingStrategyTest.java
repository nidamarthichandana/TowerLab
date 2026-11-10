package com.towerlab.engine.strategy;

import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.config.TargetType;
import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.model.AttackResult;
import com.towerlab.engine.model.BasicEnemy;
import com.towerlab.engine.model.CannonTower;
import com.towerlab.engine.model.ContinuousPosition;
import com.towerlab.engine.model.Enemy;
import com.towerlab.engine.model.Path;
import com.towerlab.engine.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetingStrategyTest {

    private final Path straightPath = new Path(List.of(
            new Position(0, 0),
            new Position(50, 0)
    ));

    private final TowerConfig cannonConfig = new TowerConfig(
            "Cannon", "MVP", 60, 2.5, 12.0, 12, TargetType.GROUND, 40, 1.0, 0.0, 0,
            "Splash: enemies within 1.0 cells of the target take full damage"
    );

    private BasicEnemy createEnemyWithProgress(int id, double targetProgress, double health) {
        EnemyConfig config = new EnemyConfig("Enemy_" + id, health, targetProgress, 8, 1);
        BasicEnemy enemy = new BasicEnemy(id, config);
        enemy.advance(straightPath);
        return enemy;
    }

    @Test
    @DisplayName("Each strategy picks the expected enemy among 4 enemies with different progress, health, and distance")
    void testEachStrategyPicksExpectedEnemyAmongFour() {
        CannonTower tower = new CannonTower(1, cannonConfig, new Position(0, 0), TargetingMode.FIRST.strategy());

        // Enemy 1: Furthest along path (progress 18.0), HP 30, distance 3.5
        BasicEnemy e1 = createEnemyWithProgress(1, 18.0, 30.0);
        TargetCandidate c1 = new TargetCandidate(e1, e1.positionOn(straightPath), 3.5);

        // Enemy 2: Least progress (progress 1.0), HP 40, distance 4.0
        BasicEnemy e2 = createEnemyWithProgress(2, 1.0, 40.0);
        TargetCandidate c2 = new TargetCandidate(e2, e2.positionOn(straightPath), 4.0);

        // Enemy 3: Highest health (progress 8.0), HP 120, distance 3.0
        BasicEnemy e3 = createEnemyWithProgress(3, 8.0, 120.0);
        TargetCandidate c3 = new TargetCandidate(e3, e3.positionOn(straightPath), 3.0);

        // Enemy 4: Closest to tower (progress 10.0), HP 50, distance 1.2
        BasicEnemy e4 = createEnemyWithProgress(4, 10.0, 50.0);
        TargetCandidate c4 = new TargetCandidate(e4, e4.positionOn(straightPath), 1.2);

        List<TargetCandidate> candidates = List.of(c1, c2, c3, c4);

        // FirstTargeting -> picks e1 (highest progress = 18.0)
        Optional<TargetCandidate> firstTarget = new FirstTargeting().selectTarget(tower, candidates);
        assertTrue(firstTarget.isPresent());
        assertEquals(e1.id(), firstTarget.get().enemy().id());

        // LastTargeting -> picks e2 (lowest progress = 1.0)
        Optional<TargetCandidate> lastTarget = new LastTargeting().selectTarget(tower, candidates);
        assertTrue(lastTarget.isPresent());
        assertEquals(e2.id(), lastTarget.get().enemy().id());

        // StrongestTargeting -> picks e3 (highest current health = 120)
        Optional<TargetCandidate> strongestTarget = new StrongestTargeting().selectTarget(tower, candidates);
        assertTrue(strongestTarget.isPresent());
        assertEquals(e3.id(), strongestTarget.get().enemy().id());

        // ClosestTargeting -> picks e4 (smallest distance = 1.2)
        Optional<TargetCandidate> closestTarget = new ClosestTargeting().selectTarget(tower, candidates);
        assertTrue(closestTarget.isPresent());
        assertEquals(e4.id(), closestTarget.get().enemy().id());
    }

    @Test
    @DisplayName("Strongest uses current health: an enemy with higher max health but damaged below another loses")
    void testStrongestUsesCurrentHealthNotMaxHealth() {
        CannonTower tower = new CannonTower(1, cannonConfig, new Position(0, 0), TargetingMode.STRONGEST.strategy());

        // High max health (120), but damaged down to 30
        BasicEnemy highMaxEnemy = createEnemyWithProgress(1, 5.0, 120.0);
        highMaxEnemy.takeDamage(90);
        assertEquals(120, highMaxEnemy.maxHealth());
        assertEquals(30, highMaxEnemy.currentHealth());

        // Moderate max health (50), undamaged at 50
        BasicEnemy moderateEnemy = createEnemyWithProgress(2, 5.0, 50.0);
        assertEquals(50, moderateEnemy.maxHealth());
        assertEquals(50, moderateEnemy.currentHealth());

        TargetCandidate cHighMax = new TargetCandidate(highMaxEnemy, highMaxEnemy.positionOn(straightPath), 2.0);
        TargetCandidate cModerate = new TargetCandidate(moderateEnemy, moderateEnemy.positionOn(straightPath), 2.0);

        List<TargetCandidate> candidates = List.of(cHighMax, cModerate);

        Optional<TargetCandidate> chosen = new StrongestTargeting().selectTarget(tower, candidates);
        assertTrue(chosen.isPresent());
        assertEquals(2, chosen.get().enemy().id(), "Candidate with higher current health (50 > 30) must be chosen");
    }

    @Test
    @DisplayName("Tie-breaking with IDs 2 and 10 picks 2 (numeric, not text order) for every strategy")
    void testTieBreakingWithIds2And10Picks2ForEveryStrategy() {
        CannonTower tower = new CannonTower(1, cannonConfig, new Position(0, 0), TargetingMode.FIRST.strategy());

        // Two enemies with identical progress, health, and distance
        BasicEnemy e2 = createEnemyWithProgress(2, 5.0, 40.0);
        BasicEnemy e10 = createEnemyWithProgress(10, 5.0, 40.0);

        ContinuousPosition pos = new ContinuousPosition(5.0, 0.0);
        TargetCandidate c2 = new TargetCandidate(e2, pos, 2.0);
        TargetCandidate c10 = new TargetCandidate(e10, pos, 2.0);

        List<TargetingStrategy> strategies = List.of(
                new FirstTargeting(),
                new LastTargeting(),
                new StrongestTargeting(),
                new ClosestTargeting()
        );

        for (TargetingStrategy strategy : strategies) {
            // Test [c10, c2] order
            Optional<TargetCandidate> chosen1 = strategy.selectTarget(tower, List.of(c10, c2));
            assertTrue(chosen1.isPresent());
            assertEquals(2, chosen1.get().enemy().id(),
                    strategy.getClass().getSimpleName() + " must pick numeric ID 2 over 10 (order [c10, c2])");

            // Test [c2, c10] order
            Optional<TargetCandidate> chosen2 = strategy.selectTarget(tower, List.of(c2, c10));
            assertTrue(chosen2.isPresent());
            assertEquals(2, chosen2.get().enemy().id(),
                    strategy.getClass().getSimpleName() + " must pick numeric ID 2 over 10 (order [c2, c10])");
        }
    }

    @Test
    @DisplayName("Shuffling the input list gives the same result")
    void testShufflingInputListGivesSameResult() {
        CannonTower tower = new CannonTower(1, cannonConfig, new Position(0, 0), TargetingMode.FIRST.strategy());

        // 5 candidates with varied progress, health, distance, and IDs
        BasicEnemy e1 = createEnemyWithProgress(1, 10.0, 50.0);
        BasicEnemy e2 = createEnemyWithProgress(2, 10.0, 50.0); // tied with e1 on progress, e1 wins by ID
        BasicEnemy e3 = createEnemyWithProgress(3, 5.0, 80.0);
        BasicEnemy e4 = createEnemyWithProgress(4, 2.0, 20.0);
        BasicEnemy e5 = createEnemyWithProgress(5, 7.0, 60.0);

        TargetCandidate c1 = new TargetCandidate(e1, e1.positionOn(straightPath), 3.0);
        TargetCandidate c2 = new TargetCandidate(e2, e2.positionOn(straightPath), 3.0);
        TargetCandidate c3 = new TargetCandidate(e3, e3.positionOn(straightPath), 1.0);
        TargetCandidate c4 = new TargetCandidate(e4, e4.positionOn(straightPath), 4.0);
        TargetCandidate c5 = new TargetCandidate(e5, e5.positionOn(straightPath), 2.0);

        List<TargetCandidate> baseList = List.of(c1, c2, c3, c4, c5);

        List<TargetingStrategy> strategies = List.of(
                new FirstTargeting(),
                new LastTargeting(),
                new StrongestTargeting(),
                new ClosestTargeting()
        );

        for (TargetingStrategy strategy : strategies) {
            Optional<TargetCandidate> reference = strategy.selectTarget(tower, baseList);
            assertTrue(reference.isPresent());
            int expectedId = reference.get().enemy().id();

            Random rng = new Random(42);
            for (int i = 0; i < 20; i++) {
                List<TargetCandidate> shuffled = new ArrayList<>(baseList);
                Collections.shuffle(shuffled, rng);
                Optional<TargetCandidate> result = strategy.selectTarget(tower, shuffled);
                assertTrue(result.isPresent());
                assertEquals(expectedId, result.get().enemy().id(),
                        strategy.getClass().getSimpleName() + " must produce invariant result under shuffling");
            }
        }
    }

    @Test
    @DisplayName("Empty list gives Optional.empty()")
    void testEmptyListGivesOptionalEmpty() {
        CannonTower tower = new CannonTower(1, cannonConfig, new Position(0, 0), TargetingMode.FIRST.strategy());

        List<TargetingStrategy> strategies = List.of(
                new FirstTargeting(),
                new LastTargeting(),
                new StrongestTargeting(),
                new ClosestTargeting()
        );

        for (TargetingStrategy strategy : strategies) {
            assertEquals(Optional.empty(), strategy.selectTarget(tower, List.of()),
                    strategy.getClass().getSimpleName() + " must return empty Optional for empty candidates");
        }
    }

    @Test
    @DisplayName("Null arguments are rejected by strategies and TargetCandidate")
    void testNullArgumentsRejected() {
        CannonTower tower = new CannonTower(1, cannonConfig, new Position(0, 0), TargetingMode.FIRST.strategy());
        BasicEnemy enemy = createEnemyWithProgress(1, 1.0, 40.0);
        ContinuousPosition pos = new ContinuousPosition(1.0, 0.0);
        TargetCandidate validCandidate = new TargetCandidate(enemy, pos, 1.0);

        List<TargetingStrategy> strategies = List.of(
                new FirstTargeting(),
                new LastTargeting(),
                new StrongestTargeting(),
                new ClosestTargeting()
        );

        for (TargetingStrategy strategy : strategies) {
            assertThrows(IllegalArgumentException.class, () -> strategy.selectTarget(null, List.of(validCandidate)),
                    strategy.getClass().getSimpleName() + " must reject null tower");
            assertThrows(IllegalArgumentException.class, () -> strategy.selectTarget(tower, null),
                    strategy.getClass().getSimpleName() + " must reject null candidates list");
            assertThrows(IllegalArgumentException.class, () -> strategy.selectTarget(tower, Collections.singletonList(null)),
                    strategy.getClass().getSimpleName() + " must reject null candidate in list");
        }

        // TargetCandidate constructor validations
        assertThrows(IllegalArgumentException.class, () -> new TargetCandidate(null, pos, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new TargetCandidate(enemy, null, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new TargetCandidate(enemy, pos, -0.01));
    }

    @Test
    @DisplayName("TargetingMode maps each value to the right strategy class, and fromString rejects invalid values")
    void testTargetingModeMappingsAndFromString() {
        assertInstanceOf(FirstTargeting.class, TargetingMode.FIRST.strategy());
        assertInstanceOf(LastTargeting.class, TargetingMode.LAST.strategy());
        assertInstanceOf(StrongestTargeting.class, TargetingMode.STRONGEST.strategy());
        assertInstanceOf(ClosestTargeting.class, TargetingMode.CLOSEST.strategy());

        assertEquals(TargetingMode.FIRST, TargetingMode.fromString("FIRST"));
        assertEquals(TargetingMode.FIRST, TargetingMode.fromString("first"));
        assertEquals(TargetingMode.FIRST, TargetingMode.fromString(" First "));

        assertEquals(TargetingMode.LAST, TargetingMode.fromString("LAST"));
        assertEquals(TargetingMode.LAST, TargetingMode.fromString("last"));

        assertEquals(TargetingMode.STRONGEST, TargetingMode.fromString("STRONGEST"));
        assertEquals(TargetingMode.STRONGEST, TargetingMode.fromString("strongest"));

        assertEquals(TargetingMode.CLOSEST, TargetingMode.fromString("CLOSEST"));
        assertEquals(TargetingMode.CLOSEST, TargetingMode.fromString("closest"));

        assertThrows(IllegalArgumentException.class, () -> TargetingMode.fromString("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> TargetingMode.fromString("RANDOM"));
        assertThrows(IllegalArgumentException.class, () -> TargetingMode.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> TargetingMode.fromString("   "));
        assertThrows(IllegalArgumentException.class, () -> TargetingMode.fromString(null));
    }

    @Test
    @DisplayName("Integration: Cannon with each mode attacks the expected enemy in a three-enemy scene")
    void testIntegrationCannonWithEachModeAttacksExpectedEnemyInThreeEnemyScene() {
        // Path along x-axis from (0, 0) to (20, 0)
        Path path = new Path(List.of(new Position(0, 0), new Position(20, 0)));
        Position towerPos = new Position(2, 1);
        RandomGenerator rng = new SplittableRandom(1);

        // Three ground enemies on the path:
        // Enemy A (id 1): at (3.5, 0). Progress = 3.5. Health = 30. Distance to (2, 1) = hypot(1.5, 1) = ~1.803 <= 2.5
        // Enemy B (id 2): at (0.5, 0). Progress = 0.5. Health = 40. Distance to (2, 1) = hypot(1.5, 1) = ~1.803 <= 2.5
        // Enemy C (id 3): at (2.0, 0). Progress = 2.0. Health = 100. Distance to (2, 1) = hypot(0, 1) = 1.0 <= 2.5
        // Note: enemies are 1.5 cells apart, well outside Cannon splash radius (1.0), so each attack hits only the target.

        // 1. FIRST mode -> attacks Enemy A (highest progress: 3.5)
        CannonTower cannonFirst = new CannonTower(1, cannonConfig, towerPos, TargetingMode.FIRST.strategy());
        BasicEnemy eA1 = createEnemyWithProgress(1, 3.5, 30.0);
        BasicEnemy eB1 = createEnemyWithProgress(2, 0.5, 40.0);
        BasicEnemy eC1 = createEnemyWithProgress(3, 2.0, 100.0);
        AttackResult resFirst = cannonFirst.update(List.of(eA1, eB1, eC1), path, rng);
        assertTrue(resFirst.fired());
        assertEquals(1, resFirst.hits().size());
        assertEquals(eA1.id(), resFirst.hits().get(0).enemyId(), "FIRST mode must target Enemy A (highest progress)");

        // 2. LAST mode -> attacks Enemy B (lowest progress: 0.5)
        CannonTower cannonLast = new CannonTower(2, cannonConfig, towerPos, TargetingMode.LAST.strategy());
        BasicEnemy eA2 = createEnemyWithProgress(1, 3.5, 30.0);
        BasicEnemy eB2 = createEnemyWithProgress(2, 0.5, 40.0);
        BasicEnemy eC2 = createEnemyWithProgress(3, 2.0, 100.0);
        AttackResult resLast = cannonLast.update(List.of(eA2, eB2, eC2), path, rng);
        assertTrue(resLast.fired());
        assertEquals(1, resLast.hits().size());
        assertEquals(eB2.id(), resLast.hits().get(0).enemyId(), "LAST mode must target Enemy B (lowest progress)");

        // 3. STRONGEST mode -> attacks Enemy C (highest current health: 100)
        CannonTower cannonStrongest = new CannonTower(3, cannonConfig, towerPos, TargetingMode.STRONGEST.strategy());
        BasicEnemy eA3 = createEnemyWithProgress(1, 3.5, 30.0);
        BasicEnemy eB3 = createEnemyWithProgress(2, 0.5, 40.0);
        BasicEnemy eC3 = createEnemyWithProgress(3, 2.0, 100.0);
        AttackResult resStrongest = cannonStrongest.update(List.of(eA3, eB3, eC3), path, rng);
        assertTrue(resStrongest.fired());
        assertEquals(1, resStrongest.hits().size());
        assertEquals(eC3.id(), resStrongest.hits().get(0).enemyId(), "STRONGEST mode must target Enemy C (highest health)");

        // 4. CLOSEST mode -> attacks Enemy C (smallest distance: 1.0)
        CannonTower cannonClosest = new CannonTower(4, cannonConfig, towerPos, TargetingMode.CLOSEST.strategy());
        BasicEnemy eA4 = createEnemyWithProgress(1, 3.5, 30.0);
        BasicEnemy eB4 = createEnemyWithProgress(2, 0.5, 40.0);
        BasicEnemy eC4 = createEnemyWithProgress(3, 2.0, 100.0);
        AttackResult resClosest = cannonClosest.update(List.of(eA4, eB4, eC4), path, rng);
        assertTrue(resClosest.fired());
        assertEquals(1, resClosest.hits().size());
        assertEquals(eC4.id(), resClosest.hits().get(0).enemyId(), "CLOSEST mode must target Enemy C (closest to tower)");

        // 5. Dynamic strategy change via changeStrategy
        cannonFirst.changeStrategy(TargetingMode.LAST.strategy());
        assertEquals(TargetingMode.LAST.strategy(), cannonFirst.strategy());
    }
}
