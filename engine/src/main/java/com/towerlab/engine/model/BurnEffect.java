package com.towerlab.engine.model;

/**
 * Implements Burn status effect per GR-22:
 * Deals periodic true damage at configurable intervals over a duration.
 * Reapplying refreshes duration without stacking or resetting the interval.
 * Requires explicit configuration for source tower ID, damage, interval, and duration.
 */
public class BurnEffect extends StatusEffect {

    private final int damage;
    private final int intervalTicks;
    private int ticksSinceLastDamage;
    private int pendingDamage;

    /**
     * Constructs a BurnEffect with explicit source tower id, damage, interval, and duration.
     *
     * @param sourceTowerId identifier of the tower inflicting the burn
     * @param damage        true damage dealt per interval (must be positive)
     * @param intervalTicks interval in ticks between damage procs (must be positive)
     * @param durationTicks total duration in ticks (must be positive)
     */
    public BurnEffect(int sourceTowerId, int damage, int intervalTicks, int durationTicks) {
        super(sourceTowerId, damage, durationTicks);
        if (damage <= 0) {
            throw new IllegalArgumentException("Burn damage must be positive, got: " + damage);
        }
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("Burn interval must be positive, got: " + intervalTicks);
        }
        this.damage = damage;
        this.intervalTicks = intervalTicks;
        this.ticksSinceLastDamage = 0;
        this.pendingDamage = 0;
    }

    public int damage() {
        return damage;
    }

    public int intervalTicks() {
        return intervalTicks;
    }

    /**
     * Returns and consumes any damage due on the current tick.
     *
     * @return damage amount due, or 0 if none
     */
    public int consumeDamage() {
        int due = pendingDamage;
        pendingDamage = 0;
        return due;
    }

    @Override
    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            ticksSinceLastDamage++;
            if (ticksSinceLastDamage >= intervalTicks) {
                pendingDamage = damage;
                ticksSinceLastDamage = 0;
            } else {
                pendingDamage = 0;
            }
        } else {
            pendingDamage = 0;
        }
    }

    @Override
    public void refresh() {
        this.remainingTicks = durationTicks;
    }
}
