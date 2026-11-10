package com.towerlab.engine.model;

import com.towerlab.engine.config.SpawnGroupConfig;
import com.towerlab.engine.config.WaveConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveTest {

    @Test
    @DisplayName("Wave 2: 10 Basic (interval 15) and 4 Fast (interval 25, delay 100)")
    void testWave2Specification() {
        SpawnGroupConfig basicConfig = new SpawnGroupConfig("Basic", 10, 15, 0);
        SpawnGroupConfig fastConfig = new SpawnGroupConfig("Fast", 4, 25, 100);
        WaveConfig wave2Config = new WaveConfig(2, List.of(basicConfig, fastConfig));

        Wave wave2 = new Wave(wave2Config);
        assertEquals(2, wave2.waveNumber());
        assertEquals(2, wave2.groups().size());
        assertEquals(14, wave2.totalEnemies(), "Wave 2 must have 10 + 4 = 14 total enemies");

        SpawnGroup fastGroup = wave2.groups().get(1);
        assertEquals("Fast", fastGroup.enemyType());
        assertEquals(4, fastGroup.count());
        assertEquals(25, fastGroup.interval());
        assertEquals(100, fastGroup.startDelay());
        assertEquals(List.of(100, 125, 150, 175), fastGroup.nominalSpawnTicks(),
                "Fast spawn ticks must be exactly 100, 125, 150, 175");

        List<ScheduledSpawn> schedule = wave2.nominalSchedule();
        assertEquals(14, schedule.size());

        // Verify schedule is monotonically sorted by relative tick
        for (int i = 0; i < schedule.size() - 1; i++) {
            assertTrue(schedule.get(i).relativeTick() <= schedule.get(i + 1).relativeTick(),
                    "Schedule must be sorted by tick at index " + i);
        }

        // Expected sorted schedule:
        // 0: Basic, 15: Basic, 30: Basic, 45: Basic, 60: Basic, 75: Basic, 90: Basic,
        // 100: Fast, 105: Basic, 120: Basic, 125: Fast, 135: Basic, 150: Fast, 175: Fast
        assertEquals(0, schedule.get(0).relativeTick());
        assertEquals("Basic", schedule.get(0).enemyType());

        assertEquals(100, schedule.get(7).relativeTick());
        assertEquals("Fast", schedule.get(7).enemyType());

        assertEquals(105, schedule.get(8).relativeTick());
        assertEquals("Basic", schedule.get(8).enemyType());

        assertEquals(120, schedule.get(9).relativeTick());
        assertEquals("Basic", schedule.get(9).enemyType());

        assertEquals(125, schedule.get(10).relativeTick());
        assertEquals("Fast", schedule.get(10).enemyType());

        assertEquals(175, schedule.get(13).relativeTick());
        assertEquals("Fast", schedule.get(13).enemyType());
    }

    @Test
    @DisplayName("Wave 5: 6 Armored (interval 40) and 10 Basic (interval 12) has 16 enemies")
    void testWave5Specification() {
        SpawnGroupConfig armoredConfig = new SpawnGroupConfig("Armored", 6, 40, 0);
        SpawnGroupConfig basicConfig = new SpawnGroupConfig("Basic", 10, 12, 0);
        WaveConfig wave5Config = new WaveConfig(5, List.of(armoredConfig, basicConfig));

        Wave wave5 = new Wave(wave5Config);
        assertEquals(5, wave5.waveNumber());
        assertEquals(16, wave5.totalEnemies(), "Wave 5 must have 6 + 10 = 16 total enemies");
        assertEquals(16, wave5.nominalSchedule().size());

        // At tick 0, both Armored and Basic spawn: group order tie-breaking puts Armored first
        assertEquals(0, wave5.nominalSchedule().get(0).relativeTick());
        assertEquals("Armored", wave5.nominalSchedule().get(0).enemyType());
        assertEquals(0, wave5.nominalSchedule().get(1).relativeTick());
        assertEquals("Basic", wave5.nominalSchedule().get(1).enemyType());
    }

    @Test
    @DisplayName("Immutability: groups and schedule lists cannot be modified")
    void testWaveImmutability() {
        WaveConfig config = new WaveConfig(1, List.of(new SpawnGroupConfig("Basic", 5, 20, 0)));
        Wave wave = new Wave(config);

        assertThrows(UnsupportedOperationException.class, () -> wave.groups().add(null));
        assertThrows(UnsupportedOperationException.class, () -> wave.nominalSchedule().add(null));
        assertThrows(UnsupportedOperationException.class, () -> wave.groups().get(0).nominalSpawnTicks().add(99));
    }

    @Test
    @DisplayName("Null arguments are rejected")
    void testNullValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Wave(null));
        assertThrows(IllegalArgumentException.class, () -> new SpawnGroup(null));
        assertThrows(IllegalArgumentException.class, () -> new ScheduledSpawn(0, null));
        assertThrows(IllegalArgumentException.class, () -> new ScheduledSpawn(0, ""));
        assertThrows(IllegalArgumentException.class, () -> new ScheduledSpawn(-1, "Basic"));
    }
}
