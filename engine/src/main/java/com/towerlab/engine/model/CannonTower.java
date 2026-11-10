package com.towerlab.engine.model;

import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.strategy.TargetingStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Concrete Tower implementing Cannon mechanics per GR-12, GR-20:
 * Deals area-of-effect damage to the target and any alive enemies within splashRadius of the target.
 * Secondary enemies do not need to be within the Cannon's own range.
 */
public class CannonTower extends Tower {

    public CannonTower(int id, TowerConfig config, Position position, TargetingStrategy strategy) {
        super(id, config, position, strategy);
    }

    @Override
    protected List<HitResult> resolveAttack(Enemy target, List<Enemy> allEnemies, Path path, RandomGenerator rng) {
        ContinuousPosition targetPos = target.positionOn(path);
        double splash = config().splashRadius();

        List<Enemy> affected = new ArrayList<>();
        for (Enemy enemy : allEnemies) {
            if (enemy != null && enemy.isAlive() && !enemy.hasReachedEnd(path) && canTarget(enemy)) {
                double dist = enemy.positionOn(path).distanceTo(targetPos);
                if (dist <= splash) {
                    affected.add(enemy);
                }
            }
        }

        if (!affected.contains(target) && target.isAlive() && !target.hasReachedEnd(path)) {
            affected.add(target);
        }

        // Process affected enemies in ascending numeric enemy ID order
        affected.sort(Comparator.comparingInt(Enemy::id));

        int rawDamage = (int) Math.round(config().damage());
        List<HitResult> hits = new ArrayList<>(affected.size());
        for (Enemy enemy : affected) {
            boolean wasAlive = enemy.isAlive();
            boolean evaded = enemy.evasionChance() > 0.0 && rng.nextDouble() < enemy.evasionChance();
            if (evaded) {
                hits.add(new HitResult(enemy.id(), 0, true, false, false));
            } else {
                int damageDealt = enemy.takeDamage(rawDamage);
                boolean killed = wasAlive && !enemy.isAlive();
                hits.add(new HitResult(enemy.id(), damageDealt, false, false, killed));
            }
        }
        return List.copyOf(hits);
    }
}
