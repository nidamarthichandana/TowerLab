package com.towerlab.engine.simulation;

import com.towerlab.engine.event.EventBus;
import com.towerlab.engine.factory.TowerFactory;
import com.towerlab.engine.model.GameState;

/**
 * Command interface representing an immutable player action tagged with its target wave per GR-36.
 */
public interface PlayerAction {

    /**
     * Identifies the action type (e.g. "BuildTower", "UpgradeTower", "SellTower").
     *
     * @return action type name
     */
    String type();

    /**
     * The wave index to which this player action applies.
     *
     * @return wave number
     */
    int wave();

    /**
     * Executes the action against the game state, using the tower factory and event bus.
     *
     * @param state        current mutable game state
     * @param towerFactory factory for validating and instantiating towers
     * @param eventBus     event bus to publish resulting game events
     * @param tick         current simulation tick
     */
    void execute(GameState state, TowerFactory towerFactory, EventBus eventBus, int tick);
}
