package com.towerlab.engine.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameEventTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Serialize and deserialize all 12 GameEvent types verifying 'type' and 'tick' fields")
    void testSerializeAndDeserializeAllEvents() throws Exception {
        List<GameEvent> events = List.of(
                new WaveStarted(10, 1),
                new EnemySpawned(15, 42, "Basic", 1),
                new EnemyMoved(20, 42, 3.5, 4.2),
                new TowerBuilt(25, 7, "Cannon", 2, 3, 90),
                new TowerFired(30, 7, "Cannon", 42, 12),
                new TowerMissed(35, 7, 43),
                new EffectApplied(40, 42, "Slow", 0.5, 30),
                new EnemyKilled(45, 42, "Basic", 7, 8, 98),
                new LifeLost(50, 44, 2, 18),
                new WaveCompleted(55, 1, 20, 118),
                new ActionRejected(60, "BuildTower", "Insufficient funds"),
                new GameEnded(65, "WON")
        );

        for (GameEvent event : events) {
            String json = objectMapper.writeValueAsString(event);
            assertNotNull(json);

            JsonNode tree = objectMapper.readTree(json);
            String expectedType = event.getClass().getSimpleName();

            assertEquals(expectedType, tree.get("type").asText(),
                    "Event " + expectedType + " must have matching 'type' discriminator");
            assertEquals(event.tick(), tree.get("tick").asInt(),
                    "Event " + expectedType + " must have matching 'tick'");

            // Polymorphic deserialization through sealed GameEvent interface
            GameEvent deserialized = objectMapper.readValue(json, GameEvent.class);
            assertNotNull(deserialized);
            assertEquals(event.getClass(), deserialized.getClass());
            assertEquals(event, deserialized);
        }
    }

    @Test
    @DisplayName("Verify event-specific payload fields serialized and deserialized accurately")
    void testSpecificEventPayloadFields() throws Exception {
        // WaveStarted
        WaveStarted ws = new WaveStarted(1, 2);
        JsonNode wsNode = objectMapper.readTree(objectMapper.writeValueAsString(ws));
        assertEquals(2, wsNode.get("wave").asInt());

        // EnemySpawned
        EnemySpawned es = new EnemySpawned(2, 5, "Fast", 3);
        JsonNode esNode = objectMapper.readTree(objectMapper.writeValueAsString(es));
        assertEquals(5, esNode.get("enemyId").asInt());
        assertEquals("Fast", esNode.get("enemyType").asText());
        assertEquals(3, esNode.get("wave").asInt());

        // EnemyMoved
        EnemyMoved em = new EnemyMoved(3, 5, 2.75, 4.5);
        JsonNode emNode = objectMapper.readTree(objectMapper.writeValueAsString(em));
        assertEquals(2.75, emNode.get("x").asDouble(), 1e-6);
        assertEquals(4.5, emNode.get("y").asDouble(), 1e-6);

        // TowerBuilt
        TowerBuilt tb = new TowerBuilt(4, 1, "Sniper", 3, 4, 60);
        JsonNode tbNode = objectMapper.readTree(objectMapper.writeValueAsString(tb));
        assertEquals(1, tbNode.get("towerId").asInt());
        assertEquals("Sniper", tbNode.get("towerType").asText());
        assertEquals(3, tbNode.get("x").asInt());
        assertEquals(4, tbNode.get("y").asInt());
        assertEquals(60, tbNode.get("goldAfter").asInt());

        // TowerFired
        TowerFired tf = new TowerFired(5, 1, "Sniper", 5, 25);
        JsonNode tfNode = objectMapper.readTree(objectMapper.writeValueAsString(tf));
        assertEquals(1, tfNode.get("towerId").asInt());
        assertEquals("Sniper", tfNode.get("towerType").asText());
        assertEquals(5, tfNode.get("enemyId").asInt());
        assertEquals(25, tfNode.get("damageDealt").asInt());

        // TowerMissed
        TowerMissed tm = new TowerMissed(6, 1, 5);
        JsonNode tmNode = objectMapper.readTree(objectMapper.writeValueAsString(tm));
        assertEquals(1, tmNode.get("towerId").asInt());
        assertEquals(5, tmNode.get("enemyId").asInt());

        // EffectApplied
        EffectApplied ea = new EffectApplied(7, 5, "Slow", 0.6, 30);
        JsonNode eaNode = objectMapper.readTree(objectMapper.writeValueAsString(ea));
        assertEquals(5, eaNode.get("enemyId").asInt());
        assertEquals("Slow", eaNode.get("effectType").asText());
        assertEquals(0.6, eaNode.get("magnitude").asDouble(), 1e-6);
        assertEquals(30, eaNode.get("durationTicks").asInt());

        // EnemyKilled
        EnemyKilled ek = new EnemyKilled(8, 5, "Fast", 1, 7, 67);
        JsonNode ekNode = objectMapper.readTree(objectMapper.writeValueAsString(ek));
        assertEquals(5, ekNode.get("enemyId").asInt());
        assertEquals("Fast", ekNode.get("enemyType").asText());
        assertEquals(1, ekNode.get("towerId").asInt());
        assertEquals(7, ekNode.get("reward").asInt());
        assertEquals(67, ekNode.get("goldAfter").asInt());

        // LifeLost
        LifeLost ll = new LifeLost(9, 6, 1, 19);
        JsonNode llNode = objectMapper.readTree(objectMapper.writeValueAsString(ll));
        assertEquals(6, llNode.get("enemyId").asInt());
        assertEquals(1, llNode.get("damage").asInt());
        assertEquals(19, llNode.get("baseHealthAfter").asInt());

        // WaveCompleted
        WaveCompleted wc = new WaveCompleted(10, 1, 20, 87);
        JsonNode wcNode = objectMapper.readTree(objectMapper.writeValueAsString(wc));
        assertEquals(1, wcNode.get("wave").asInt());
        assertEquals(20, wcNode.get("bonusGold").asInt());
        assertEquals(87, wcNode.get("goldAfter").asInt());

        // ActionRejected
        ActionRejected ar = new ActionRejected(11, "BuildTower", "Cell occupied");
        JsonNode arNode = objectMapper.readTree(objectMapper.writeValueAsString(ar));
        assertEquals("BuildTower", arNode.get("actionType").asText());
        assertEquals("Cell occupied", arNode.get("reason").asText());

        // GameEnded
        GameEnded ge = new GameEnded(12, "LOST");
        JsonNode geNode = objectMapper.readTree(objectMapper.writeValueAsString(ge));
        assertEquals("LOST", geNode.get("outcome").asText());
    }

    @Test
    @DisplayName("Null arguments in event constructors are rejected")
    void testNullArgumentsRejected() {
        assertThrows(NullPointerException.class, () -> new EnemySpawned(1, 1, null, 1));
        assertThrows(NullPointerException.class, () -> new TowerBuilt(1, 1, null, 0, 0, 100));
        assertThrows(NullPointerException.class, () -> new TowerFired(1, 1, null, 1, 10));
        assertThrows(NullPointerException.class, () -> new EffectApplied(1, 1, null, 0.5, 10));
        assertThrows(NullPointerException.class, () -> new EnemyKilled(1, 1, null, 1, 10, 100));
        assertThrows(NullPointerException.class, () -> new ActionRejected(1, null, "reason"));
        assertThrows(NullPointerException.class, () -> new ActionRejected(1, "action", null));
        assertThrows(NullPointerException.class, () -> new GameEnded(1, null));
    }
}
