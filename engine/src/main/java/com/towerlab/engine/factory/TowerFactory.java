package com.towerlab.engine.factory;

import com.towerlab.engine.config.TowerConfig;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.model.CannonTower;
import com.towerlab.engine.model.FrostTower;
import com.towerlab.engine.model.Position;
import com.towerlab.engine.model.SniperTower;
import com.towerlab.engine.model.Tower;
import com.towerlab.engine.strategy.TargetingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory for creating concrete Tower instances based on configured tower specifications.
 * Uses a registry map to map tower type names to creator functions, adhering to OCP.
 * Rejects Stretch towers (e.g. Rapid) and unknown types with InvalidConfigException.
 */
public class TowerFactory {

    /**
     * Functional interface for instantiating a Tower.
     */
    @FunctionalInterface
    public interface TowerCreator {
        Tower create(int id, TowerConfig config, Position position, TargetingStrategy strategy);
    }

    private final Map<String, TowerConfig> configs;
    private final Map<String, TowerCreator> creators = new HashMap<>();

    public TowerFactory(Map<String, TowerConfig> configs) {
        Objects.requireNonNull(configs, "configs must not be null");
        this.configs = Map.copyOf(configs);
        registerDefaultCreators();
    }

    private void registerDefaultCreators() {
        creators.put("Cannon", CannonTower::new);
        creators.put("Sniper", SniperTower::new);
        creators.put("Frost", FrostTower::new);
    }

    /**
     * Registers a new tower creator function for extending tower types without modifying existing code.
     *
     * @param type    the tower type identifier
     * @param creator the function creating a Tower
     */
    public void registerCreator(String type, TowerCreator creator) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(creator, "creator must not be null");
        creators.put(type, creator);
    }

    /**
     * Creates a tower instance of the specified type at the given position and with the given targeting strategy.
     *
     * @param type     the tower type name
     * @param id       the unique numeric tower id
     * @param position discrete grid position where the tower is placed
     * @param strategy targeting strategy to assign to the tower
     * @return instantiated Tower subclass
     * @throws InvalidConfigException if type is null, unknown, unsupported (e.g. Rapid), or lacks configuration
     */
    public Tower create(String type, int id, Position position, TargetingStrategy strategy) {
        if (type == null) {
            throw new InvalidConfigException("Tower type must not be null");
        }
        if (position == null) {
            throw new IllegalArgumentException("Position must not be null");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("TargetingStrategy must not be null");
        }
        if ("Rapid".equalsIgnoreCase(type)) {
            throw new InvalidConfigException("Rapid tower is a Stretch feature and unsupported in MVP: " + type);
        }
        TowerCreator creator = creators.get(type);
        if (creator == null) {
            throw new InvalidConfigException("Unknown tower type: " + type);
        }
        TowerConfig config = configs.get(type);
        if (config == null) {
            throw new InvalidConfigException("Missing configuration for tower type: " + type);
        }
        return creator.create(id, config, position, strategy);
    }

    /**
     * Retrieves the TowerConfig for the specified tower type.
     *
     * @param type tower type name
     * @return the associated TowerConfig
     * @throws InvalidConfigException if type is null, unknown, unsupported (e.g. Rapid), or lacks configuration
     */
    public TowerConfig config(String type) {
        if (type == null) {
            throw new InvalidConfigException("Tower type must not be null");
        }
        if ("Rapid".equalsIgnoreCase(type)) {
            throw new InvalidConfigException("Rapid tower is a Stretch feature and unsupported in MVP: " + type);
        }
        if (!creators.containsKey(type)) {
            throw new InvalidConfigException("Unknown tower type: " + type);
        }
        TowerConfig config = configs.get(type);
        if (config == null) {
            throw new InvalidConfigException("Missing configuration for tower type: " + type);
        }
        return config;
    }
}
