package com.towerlab.engine.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.towerlab.engine.config.ConfigLoader;
import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.SimulationConfig;
import com.towerlab.engine.event.ActionRejected;
import com.towerlab.engine.strategy.TargetingMode;
import com.towerlab.engine.test.TestConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleResultExportTest {

    @Test
    @DisplayName("SampleResultExportTest: run level1 with fixed seed and 3-4 sensible towers, serialise to JSON")
    void testSampleResultExport() throws Exception {
        long startTime = System.currentTimeMillis();

        GameConfig gameConfig = TestConfigHelper.getFrozenGameConfig();

        // 3 sensible towers placed in valid buildable cells near path
        List<PlayerAction> actions = List.of(
                new BuildTowerAction(0, "Cannon", 1, 2, TargetingMode.FIRST),
                new BuildTowerAction(0, "Frost", 3, 0, TargetingMode.CLOSEST),
                new BuildTowerAction(1, "Sniper", 7, 4, TargetingMode.STRONGEST)
        );

        SimulationConfig config = new SimulationConfig(gameConfig, 42L, actions);
        Simulation simulation = new Simulation();
        SimulationResult result = simulation.run(config);

        long runTimeMs = System.currentTimeMillis() - startTime;

        assertNotNull(result);
        assertTrue(result.events().size() > 0);

        // Count all ActionRejected events in the sample simulation
        long rejectedCount = result.events().stream()
                .filter(e -> e instanceof ActionRejected)
                .count();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path outputFile = targetDir.resolve("sample-simulation-result.json");

        mapper.writeValue(outputFile.toFile(), result);

        assertTrue(Files.exists(outputFile), "Exported JSON file must exist");
        assertTrue(Files.size(outputFile) > 0, "Exported JSON file must be non-empty");

        System.out.println("==================================================");
        System.out.println("Sample Simulation Export Completed:");
        System.out.println("  Outcome:               " + result.outcome());
        System.out.println("  Total ticks:           " + result.ticks());
        System.out.println("  Base health:           " + result.baseHealth());
        System.out.println("  Gold:                  " + result.gold());
        System.out.println("  Event count:           " + result.events().size());
        System.out.println("  ActionRejected count:  " + rejectedCount);
        System.out.println("  Run time:              " + runTimeMs + " ms");
        System.out.println("  Export file:           " + outputFile.toAbsolutePath());
        System.out.println("==================================================");

        // Fail test if count of ActionRejected events is greater than zero
        assertEquals(0, rejectedCount, "Sample simulation must not produce any ActionRejected events");
    }
}
