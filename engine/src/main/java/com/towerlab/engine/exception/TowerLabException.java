package com.towerlab.engine.exception;

/**
 * Base custom exception for all TowerLab domain errors.
 */
public class TowerLabException extends RuntimeException {

    public TowerLabException(String message) {
        super(message);
    }

    public TowerLabException(String message, Throwable cause) {
        super(message, cause);
    }
}
