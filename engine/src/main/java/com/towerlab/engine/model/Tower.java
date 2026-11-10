package com.towerlab.engine.model;

import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.strategy.TargetCandidate;
import com.towerlab.engine.strategy.TargetingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Base abstract class representing a defensive tower placed on the grid.
 * Implements the attack update template method including cooldown tracking,
 * candidate filtering, targeting strategy query, and attack resolution.
 */
public abstract class Tower {

    private final int id;
    private final TowerConfig config;
    private final Position position;
    private int level;
    private int totalGoldInvested;
    private int cooldownLeft;
    private TargetingStrategy strategy;

    /**
     * Constructs a Tower with configuration, position, and targeting strategy.
     *
     * @param id       non-negative unique tower identifier
     * @param config   tower configuration parameters
     * @param position grid coordinates of the tower
     * @param strategy targeting strategy for selecting candidate enemies
     */
    public Tower(int id, TowerConfig config, Position position, TargetingStrategy strategy) {
        if (id < 0) {
            throw new IllegalArgumentException("Tower id cannot be negative, got: " + id);
        }
        if (config == null) {
            throw new IllegalArgumentException("TowerConfig cannot be null");
        }
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("TargetingStrategy cannot be null");
        }
        this.id = id;
        this.config = config;
        this.position = position;
        this.strategy = strategy;
        this.level = 1;
        this.totalGoldInvested = config.cost();
        this.cooldownLeft = 0;
    }

    public int id() {
        return id;
    }

    public TowerConfig config() {
        return config;
    }

    public Position position() {
        return position;
    }

    public int level() {
        return level;
    }

    public int totalGoldInvested() {
        return totalGoldInvested;
    }

    public int cooldownLeft() {
        return cooldownLeft;
    }

    public TargetingStrategy strategy() {
        return strategy;
    }

    /**
     * Changes the tower's targeting strategy.
     *
     * @param newStrategy the new targeting strategy (must not be null)
     */
    public void changeStrategy(TargetingStrategy newStrategy) {
        if (newStrategy == null) {
            throw new IllegalArgumentException("TargetingStrategy cannot be null");
        }
        this.strategy = newStrategy;
    }

    /**
     * Checks if this tower is capable of targeting the given enemy based on target type.
     *
     * @param enemy the candidate enemy
     * @return true if targetable
     */
    public boolean canTarget(Enemy enemy) {
        if (enemy == null) {
            return false;
        }
        com.towerlab.engine.config.TargetType targets = config.targets();
        if (targets == null || targets == com.towerlab.engine.config.TargetType.BOTH) {
            return true;
        }
        // In MVP all enemies are Ground per GR Table 5
        if (targets == com.towerlab.engine.config.TargetType.GROUND) {
            return true;
        }
        if (targets == com.towerlab.engine.config.TargetType.AIR) {
            return false;
        }
        return true;
    }

    /**
     * Calculates the Euclidean distance between the tower's grid cell and the enemy's continuous position.
     * Uses cell coordinates directly with no offset per specification.
     *
     * @param enemy the enemy
     * @param path  the path being traversed
     * @return Euclidean distance in grid cells
     */
    public double distanceTo(Enemy enemy, Path path) {
        if (enemy == null) {
            throw new IllegalArgumentException("Enemy cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        ContinuousPosition enemyPos = enemy.positionOn(path);
        double dx = this.position.x() - enemyPos.x();
        double dy = this.position.y() - enemyPos.y();
        return Math.hypot(dx, dy);
    }

    /**
     * Executes one simulation tick update for this tower using a template method:
     * 1. If cooldownLeft > 0, decrement it.
     * 2. If cooldownLeft is still > 0, return AttackResult.none.
     * 3. Build candidates: alive enemies that have not reached the end, within range, and targetable.
     * 4. Ask the strategy for a target. If none, return AttackResult.none and keep tower ready (cooldown stays 0).
     * 5. Call abstract hook to resolve the attack, then set cooldownLeft = cooldownTicks.
     *
     * @param allEnemies all enemies in the simulation
     * @param path       the level path
     * @param rng        random generator for evasion rolls
     * @return AttackResult representing the outcome of this update
     */
    public final AttackResult update(List<Enemy> allEnemies, Path path, RandomGenerator rng) {
        if (allEnemies == null) {
            throw new IllegalArgumentException("allEnemies cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (rng == null) {
            throw new IllegalArgumentException("RandomGenerator cannot be null");
        }

        if (cooldownLeft > 0) {
            cooldownLeft--;
        }
        if (cooldownLeft > 0) {
            return AttackResult.none(id);
        }

        List<TargetCandidate> candidates = new ArrayList<>();
        for (Enemy enemy : allEnemies) {
            if (enemy != null && enemy.isAlive() && !enemy.hasReachedEnd(path) && canTarget(enemy)) {
                ContinuousPosition enemyPos = enemy.positionOn(path);
                double distance = Math.hypot(this.position.x() - enemyPos.x(), this.position.y() - enemyPos.y());
                if (distance <= config.range()) {
                    candidates.add(new TargetCandidate(enemy, enemyPos, distance));
                }
            }
        }

        Optional<TargetCandidate> targetOpt = strategy.selectTarget(this, candidates);
        if (targetOpt.isEmpty()) {
            return AttackResult.none(id);
        }

        Enemy target = targetOpt.get().enemy();
        List<HitResult> hits = resolveAttack(target, allEnemies, path, rng);
        this.cooldownLeft = config.cooldown();
        return new AttackResult(id, true, hits);
    }

    /**
     * Subclass hook to resolve attack mechanics against the chosen target.
     *
     * @param target     the target chosen by the targeting strategy
     * @param allEnemies all enemies currently in simulation
     * @param path       the level path
     * @param rng        random generator for evasion checks
     * @return list of individual hit outcomes
     */
    protected abstract List<HitResult> resolveAttack(Enemy target, List<Enemy> allEnemies, Path path, RandomGenerator rng);
}
