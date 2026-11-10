package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event published when a tower is successfully constructed.
 *
 * @param tick      simulation tick
 * @param towerId   unique tower identifier
 * @param towerType type of the tower built
 * @param x         grid x-coordinate
 * @param y         grid y-coordinate
 * @param goldAfter player gold balance remaining after building
 */
@JsonTypeName("TowerBuilt")
public record TowerBuilt(
        int tick,
        int towerId,
        String towerType,
        int x,
        int y,
        int goldAfter
) implements GameEvent {

    public TowerBuilt {
        Objects.requireNonNull(towerType, "towerType must not be null");
    }
}
