package com.towerlab.engine.event;

/**
 * Listener interface for observing published game events.
 */
@FunctionalInterface
public interface GameEventListener {

    /**
     * Invoked when a game event is published.
     *
     * @param event the published game event
     */
    void onEvent(GameEvent event);
}
