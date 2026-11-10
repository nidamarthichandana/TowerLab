package com.towerlab.engine.model;

import com.towerlab.engine.config.EnemyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnemyTest {

    // 18-cell path from GR-8: (0,1) -> (6,1) -> (6,5) -> (10,5) -> (10,2) -> (11,2)
    private final Path examplePath = new Path(List.of(
            new Position(0, 1),
            new Position(6, 1),
            new Position(6, 5),
            new Position(10, 5),
            new Position(10, 2),
            new Position(11, 2)
    ));

    @Test
    @DisplayName("Basic enemy has 40 HP per GR-Table 5")
    void testBasicEnemyHas40Hp() {
        BasicEnemy basic = new BasicEnemy(1);
        assertEquals(1, basic.id());
        assertEquals(40, basic.currentHealth());
        assertEquals(40, basic.maxHealth());
        assertTrue(basic.isAlive());
        assertEquals(0.0, basic.evasionChance());
    }

    @Test
    @DisplayName("Armored enemy takes round(12 x 0.6) = 7 damage with raw damage 12 per GR-18")
    void testArmoredEnemyDamageCalculation() {
        ArmoredEnemy armored = new ArmoredEnemy(2);
        assertEquals(2, armored.id());
        assertEquals(120, armored.currentHealth());

        // Raw damage 12 with 40% armour: round(12 * (1 - 0.40)) = round(7.2) = 7
        int damageDealt = armored.takeDamage(12);
        assertEquals(7, damageDealt);
        assertEquals(113, armored.currentHealth());
    }

    @Test
    @DisplayName("Minimum damage is 1 per GR-18")
    void testMinimumDamageIsOne() {
        ArmoredEnemy armored = new ArmoredEnemy(2);

        // Raw damage 1 with 40% armour: round(1 * 0.6) = round(0.6) = 1 (max(1, 1) = 1)
        int damageDealt = armored.takeDamage(1);
        assertEquals(1, damageDealt);
        assertEquals(119, armored.currentHealth());

        // Extreme armour enemy (e.g. 95% armour): round(1 * 0.05) = 0 -> minimum damage is 1
        Enemy highArmour = new BasicEnemy(3, new EnemyConfig("Tank", "MVP", 100.0, 0.05, 0.95, 10, 1, 0.0));
        int minDmg = highArmour.takeDamage(1);
        assertEquals(1, minDmg);
        assertEquals(99, highArmour.currentHealth());
    }

    @Test
    @DisplayName("Damage exceeding remaining health returns actual health lost and sets health to 0")
    void testExcessDamageReturnsActualHealthLost() {
        EnemyConfig lowHpConfig = new EnemyConfig("LowHP", 3.0, 0.05, 5, 1);
        BasicEnemy enemy = new BasicEnemy(4, lowHpConfig);
        assertEquals(3, enemy.currentHealth());

        // Enemy with 3 HP receiving 7 calculated damage loses 3 HP and method returns 3
        int damageDealt = enemy.takeDamage(7);
        assertEquals(3, damageDealt);
        assertEquals(0, enemy.currentHealth());
        assertFalse(enemy.isAlive());
    }

    @Test
    @DisplayName("Excess lethal true damage returns actual health lost and sets health to 0")
    void testExcessLethalTrueDamageReturnsActualHealthLost() {
        EnemyConfig lowHpConfig = new EnemyConfig("LowHP", 2.0, 0.05, 5, 1);
        BasicEnemy enemy = new BasicEnemy(5, lowHpConfig);
        assertEquals(2, enemy.currentHealth());

        // Enemy with 2 HP receiving 5 true damage loses 2 HP and method returns 2
        int trueDamageDealt = enemy.takeTrueDamage(5);
        assertEquals(2, trueDamageDealt);
        assertEquals(0, enemy.currentHealth());
        assertFalse(enemy.isAlive());
    }

    @Test
    @DisplayName("Already-dead enemy takes 0 damage and 0 true damage")
    void testAlreadyDeadEnemyTakesZeroDamage() {
        BasicEnemy basic = new BasicEnemy(1);
        basic.takeDamage(100);
        assertEquals(0, basic.currentHealth());
        assertFalse(basic.isAlive());

        assertEquals(0, basic.takeDamage(10));
        assertEquals(0, basic.takeTrueDamage(5));
        assertEquals(0, basic.currentHealth());
    }

    @Test
    @DisplayName("Dead enemy ignores status effects and tickEffects does nothing")
    void testDeadEnemyIgnoresStatusEffectsAndTickEffects() {
        BasicEnemy basic = new BasicEnemy(1);
        basic.takeTrueDamage(40);
        assertFalse(basic.isAlive());

        basic.applyEffect(new SlowEffect(10, 0.50, 30));
        basic.applyEffect(new BurnEffect(10, 1, 5, 40));
        assertTrue(basic.activeEffects().isEmpty(), "Dead enemy must not receive status effects");

        // tickEffects should do nothing and return empty list on dead enemy
        List<EffectDamage> damages = basic.tickEffects();
        assertTrue(damages.isEmpty());
        assertEquals(0, basic.currentHealth());
    }

    @Test
    @DisplayName("Burn ignores armour per GR-18 and deals true damage")
    void testBurnIgnoresArmour() {
        ArmoredEnemy armored = new ArmoredEnemy(2); // 40% armour
        int trueDamage = armored.takeTrueDamage(10);
        assertEquals(10, trueDamage, "True damage must not be reduced by armour");
        assertEquals(110, armored.currentHealth());

        // Apply BurnEffect (1 damage every 5 ticks for 40 ticks per GR-22)
        BurnEffect burn = new BurnEffect(10, 1, 5, 40);
        armored.applyEffect(burn);

        // Advance 5 ticks: tick 5 should apply 1 true damage ignoring 40% armour
        for (int i = 0; i < 5; i++) {
            armored.tickEffects();
        }
        assertEquals(109, armored.currentHealth());
    }

    @Test
    @DisplayName("Burn deals 1 damage at 5, 10, ..., 40 ticks (8 procs total) and expires after 40 per GR-22")
    void testBurnTimingAndProcCount() {
        BasicEnemy basic = new BasicEnemy(1); // 40 HP
        basic.applyEffect(new BurnEffect(77, 1, 5, 40));

        // Ticks 1 to 4: no damage
        for (int t = 1; t <= 4; t++) {
            List<EffectDamage> damages = basic.tickEffects();
            assertTrue(damages.isEmpty(), "No damage events before tick 5");
            assertEquals(40, basic.currentHealth(), "No burn damage should be dealt before tick 5");
        }

        // Tick 5: 1st damage proc
        List<EffectDamage> proc1 = basic.tickEffects();
        assertEquals(1, proc1.size());
        assertEquals(77, proc1.get(0).sourceTowerId());
        assertEquals(1, proc1.get(0).amount());
        assertEquals(39, basic.currentHealth(), "1st burn proc at tick 5");

        // Ticks 6 to 40: procs at 10, 15, 20, 25, 30, 35, 40
        int expectedHealth = 39;
        for (int t = 6; t <= 40; t++) {
            List<EffectDamage> damages = basic.tickEffects();
            if (t % 5 == 0) {
                expectedHealth--;
                assertEquals(1, damages.size());
                assertEquals(77, damages.get(0).sourceTowerId());
                assertEquals(1, damages.get(0).amount());
                assertEquals(expectedHealth, basic.currentHealth(), "Burn proc at tick " + t);
            } else {
                assertTrue(damages.isEmpty());
                assertEquals(expectedHealth, basic.currentHealth(), "No burn proc at tick " + t);
            }
        }

        // 8 procs total dealt: 40 - 8 = 32 HP
        assertEquals(32, basic.currentHealth());
        assertTrue(basic.activeEffects().isEmpty(), "Burn effect must expire after 40 ticks");

        // Tick 41: no further damage
        List<EffectDamage> postExpiry = basic.tickEffects();
        assertTrue(postExpiry.isEmpty());
        assertEquals(32, basic.currentHealth());
    }

    @Test
    @DisplayName("Burn reapplication refreshes duration to 40 without resetting interval counter or stacking per GR-22")
    void testBurnReapplicationPreservesIntervalPhase() {
        BasicEnemy basic = new BasicEnemy(1);
        basic.applyEffect(new BurnEffect(10, 1, 5, 40));

        // Tick 1 to 5: 1 proc on tick 5 (HP 39)
        for (int t = 1; t <= 5; t++) {
            basic.tickEffects();
        }
        assertEquals(39, basic.currentHealth());

        // Tick 6 and 7: 2 ticks elapse since last proc (HP 39)
        basic.tickEffects();
        basic.tickEffects();
        assertEquals(39, basic.currentHealth());

        // At tick 7, reapply burn
        basic.applyEffect(new BurnEffect(10, 1, 5, 40));
        assertEquals(1, basic.activeEffects().size(), "Burn effects must not stack");

        BurnEffect activeBurn = (BurnEffect) basic.activeEffects().iterator().next();
        assertEquals(40, activeBurn.remainingTicks(), "Burn reapplication refreshes remaining duration to 40");

        // Ticks 8 and 9 (ticks 3 and 4 since tick 5): no damage
        basic.tickEffects();
        assertEquals(39, basic.currentHealth());
        basic.tickEffects();
        assertEquals(39, basic.currentHealth());

        // Tick 10 (5 ticks since tick 5): damage MUST proc because interval was not reset!
        basic.tickEffects();
        assertEquals(38, basic.currentHealth(), "Damage must proc at tick 10 preserving the 5-tick interval");
    }

    @Test
    @DisplayName("Slow rules per GR-21: weaker slow ignored, equal refreshes duration, stronger replaces")
    void testSlowRulesWeakerIgnoredEqualRefreshesStrongerReplaces() {
        BasicEnemy basic = new BasicEnemy(1);

        // Apply 60% slow for 30 ticks from tower 10
        SlowEffect slow60 = new SlowEffect(10, 0.60, 30);
        basic.applyEffect(slow60);
        assertEquals(1, basic.activeEffects().size());

        // Advance 10 ticks
        for (int i = 0; i < 10; i++) {
            basic.tickEffects();
        }
        assertEquals(20, slow60.remainingTicks());

        // Apply weaker slow (50% slow): must be ignored entirely
        basic.applyEffect(new SlowEffect(11, 0.50, 30));
        assertEquals(1, basic.activeEffects().size());
        SlowEffect currentSlow = (SlowEffect) basic.activeEffects().iterator().next();
        assertEquals(0.60, currentSlow.slowFraction(), 1e-6);
        assertEquals(20, currentSlow.remainingTicks(), "Weaker slow must be ignored and not refresh duration");
        assertEquals(10, currentSlow.sourceTowerId(), "Source tower ID must remain from original stronger slow");

        // Apply equal slow (60% slow): refreshes duration to 30 ticks
        basic.applyEffect(new SlowEffect(12, 0.60, 30));
        assertEquals(1, basic.activeEffects().size());
        currentSlow = (SlowEffect) basic.activeEffects().iterator().next();
        assertEquals(0.60, currentSlow.slowFraction(), 1e-6);
        assertEquals(30, currentSlow.remainingTicks(), "Equal slow must refresh duration to 30");

        // Advance 5 ticks
        for (int i = 0; i < 5; i++) {
            basic.tickEffects();
        }
        assertEquals(25, currentSlow.remainingTicks());

        // Apply stronger slow (75% slow): replaces the existing slow
        SlowEffect slow75 = new SlowEffect(13, 0.75, 40);
        basic.applyEffect(slow75);
        assertEquals(1, basic.activeEffects().size());
        currentSlow = (SlowEffect) basic.activeEffects().iterator().next();
        assertEquals(0.75, currentSlow.slowFraction(), 1e-6);
        assertEquals(40, currentSlow.remainingTicks(), "Stronger slow must replace existing slow");
        assertEquals(13, currentSlow.sourceTowerId(), "Source tower ID must update to replacing slow");
    }

    @Test
    @DisplayName("Slow expiry restores full movement speed on the exact tick per GR-21")
    void testSlowExpiryRestoresFullSpeedOnExactTick() {
        BasicEnemy basic = new BasicEnemy(1); // speed 0.05
        SlowEffect slow = new SlowEffect(10, 0.50, 2); // 50% slow for 2 ticks
        basic.applyEffect(slow);

        // Tick 1: under slow (speed 0.05 * 0.5 = 0.025)
        basic.advance(examplePath);
        basic.tickEffects();
        assertEquals(0.025, basic.progress(), 1e-6);
        assertFalse(slow.isExpired());

        // Tick 2: under slow (speed 0.05 * 0.5 = 0.025)
        basic.advance(examplePath);
        basic.tickEffects();
        assertEquals(0.050, basic.progress(), 1e-6);
        assertTrue(slow.isExpired());
        assertTrue(basic.activeEffects().isEmpty(), "Slow effect must be removed after expiry");

        // Tick 3: full speed (0.05)
        basic.advance(examplePath);
        basic.tickEffects();
        assertEquals(0.100, basic.progress(), 1e-6, "Enemy must move at full speed after slow expires");
    }

    @Test
    @DisplayName("Basic enemy at 0.05 cells/tick covers an 18-cell path in 360 ticks per GR-1 & GR-8")
    void testBasicEnemyTraverses18CellPathIn360Ticks() {
        BasicEnemy basic = new BasicEnemy(1);
        assertEquals(18.0, examplePath.totalLength(), 1e-6);

        assertFalse(basic.hasReachedEnd());
        assertEquals(0.0, basic.progress(), 1e-6);

        // Advance for 360 ticks: 360 * 0.05 = 18.0 cells
        for (int i = 0; i < 360; i++) {
            basic.advance(examplePath);
        }

        assertEquals(18.0, basic.progress(), 1e-6);
        assertTrue(basic.hasReachedEnd());
        assertTrue(basic.hasReachedEnd(examplePath));

        ContinuousPosition destination = basic.position(examplePath);
        assertEquals(11.0, destination.x(), 1e-6);
        assertEquals(2.0, destination.y(), 1e-6);
        ContinuousPosition destinationOn = basic.positionOn(examplePath);
        assertEquals(11.0, destinationOn.x(), 1e-6);
        assertEquals(2.0, destinationOn.y(), 1e-6);
    }

    @Test
    @DisplayName("Fast enemy has 25 HP, speed 0.10, and 20% evasion chance per GR-Table 5")
    void testFastEnemyAttributes() {
        FastEnemy fast = new FastEnemy(1);
        assertEquals(25, fast.currentHealth());
        assertEquals(0.10, fast.config().speed(), 1e-6);
        assertEquals(0.20, fast.evasionChance(), 1e-6);
    }

    @Test
    @DisplayName("Invalid state and arguments are rejected")
    void testInvalidStateRejected() {
        // Negative id
        assertThrows(IllegalArgumentException.class, () -> new BasicEnemy(-1));

        // Non-positive health
        EnemyConfig invalidHealth = new EnemyConfig("Bad", -10.0, 0.05, 5, 1);
        assertThrows(IllegalArgumentException.class, () -> new BasicEnemy(1, invalidHealth));

        // Non-positive damage
        BasicEnemy basic = new BasicEnemy(1);
        assertThrows(IllegalArgumentException.class, () -> basic.takeDamage(0));
        assertThrows(IllegalArgumentException.class, () -> basic.takeDamage(-5));
        assertThrows(IllegalArgumentException.class, () -> basic.takeTrueDamage(0));
        assertThrows(IllegalArgumentException.class, () -> basic.takeTrueDamage(-3));

        // Null path or effect
        assertThrows(IllegalArgumentException.class, () -> basic.advance(null));
        assertThrows(IllegalArgumentException.class, () -> basic.applyEffect(null));

        // Invalid Path waypoints
        assertThrows(IllegalArgumentException.class, () -> new Path((List<Position>) null));
        assertThrows(IllegalArgumentException.class, () -> new Path(List.of(new Position(0, 0))));

        // Invalid SlowEffect / BurnEffect parameters
        assertThrows(IllegalArgumentException.class, () -> new SlowEffect(1, 0.0, 30));
        assertThrows(IllegalArgumentException.class, () -> new SlowEffect(1, 1.0, 30));
        assertThrows(IllegalArgumentException.class, () -> new SlowEffect(1, 0.5, 0));
        assertThrows(IllegalArgumentException.class, () -> new BurnEffect(1, 0, 5, 40));
        assertThrows(IllegalArgumentException.class, () -> new BurnEffect(1, 1, 0, 40));
        assertThrows(IllegalArgumentException.class, () -> new BurnEffect(1, 1, 5, 0));
    }
}
