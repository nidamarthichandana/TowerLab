package com.towerlab.engine.model;

import com.towerlab.engine.config.EnemyConfig;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base abstract class representing an enemy unit advancing along a path.
 * Implements Damageable and tracks health, path progress, and active status effects.
 * Contains no random number generation per GR specifications.
 */
public abstract class Enemy implements Damageable {

    private final String id;
    private final EnemyConfig config;
    private final int maxHealth;
    private int health;
    private double progress;
    private boolean reachedEnd;
    private final Map<Class<? extends StatusEffect>, StatusEffect> activeEffects;

    /**
     * Constructs an Enemy from an id and EnemyConfig.
     *
     * @param id     unique identifier for the enemy instance
     * @param config enemy configuration parameters
     */
    public Enemy(String id, EnemyConfig config) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Enemy id cannot be null or blank");
        }
        if (config == null) {
            throw new IllegalArgumentException("EnemyConfig cannot be null");
        }
        if (config.health() <= 0.0) {
            throw new IllegalArgumentException("Initial enemy health must be positive, got: " + config.health());
        }
        if (config.speed() <= 0.0) {
            throw new IllegalArgumentException("Enemy speed must be positive, got: " + config.speed());
        }
        this.id = id.trim();
        this.config = config;
        this.maxHealth = (int) Math.round(config.health());
        this.health = this.maxHealth;
        this.progress = 0.0;
        this.reachedEnd = false;
        this.activeEffects = new LinkedHashMap<>();
    }

    public String id() {
        return id;
    }

    public EnemyConfig config() {
        return config;
    }

    public int maxHealth() {
        return maxHealth;
    }

    @Override
    public int currentHealth() {
        return health;
    }

    @Override
    public boolean isAlive() {
        return health > 0;
    }

    public double progress() {
        return progress;
    }

    public boolean hasReachedEnd() {
        return reachedEnd;
    }

    public boolean hasReachedEnd(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        return this.progress >= path.totalLength();
    }

    public ContinuousPosition position(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        return path.pointAt(progress);
    }

    @Override
    public int takeDamage(int rawDamage) {
        if (rawDamage <= 0) {
            throw new IllegalArgumentException("Raw damage must be positive, got: " + rawDamage);
        }
        if (!isAlive()) {
            return 0;
        }
        int effectiveDamage = (int) Math.max(1L, Math.round(rawDamage * (1.0 - config.armour())));
        int damageDealt = Math.min(this.health, effectiveDamage);
        this.health -= damageDealt;
        return damageDealt;
    }

    @Override
    public int takeTrueDamage(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("True damage amount must be positive, got: " + amount);
        }
        if (!isAlive()) {
            return 0;
        }
        int damageDealt = Math.min(this.health, amount);
        this.health -= damageDealt;
        return damageDealt;
    }

    /**
     * Advances progress along the path by speed times slow factor.
     *
     * @param path the path being traversed
     */
    public void advance(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (!isAlive()) {
            return;
        }

        double slowFactor = 1.0;
        StatusEffect slow = activeEffects.get(SlowEffect.class);
        if (slow instanceof SlowEffect slowEffect && !slowEffect.isExpired()) {
            slowFactor = slowEffect.speedMultiplier();
        }

        double step = config.speed() * slowFactor;
        this.progress += step;
        if (this.progress >= path.totalLength()) {
            this.progress = path.totalLength();
            this.reachedEnd = true;
        }
    }

    /**
     * Ticks active status effects, applying burn damage and removing expired effects.
     */
    public void tickEffects() {
        if (!isAlive()) {
            return;
        }

        Iterator<Map.Entry<Class<? extends StatusEffect>, StatusEffect>> iterator = activeEffects.entrySet().iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next().getValue();
            effect.tick();
            if (effect instanceof BurnEffect burn) {
                int damage = burn.consumeDamage();
                if (damage > 0 && isAlive()) {
                    takeTrueDamage(damage);
                }
            }
            if (effect.isExpired()) {
                iterator.remove();
            }
        }
    }

    /**
     * Applies a status effect per GR-21 and GR-22 rules:
     * - Slow: stronger slow replaces weaker slow; equal slow refreshes duration; weaker slow is ignored.
     * - Burn: reapplying refreshes duration without resetting interval counter and without stacking.
     * - Dead enemies ignore incoming status effects.
     *
     * @param effect the effect to apply
     */
    public void applyEffect(StatusEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("Status effect cannot be null");
        }
        if (!isAlive()) {
            return;
        }

        if (effect instanceof SlowEffect newSlow) {
            SlowEffect existing = (SlowEffect) activeEffects.get(SlowEffect.class);
            if (existing == null) {
                activeEffects.put(SlowEffect.class, newSlow);
            } else if (newSlow.slowFraction() > existing.slowFraction()) {
                activeEffects.put(SlowEffect.class, newSlow);
            } else if (Double.compare(newSlow.slowFraction(), existing.slowFraction()) == 0) {
                existing.refresh();
            }
            // Weaker incoming slow is ignored entirely per GR-21
        } else if (effect instanceof BurnEffect newBurn) {
            BurnEffect existing = (BurnEffect) activeEffects.get(BurnEffect.class);
            if (existing == null) {
                activeEffects.put(BurnEffect.class, newBurn);
            } else {
                existing.refresh();
            }
        } else {
            activeEffects.put(effect.getClass(), effect);
        }
    }

    public Collection<StatusEffect> activeEffects() {
        return List.copyOf(activeEffects.values());
    }

    public double evasionChance() {
        return config.evasionChance();
    }
}
