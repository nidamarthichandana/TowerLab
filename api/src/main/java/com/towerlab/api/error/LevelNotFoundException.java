package com.towerlab.api.error;

/**
 * Thrown when a requested level identifier cannot be found.
 */
public class LevelNotFoundException extends RuntimeException {
    public LevelNotFoundException(String message) {
        super(message);
    }
}
