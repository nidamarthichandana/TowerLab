package com.towerlab.engine.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchResultSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("BatchResult serialises to JSON with exact field names matching frontend contract")
    void testBatchResultJsonFieldNames() throws Exception {
        Averages averages = new Averages(1250.5, 45.0, 2.5, 17.5, 1420.0);
        TowerStat towerStat = new TowerStat("Cannon", 0.65, 4.2, 0.85);
        Finding finding = new Finding("GR-50", 0.15, "Increase Cannon attack speed");
        BatchResult result = new BatchResult(
                100,
                0.85,
                averages,
                List.of(towerStat),
                List.of(finding)
        );

        String json = mapper.writeValueAsString(result);
        JsonNode root = mapper.readTree(json);

        // Root fields: runs, winRate, averages, towerStats, findings
        assertTrue(root.has("runs"), "Must contain 'runs'");
        assertTrue(root.has("winRate"), "Must contain 'winRate'");
        assertTrue(root.has("averages"), "Must contain 'averages'");
        assertTrue(root.has("towerStats"), "Must contain 'towerStats'");
        assertTrue(root.has("findings"), "Must contain 'findings'");

        assertEquals(100, root.get("runs").asInt());
        assertEquals(0.85, root.get("winRate").asDouble(), 1e-6);

        // Averages fields: damageDealt, enemiesDefeated, leakage, baseHealthRemaining, durationTicks
        JsonNode avgNode = root.get("averages");
        assertTrue(avgNode.has("damageDealt"), "Averages must contain 'damageDealt'");
        assertTrue(avgNode.has("enemiesDefeated"), "Averages must contain 'enemiesDefeated'");
        assertTrue(avgNode.has("leakage"), "Averages must contain 'leakage'");
        assertTrue(avgNode.has("baseHealthRemaining"), "Averages must contain 'baseHealthRemaining'");
        assertTrue(avgNode.has("durationTicks"), "Averages must contain 'durationTicks'");

        assertEquals(1250.5, avgNode.get("damageDealt").asDouble(), 1e-6);
        assertEquals(45.0, avgNode.get("enemiesDefeated").asDouble(), 1e-6);
        assertEquals(2.5, avgNode.get("leakage").asDouble(), 1e-6);
        assertEquals(17.5, avgNode.get("baseHealthRemaining").asDouble(), 1e-6);
        assertEquals(1420.0, avgNode.get("durationTicks").asDouble(), 1e-6);

        // TowerStat fields: towerType, damageContribution, costEfficiency, utilization
        JsonNode statsArray = root.get("towerStats");
        assertTrue(statsArray.isArray() && statsArray.size() == 1);
        JsonNode statNode = statsArray.get(0);
        assertTrue(statNode.has("towerType"), "TowerStat must contain 'towerType'");
        assertTrue(statNode.has("damageContribution"), "TowerStat must contain 'damageContribution'");
        assertTrue(statNode.has("costEfficiency"), "TowerStat must contain 'costEfficiency'");
        assertTrue(statNode.has("utilization"), "TowerStat must contain 'utilization'");

        assertEquals("Cannon", statNode.get("towerType").asText());
        assertEquals(0.65, statNode.get("damageContribution").asDouble(), 1e-6);
        assertEquals(4.2, statNode.get("costEfficiency").asDouble(), 1e-6);
        assertEquals(0.85, statNode.get("utilization").asDouble(), 1e-6);

        // Finding fields: rule, metricValue, suggestedAction
        JsonNode findingsArray = root.get("findings");
        assertTrue(findingsArray.isArray() && findingsArray.size() == 1);
        JsonNode findingNode = findingsArray.get(0);
        assertTrue(findingNode.has("rule"), "Finding must contain 'rule'");
        assertTrue(findingNode.has("metricValue"), "Finding must contain 'metricValue'");
        assertTrue(findingNode.has("suggestedAction"), "Finding must contain 'suggestedAction'");

        assertEquals("GR-50", findingNode.get("rule").asText());
        assertEquals(0.15, findingNode.get("metricValue").asDouble(), 1e-6);
        assertEquals("Increase Cannon attack speed", findingNode.get("suggestedAction").asText());
    }
}
