package com.towerlab.engine.model;

/**
 * Base abstract class for temporary status effects applied to entities.
 */
public abstract class StatusEffect {

    protected int remainingTicks;
    protected final int durationTicks;
    protected final double magnitude;

    /**
     * Constructs a StatusEffect.
     *
     * @param magnitude     strength of the effect
     * @param durationTicks total duration in ticks (must be positive)
     */
    public StatusEffect(double magnitude, int durationTicks) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("Status effect duration must be positive, got: " + durationTicks);
        }
        this.magnitude = magnitude;
        this.durationTicks = durationTicks;
        this.remainingTicks = durationTicks;
    }

    /**
     * Returns the remaining duration of this effect in ticks.
     *
     * @return remaining ticks
     */
    public int remainingTicks() {
        return remainingTicks;
    }

    /**
     * Returns the configured total duration of this effect in ticks.
     *
     * @return total duration ticks
     */
    public int durationTicks() {
        return durationTicks;
    }

    /**
     * Returns the magnitude or intensity of the effect.
     *
     * @return effect magnitude
     */
    public double magnitude() {
        return magnitude;
    }

    /**
     * Checks if this status effect has expired.
     *
     * @return true if remaining ticks <= 0
     */
    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    /**
     * Advances the effect by one simulation tick.
     */
    public abstract void tick();

    /**
     * Refreshes the effect duration back to its configured initial duration.
     */
    public abstract void refresh();
}
