package com.towerlab.engine.exception;

/**
 * Thrown when an action requires more gold/funds than the player currently possesses.
 */
public class InsufficientFundsException extends TowerLabException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
