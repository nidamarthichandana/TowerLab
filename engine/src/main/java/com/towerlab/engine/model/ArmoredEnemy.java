package com.towerlab.engine.model;

import com.towerlab.engine.config.EnemyConfig;

/**
 * Armored ground enemy (120 HP, speed 0.035, armour 40%, reward 20, damage 2).
 */
public class ArmoredEnemy extends Enemy {

    public ArmoredEnemy(int id, EnemyConfig config) {
        super(id, config);
    }
}
