package com.towerlab.engine.config;

import com.towerlab.engine.simulation.PlayerAction;

import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration record for executing a simulation run.
 *
 * @param gameConfig the loaded game definitions (towers, enemies, level)
 * @param seed       random seed used for reproducible simulation runs
 * @param actions    immutable list of scheduled player actions across waves
 */
public record SimulationConfig(
        GameConfig gameConfig,
        long seed,
        List<PlayerAction> actions
) {
    public SimulationConfig {
        Objects.requireNonNull(gameConfig, "gameConfig must not be null");
        if (actions == null) {
            actions = List.of();
        } else {
            actions = List.copyOf(actions);
            for (PlayerAction action : actions) {
                if (action == null) {
                    throw new IllegalArgumentException("PlayerAction in actions list must not be null");
                }
            }
        }
    }

    public SimulationConfig(GameConfig gameConfig, long seed) {
        this(gameConfig, seed, List.of());
    }
}
