package com.towerlab.engine.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Synchronous in-memory event bus dispatching game events to registered listeners
 * in order of registration.
 */
public class EventBus {

    private final List<GameEventListener> listeners = new ArrayList<>();

    /**
     * Registers a listener to receive published events.
     *
     * @param listener listener to register
     */
    public void register(GameEventListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    /**
     * Publishes a game event to all registered listeners in registration order.
     *
     * @param event event to publish
     */
    public void publish(GameEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        for (GameEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
