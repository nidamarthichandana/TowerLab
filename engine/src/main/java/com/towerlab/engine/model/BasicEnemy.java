package com.towerlab.engine.model;

import com.towerlab.engine.config.EnemyConfig;

/**
 * Basic ground enemy baseline (40 HP, speed 0.05, armour 0%, reward 8, damage 1).
 */
public class BasicEnemy extends Enemy {

    public BasicEnemy(String id, EnemyConfig config) {
        super(id, config);
    }

    public BasicEnemy(String id) {
        this(id, new EnemyConfig("Basic", 40.0, 0.05, 8, 1));
    }
}
