package com.towerlab.engine.analytics;

import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.event.EnemyKilled;
import com.towerlab.engine.event.LifeLost;
import com.towerlab.engine.event.TowerBuilt;
import com.towerlab.engine.event.TowerFired;
import com.towerlab.engine.event.TowerMissed;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.simulation.Outcome;
import com.towerlab.engine.simulation.SimulationResult;
import com.towerlab.engine.test.TestConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsCalculatorTest {

    private final GameConfig frozenConfig = TestConfigHelper.getFrozenGameConfig();

    @Test
    @DisplayName("Hand-built SimulationResult: known totals for damage, kills, leakage, and goldSpent")
    void testHandBuiltMetricsTotals() {
        SimulationResult result = new SimulationResult(
                Outcome.WIN,
                15,
                120,
                50,
                List.of(
                        new TowerBuilt(0, 1, "Cannon", 2, 2, 90),
                        new TowerBuilt(0, 2, "Sniper", 4, 4, 0),
                        new TowerFired(10, 1, "Cannon", 101, 12),
                        new TowerFired(10, 1, "Cannon", 102, 7),
                        new TowerFired(22, 1, "Cannon", 103, 12),
                        new TowerFired(15, 2, "Sniper", 104, 40),
                        new EnemyKilled(15, 104, "Basic", 2, 8, 8),
                        new EnemyKilled(22, 103, "Basic", 1, 8, 16),
                        new LifeLost(30, 105, 1, 19),
                        new LifeLost(40, 106, 1, 18)
                )
        );

        RunMetrics metrics = MetricsCalculator.calculate(result, frozenConfig);

        assertNotNull(metrics);
        assertEquals(Outcome.WIN, metrics.outcome());
        assertEquals(50, metrics.durationTicks());
        assertEquals(15, metrics.baseHealthRemaining());
        assertEquals(71, metrics.damageDealt()); // 12 + 7 + 12 + 40
        assertEquals(2, metrics.enemiesDefeated());
        assertEquals(2, metrics.leakage());

        // Cannon: cost 60, cooldown 12
        TowerTypeMetrics cannonMetrics = metrics.towerMetrics().get("Cannon");
        assertNotNull(cannonMetrics);
        assertEquals(31, cannonMetrics.damage());
        assertEquals(60, cannonMetrics.goldSpent()); // 1 built * 60
        assertEquals(2, cannonMetrics.shots()); // ticks 10 and 22
        assertEquals(5, cannonMetrics.maxShots()); // ceil(50 / 12.0) = 5

        // Sniper: cost 90, cooldown 40
        TowerTypeMetrics sniperMetrics = metrics.towerMetrics().get("Sniper");
        assertNotNull(sniperMetrics);
        assertEquals(40, sniperMetrics.damage());
        assertEquals(90, sniperMetrics.goldSpent()); // 1 built * 90
        assertEquals(1, sniperMetrics.shots()); // tick 15
        assertEquals(2, sniperMetrics.maxShots()); // ceil(50 / 40.0) = 2
    }

    @Test
    @DisplayName("Two TowerFired events from one cannon blast on the same tick count as ONE shot")
    void testTwoTowerFiredOnSameTickCountAsOneShot() {
        SimulationResult result = new SimulationResult(
                Outcome.WIN,
                20,
                150,
                25,
                List.of(
                        new TowerBuilt(0, 1, "Cannon", 2, 2, 90),
                        new TowerFired(12, 1, "Cannon", 10, 12),
                        new TowerFired(12, 1, "Cannon", 11, 7),
                        new TowerFired(12, 1, "Cannon", 12, 7)
                )
        );

        RunMetrics metrics = MetricsCalculator.calculate(result, frozenConfig);
        TowerTypeMetrics cannonMetrics = metrics.towerMetrics().get("Cannon");
        assertEquals(1, cannonMetrics.shots(), "Three hits on the same tick by the same tower must count as 1 shot");
    }

    @Test
    @DisplayName("Max shots calculation with ceil rounding: 25 ticks on map with cooldown 12 has maxShots 3")
    void testMaxShotsCeilRounding() {
        SimulationResult result = new SimulationResult(
                Outcome.WIN,
                20,
                150,
                25,
                List.of(
                        new TowerBuilt(0, 1, "Cannon", 2, 2, 90)
                )
        );

        RunMetrics metrics = MetricsCalculator.calculate(result, frozenConfig);
        TowerTypeMetrics cannonMetrics = metrics.towerMetrics().get("Cannon");
        // ceil(25 / 12.0) = ceil(2.0833...) = 3
        assertEquals(3, cannonMetrics.maxShots());
    }

    @Test
    @DisplayName("Tower built mid-run: 15 ticks on map with cooldown 12 has maxShots 2")
    void testTowerBuiltMidRun() {
        SimulationResult result = new SimulationResult(
                Outcome.WIN,
                20,
                150,
                25,
                List.of(
                        new TowerBuilt(10, 1, "Cannon", 2, 2, 90)
                )
        );

        RunMetrics metrics = MetricsCalculator.calculate(result, frozenConfig);
        TowerTypeMetrics cannonMetrics = metrics.towerMetrics().get("Cannon");
        // duration 25 - builtTick 10 = 15 ticks. ceil(15 / 12.0) = ceil(1.25) = 2
        assertEquals(2, cannonMetrics.maxShots());
    }

    @Test
    @DisplayName("TowerMissed events count towards shots")
    void testTowerMissedCountsAsShot() {
        SimulationResult result = new SimulationResult(
                Outcome.WIN,
                20,
                150,
                30,
                List.of(
                        new TowerBuilt(0, 1, "Cannon", 2, 2, 90),
                        new TowerMissed(5, 1, 10),
                        new TowerFired(17, 1, "Cannon", 11, 12)
                )
        );

        RunMetrics metrics = MetricsCalculator.calculate(result, frozenConfig);
        TowerTypeMetrics cannonMetrics = metrics.towerMetrics().get("Cannon");
        assertEquals(2, cannonMetrics.shots(), "One miss at tick 5 and one hit at tick 17 count as 2 shots");
    }

    @Test
    @DisplayName("Unknown tower type in events throws InvalidConfigException")
    void testUnknownTowerTypeRejected() {
        SimulationResult badBuilt = new SimulationResult(
                Outcome.WIN, 20, 150, 10,
                List.of(new TowerBuilt(0, 1, "LaserCannon", 1, 1, 100))
        );
        assertThrows(InvalidConfigException.class, () -> MetricsCalculator.calculate(badBuilt, frozenConfig));

        SimulationResult badFired = new SimulationResult(
                Outcome.WIN, 20, 150, 10,
                List.of(new TowerFired(5, 1, "PlasmaTower", 10, 20))
        );
        assertThrows(InvalidConfigException.class, () -> MetricsCalculator.calculate(badFired, frozenConfig));

        SimulationResult badMissed = new SimulationResult(
                Outcome.WIN, 20, 150, 10,
                List.of(new TowerMissed(5, 999, 10))
        );
        assertThrows(InvalidConfigException.class, () -> MetricsCalculator.calculate(badMissed, frozenConfig));
    }

    @Test
    @DisplayName("Null arguments to MetricsCalculator.calculate throw InvalidConfigException")
    void testNullArgumentsRejected() {
        assertThrows(InvalidConfigException.class, () -> MetricsCalculator.calculate(null, frozenConfig));
        SimulationResult validResult = new SimulationResult(Outcome.WIN, 20, 150, 10, List.of());
        assertThrows(InvalidConfigException.class, () -> MetricsCalculator.calculate(validResult, null));
    }

    @Test
    @DisplayName("Aggregation: pooled values on a hand-computed two-run example")
    void testAggregationPooledValuesTwoRuns() {
        // Run 1:
        // Cannon: damage 120, goldSpent 60, shots 10, maxShots 12
        // Sniper: damage 80, goldSpent 90, shots 2, maxShots 4
        RunMetrics run1 = new RunMetrics(
                Outcome.WIN, 100, 19, 200, 5, 1,
                Map.of(
                        "Cannon", new TowerTypeMetrics(120, 60, 10, 12),
                        "Sniper", new TowerTypeMetrics(80, 90, 2, 4)
                )
        );

        // Run 2:
        // Cannon: damage 60, goldSpent 60, shots 5, maxShots 12
        // Frost: damage 20, goldSpent 50, shots 8, maxShots 10
        RunMetrics run2 = new RunMetrics(
                Outcome.LOSS, 120, 15, 80, 2, 3,
                Map.of(
                        "Cannon", new TowerTypeMetrics(60, 60, 5, 12),
                        "Frost", new TowerTypeMetrics(20, 50, 8, 10)
                )
        );

        BatchResult batch = MetricsCalculator.aggregate(List.of(run1, run2));

        assertEquals(2, batch.runs());
        assertEquals(0.5, batch.winRate(), 1e-6);

        // Averages
        assertEquals(140.0, batch.averages().damageDealt(), 1e-6);
        assertEquals(3.5, batch.averages().enemiesDefeated(), 1e-6);
        assertEquals(2.0, batch.averages().leakage(), 1e-6);
        assertEquals(17.0, batch.averages().baseHealthRemaining(), 1e-6);
        assertEquals(110.0, batch.averages().durationTicks(), 1e-6);

        // Tower stats (sorted alphabetically: Cannon, Frost, Sniper)
        assertEquals(3, batch.towerStats().size());

        // Cannon: totalDamage = 180, totalGold = 120, totalShots = 15, totalMaxShots = 24
        // Total damage all types = 180 + 20 + 80 = 280
        TowerStat cannonStat = batch.towerStats().get(0);
        assertEquals("Cannon", cannonStat.towerType());
        assertEquals(180.0 / 280.0, cannonStat.damageContribution(), 1e-6);
        assertEquals(180.0 / 120.0, cannonStat.costEfficiency(), 1e-6);
        assertEquals(15.0 / 24.0, cannonStat.utilization(), 1e-6);

        // Frost: totalDamage = 20, totalGold = 50, totalShots = 8, totalMaxShots = 10
        TowerStat frostStat = batch.towerStats().get(1);
        assertEquals("Frost", frostStat.towerType());
        assertEquals(20.0 / 280.0, frostStat.damageContribution(), 1e-6);
        assertEquals(20.0 / 50.0, frostStat.costEfficiency(), 1e-6);
        assertEquals(8.0 / 10.0, frostStat.utilization(), 1e-6);

        // Sniper: totalDamage = 80, totalGold = 90, totalShots = 2, totalMaxShots = 4
        TowerStat sniperStat = batch.towerStats().get(2);
        assertEquals("Sniper", sniperStat.towerType());
        assertEquals(80.0 / 280.0, sniperStat.damageContribution(), 1e-6);
        assertEquals(80.0 / 90.0, sniperStat.costEfficiency(), 1e-6);
        assertEquals(2.0 / 4.0, sniperStat.utilization(), 1e-6);
    }

    @Test
    @DisplayName("Aggregation: zero-damage and zero-spend cases give 0 (never NaN or Infinity); utilization capped at 1.0")
    void testAggregationZeroDamageZeroSpendAndCappedUtilization() {
        RunMetrics run = new RunMetrics(
                Outcome.WIN, 100, 20, 0, 0, 0,
                Map.of(
                        "ZeroSpend", new TowerTypeMetrics(50, 0, 5, 10),
                        "ZeroDamage", new TowerTypeMetrics(0, 60, 0, 10),
                        "ZeroMaxShots", new TowerTypeMetrics(10, 50, 0, 0),
                        "OverUtilized", new TowerTypeMetrics(10, 50, 15, 10)
                )
        );

        BatchResult batch = MetricsCalculator.aggregate(List.of(run));

        for (TowerStat stat : batch.towerStats()) {
            assertFalse(Double.isNaN(stat.damageContribution()), "damageContribution must not be NaN");
            assertFalse(Double.isInfinite(stat.damageContribution()), "damageContribution must not be Infinite");
            assertFalse(Double.isNaN(stat.costEfficiency()), "costEfficiency must not be NaN");
            assertFalse(Double.isInfinite(stat.costEfficiency()), "costEfficiency must not be Infinite");
            assertFalse(Double.isNaN(stat.utilization()), "utilization must not be NaN");
            assertFalse(Double.isInfinite(stat.utilization()), "utilization must not be Infinite");
            assertTrue(stat.utilization() <= 1.0, "utilization must be capped at 1.0");
        }

        TowerStat zeroSpend = batch.towerStats().stream().filter(s -> "ZeroSpend".equals(s.towerType())).findFirst().orElseThrow();
        assertEquals(0.0, zeroSpend.costEfficiency(), "Zero spend must produce 0.0 cost efficiency");

        TowerStat zeroDamage = batch.towerStats().stream().filter(s -> "ZeroDamage".equals(s.towerType())).findFirst().orElseThrow();
        assertEquals(0.0, zeroDamage.damageContribution());
        assertEquals(0.0, zeroDamage.costEfficiency());

        TowerStat zeroMaxShots = batch.towerStats().stream().filter(s -> "ZeroMaxShots".equals(s.towerType())).findFirst().orElseThrow();
        assertEquals(0.0, zeroMaxShots.utilization());

        TowerStat overUtilized = batch.towerStats().stream().filter(s -> "OverUtilized".equals(s.towerType())).findFirst().orElseThrow();
        assertEquals(1.0, overUtilized.utilization(), "Utilization above 1.0 must be capped at 1.0");
    }

    @Test
    @DisplayName("Aggregation: towerStats sorted alphabetically and only built types listed")
    void testOnlyBuiltTypesListedInTowerStats() {
        RunMetrics run = new RunMetrics(
                Outcome.WIN, 100, 20, 100, 2, 0,
                Map.of("Cannon", new TowerTypeMetrics(100, 60, 5, 8))
        );

        BatchResult batch = MetricsCalculator.aggregate(List.of(run));
        assertEquals(1, batch.towerStats().size());
        assertEquals("Cannon", batch.towerStats().get(0).towerType());
    }
}
