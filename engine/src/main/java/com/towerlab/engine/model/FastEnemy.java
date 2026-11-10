package com.towerlab.engine.model;

import com.towerlab.engine.config.EnemyConfig;

/**
 * Fast ground enemy (25 HP, speed 0.10, armour 0%, reward 7, damage 1, 20% evasion chance).
 */
public class FastEnemy extends Enemy {

    public FastEnemy(int id, EnemyConfig config) {
        super(id, config);
    }

    @Override
    public double evasionChance() {
        return config().evasionChance();
    }
}
