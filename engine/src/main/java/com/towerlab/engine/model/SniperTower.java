package com.towerlab.engine.model;

import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.strategy.TargetingStrategy;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Concrete Tower implementing Sniper mechanics per GR-12:
 * Single-target high-damage attack with extended range.
 */
public class SniperTower extends Tower {

    public SniperTower(int id, TowerConfig config, Position position, TargetingStrategy strategy) {
        super(id, config, position, strategy);
    }

    @Override
    protected List<HitResult> resolveAttack(Enemy target, List<Enemy> allEnemies, Path path, RandomGenerator rng) {
        boolean wasAlive = target.isAlive();
        boolean evaded = target.evasionChance() > 0.0 && rng.nextDouble() < target.evasionChance();
        if (evaded) {
            return List.of(new HitResult(target.id(), 0, true, false, false));
        }

        int rawDamage = (int) Math.round(config().damage());
        int damageDealt = target.takeDamage(rawDamage);
        boolean killed = wasAlive && !target.isAlive();
        return List.of(new HitResult(target.id(), damageDealt, false, false, killed));
    }
}
