package com.towerlab.engine.model;

/**
 * Game session lifecycle states per GR-33.
 */
public enum GameStatus {
    READY,
    PLAYING,
    WAVE_COMPLETED,
    WON,
    LOST
}
