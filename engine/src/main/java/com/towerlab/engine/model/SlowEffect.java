package com.towerlab.engine.model;

/**
 * Implements Slow status effect per GR-21:
 * Reduces movement speed by a fraction for a duration.
 * Effects do not stack: a stronger slow replaces a weaker one, and reapplying refreshes duration.
 */
public class SlowEffect extends StatusEffect {

    private final double slowFraction;

    /**
     * Constructs a SlowEffect.
     *
     * @param slowFraction  fractional speed reduction in (0.0, 1.0), e.g. 0.50 for 50% slow
     * @param durationTicks duration in ticks (must be positive)
     */
    public SlowEffect(double slowFraction, int durationTicks) {
        super(slowFraction, durationTicks);
        if (slowFraction <= 0.0 || slowFraction >= 1.0) {
            throw new IllegalArgumentException("Slow fraction must be in range (0.0, 1.0), got: " + slowFraction);
        }
        this.slowFraction = slowFraction;
    }

    /**
     * Returns the fractional speed reduction (e.g. 0.5 for 50% slow).
     *
     * @return slow reduction fraction
     */
    public double slowFraction() {
        return slowFraction;
    }

    /**
     * Returns the speed multiplier applied to entity movement speed (1.0 - slowFraction).
     *
     * @return speed multiplier in (0.0, 1.0)
     */
    public double speedMultiplier() {
        return 1.0 - slowFraction;
    }

    @Override
    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    @Override
    public void refresh() {
        this.remainingTicks = durationTicks;
    }
}
