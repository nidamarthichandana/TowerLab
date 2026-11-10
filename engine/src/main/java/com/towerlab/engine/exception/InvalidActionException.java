package com.towerlab.engine.exception;

/**
 * Thrown when an invalid game action or lifecycle state transition is attempted per GR-35 and GR-37.
 */
public class InvalidActionException extends TowerLabException {

    public InvalidActionException(String message) {
        super(message);
    }

    public InvalidActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
