package com.towerlab.engine.simulation;

import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.event.EventBus;
import com.towerlab.engine.event.TowerBuilt;
import com.towerlab.engine.exception.InsufficientFundsException;
import com.towerlab.engine.exception.InvalidActionException;
import com.towerlab.engine.exception.InvalidPlacementException;
import com.towerlab.engine.factory.TowerFactory;
import com.towerlab.engine.model.GameState;
import com.towerlab.engine.model.GameStatus;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.model.Tower;
import com.towerlab.engine.strategy.TargetingMode;

import java.util.Objects;

/**
 * Player action command to construct a new tower at a specified grid position.
 * Can only be executed during READY or WAVE_COMPLETED status per GR-35.
 * Strictly verifies placement and funds before performing any state modifications.
 *
 * @param wave          target wave index
 * @param towerType     type of tower to build (e.g. Cannon, Sniper, Frost)
 * @param x             grid cell x-coordinate
 * @param y             grid cell y-coordinate
 * @param targetingMode targeting strategy mode (required)
 */
public record BuildTowerAction(
        int wave,
        String towerType,
        int x,
        int y,
        TargetingMode targetingMode
) implements PlayerAction {

    public BuildTowerAction {
        Objects.requireNonNull(towerType, "towerType must not be null");
        Objects.requireNonNull(targetingMode, "targetingMode must not be null");
    }

    @Override
    public String type() {
        return "BuildTower";
    }

    @Override
    public void execute(GameState state, TowerFactory towerFactory, EventBus eventBus, int tick) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(towerFactory, "towerFactory must not be null");
        Objects.requireNonNull(eventBus, "eventBus must not be null");

        // 1. Validate lifecycle status per GR-35
        if (state.status() != GameStatus.READY && state.status() != GameStatus.WAVE_COMPLETED) {
            throw new InvalidActionException(
                    "BuildTowerAction is only allowed during READY or WAVE_COMPLETED status, current status: " + state.status()
            );
        }

        // 2. Validate tower type configuration and cost
        TowerConfig config = towerFactory.config(towerType);
        int cost = config.cost();

        // 3. Validate placement per GR-9 and GR-27
        Position position = new Position(x, y);
        if (!state.map().isBuildable(position)) {
            throw new InvalidPlacementException("Cell " + position + " is not buildable");
        }
        if (state.towerAt(position).isPresent()) {
            throw new InvalidPlacementException("Cell " + position + " is already occupied by a tower");
        }

        // 4. Validate affordability per GR-27
        if (state.gold() < cost) {
            throw new InsufficientFundsException(
                    "Insufficient gold: required " + cost + ", but current balance is " + state.gold()
            );
        }

        // Validation succeeded: apply mutations atomically
        state.spendGold(cost);
        int towerId = state.nextTowerId();
        Tower tower = towerFactory.create(towerType, towerId, position, targetingMode.strategy());
        state.addTower(tower);
        eventBus.publish(new TowerBuilt(tick, towerId, towerType, x, y, state.gold()));
    }
}
