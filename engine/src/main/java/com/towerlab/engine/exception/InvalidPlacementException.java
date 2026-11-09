package com.towerlab.engine.exception;

/**
 * Thrown when an attempt is made to place a tower on an invalid or unbuildable grid position.
 */
public class InvalidPlacementException extends TowerLabException {

    public InvalidPlacementException(String message) {
        super(message);
    }

    public InvalidPlacementException(String message, Throwable cause) {
        super(message, cause);
    }
}
