package com.towerlab.engine.exception;

/**
 * Thrown when configuration parameters are missing, invalid, or out of acceptable bounds.
 */
public class InvalidConfigException extends TowerLabException {

    public InvalidConfigException(String message) {
        super(message);
    }

    public InvalidConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
