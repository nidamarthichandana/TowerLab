package com.towerlab.engine.factory;

import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.model.ArmoredEnemy;
import com.towerlab.engine.model.BasicEnemy;
import com.towerlab.engine.model.Enemy;
import com.towerlab.engine.model.FastEnemy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Factory for creating concrete Enemy instances based on configured enemy specifications.
 * Uses a registry map to map enemy type names to creator functions, adhering to the Open-Closed Principle.
 */
public class EnemyFactory {

    private final Map<String, EnemyConfig> configs;
    private final Map<String, BiFunction<Integer, EnemyConfig, Enemy>> creators = new HashMap<>();

    public EnemyFactory(Map<String, EnemyConfig> configs) {
        Objects.requireNonNull(configs, "configs must not be null");
        this.configs = Map.copyOf(configs);
        registerDefaultCreators();
    }

    private void registerDefaultCreators() {
        creators.put("Basic", BasicEnemy::new);
        creators.put("Fast", FastEnemy::new);
        creators.put("Armored", ArmoredEnemy::new);
    }

    /**
     * Registers a new enemy creator function for extending enemy types without modifying existing code.
     *
     * @param type    the enemy type identifier
     * @param creator the bi-function creating an Enemy given an ID and its config
     */
    public void registerCreator(String type, BiFunction<Integer, EnemyConfig, Enemy> creator) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(creator, "creator must not be null");
        creators.put(type, creator);
    }

    /**
     * Creates an enemy instance of the specified type with the given unique id.
     *
     * @param type the enemy type name
     * @param id   the unique numeric enemy id
     * @return instantiated Enemy subclass
     * @throws InvalidConfigException if type is null, unknown, or lacks configuration
     */
    public Enemy create(String type, int id) {
        if (type == null) {
            throw new InvalidConfigException("Enemy type must not be null");
        }
        BiFunction<Integer, EnemyConfig, Enemy> creator = creators.get(type);
        if (creator == null) {
            throw new InvalidConfigException("Unknown enemy type: " + type);
        }
        EnemyConfig config = configs.get(type);
        if (config == null) {
            throw new InvalidConfigException("Missing configuration for enemy type: " + type);
        }
        return creator.apply(id, config);
    }

    /**
     * Retrieves the EnemyConfig for the specified enemy type.
     *
     * @param type enemy type name
     * @return the associated EnemyConfig
     * @throws InvalidConfigException if type is null, unknown, or lacks configuration
     */
    public EnemyConfig config(String type) {
        if (type == null) {
            throw new InvalidConfigException("Enemy type must not be null");
        }
        if (!creators.containsKey(type)) {
            throw new InvalidConfigException("Unknown enemy type: " + type);
        }
        EnemyConfig config = configs.get(type);
        if (config == null) {
            throw new InvalidConfigException("Missing configuration for enemy type: " + type);
        }
        return config;
    }
}
