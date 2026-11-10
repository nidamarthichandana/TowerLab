package com.towerlab.engine.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Event listener that records and stores all received game events.
 * Provides read-only immutable access to recorded events for analysis and testing.
 */
public class EventLog implements GameEventListener {

    private final List<GameEvent> events = new ArrayList<>();

    @Override
    public void onEvent(GameEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        events.add(event);
    }

    /**
     * Returns an unmodifiable list view of all recorded events in receipt order.
     *
     * @return unmodifiable list of recorded events
     */
    public List<GameEvent> events() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Clears all recorded events from this log.
     */
    public void clear() {
        events.clear();
    }
}
