package com.towerlab.engine.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
    }

    @Test
    @DisplayName("EventBus notifies listeners in strict registration order")
    void testListenersNotifiedInRegistrationOrder() {
        List<String> order = new ArrayList<>();

        eventBus.register(event -> order.add("Listener1:" + event.getClass().getSimpleName()));
        eventBus.register(event -> order.add("Listener2:" + event.getClass().getSimpleName()));
        eventBus.register(event -> order.add("Listener3:" + event.getClass().getSimpleName()));

        WaveStarted event = new WaveStarted(1, 1);
        eventBus.publish(event);

        assertEquals(List.of(
                "Listener1:WaveStarted",
                "Listener2:WaveStarted",
                "Listener3:WaveStarted"
        ), order);
    }

    @Test
    @DisplayName("EventLog collects published events and preserves order")
    void testEventLogCollectsEvents() {
        EventLog eventLog = new EventLog();
        eventBus.register(eventLog);

        WaveStarted e1 = new WaveStarted(1, 1);
        EnemySpawned e2 = new EnemySpawned(2, 10, "Basic", 1);
        TowerBuilt e3 = new TowerBuilt(3, 1, "Cannon", 2, 2, 90);

        eventBus.publish(e1);
        eventBus.publish(e2);
        eventBus.publish(e3);

        List<GameEvent> recorded = eventLog.events();
        assertEquals(3, recorded.size());
        assertEquals(e1, recorded.get(0));
        assertEquals(e2, recorded.get(1));
        assertEquals(e3, recorded.get(2));
    }

    @Test
    @DisplayName("EventLog events list is immutable against external modifications")
    void testEventLogListIsImmutable() {
        EventLog eventLog = new EventLog();
        eventLog.onEvent(new WaveStarted(1, 1));

        List<GameEvent> list = eventLog.events();
        assertNotNull(list);
        assertEquals(1, list.size());

        assertThrows(UnsupportedOperationException.class, () ->
                list.add(new WaveStarted(2, 2)));

        assertThrows(UnsupportedOperationException.class, () ->
                list.remove(0));

        assertThrows(UnsupportedOperationException.class, list::clear);
    }

    @Test
    @DisplayName("EventBus and EventLog reject null arguments")
    void testNullArgumentsRejected() {
        assertThrows(NullPointerException.class, () -> eventBus.register(null));
        assertThrows(NullPointerException.class, () -> eventBus.publish(null));

        EventLog log = new EventLog();
        assertThrows(NullPointerException.class, () -> log.onEvent(null));
    }
}
