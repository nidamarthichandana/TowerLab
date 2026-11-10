package com.towerlab.engine.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towerlab.engine.exception.InvalidConfigException;
import com.towerlab.engine.model.Position;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and validates TowerLab game configurations from JSON files against GAME_RULES.md.
 * Supports loading from an explicitly supplied file system directory or classpath location.
 */
public class ConfigLoader {

    public static final String TOWERS_FILE = "towers.json";
    public static final String ENEMIES_FILE = "enemies.json";
    public static final String LEVEL_FILE = "level1.json";

    private final ObjectMapper objectMapper;

    public ConfigLoader() {
        this(new ObjectMapper());
    }

    public ConfigLoader(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }
        this.objectMapper = objectMapper;
    }

    /**
     * Loads and validates the game configuration from a directory on disk.
     *
     * @param directory the directory containing towers.json, enemies.json, and level1.json
     * @return the loaded and validated GameConfig
     */
    public GameConfig loadFromDirectory(Path directory) {
        if (directory == null) {
            throw new InvalidConfigException("Configuration directory path must not be null");
        }
        if (!Files.isDirectory(directory)) {
            throw new InvalidConfigException("Provided path is not a valid directory: " + directory);
        }

        Path towersPath = directory.resolve(TOWERS_FILE);
        Path enemiesPath = directory.resolve(ENEMIES_FILE);
        Path levelPath = directory.resolve(LEVEL_FILE);

        if (!Files.exists(towersPath)) {
            throw new InvalidConfigException("Required configuration file not found: " + towersPath);
        }
        if (!Files.exists(enemiesPath)) {
            throw new InvalidConfigException("Required configuration file not found: " + enemiesPath);
        }
        if (!Files.exists(levelPath)) {
            throw new InvalidConfigException("Required configuration file not found: " + levelPath);
        }

        Map<String, TowerConfig> towers = loadTowers(towersPath);
        Map<String, EnemyConfig> enemies = loadEnemies(enemiesPath);
        LevelConfig level = loadLevel(levelPath);

        validate(towers, enemies, level);
        return new GameConfig(towers, enemies, level);
    }

    /**
     * Loads and validates the game configuration from a classpath resource location.
     *
     * @param resourceBasePath base path within classpath (e.g., "", "/config", "config")
     * @return the loaded and validated GameConfig
     */
    public GameConfig loadFromClasspath(String resourceBasePath) {
        String base = resourceBasePath == null ? "" : resourceBasePath.trim();
        if (base.startsWith("/")) {
            base = base.substring(1);
        }
        if (!base.isEmpty() && !base.endsWith("/")) {
            base = base + "/";
        }

        String towersResource = base + TOWERS_FILE;
        String enemiesResource = base + ENEMIES_FILE;
        String levelResource = base + LEVEL_FILE;

        try (InputStream towersIn = getResourceStream(towersResource);
             InputStream enemiesIn = getResourceStream(enemiesResource);
             InputStream levelIn = getResourceStream(levelResource)) {

            if (towersIn == null) {
                throw new InvalidConfigException("Classpath configuration file not found: " + towersResource);
            }
            if (enemiesIn == null) {
                throw new InvalidConfigException("Classpath configuration file not found: " + enemiesResource);
            }
            if (levelIn == null) {
                throw new InvalidConfigException("Classpath configuration file not found: " + levelResource);
            }

            Map<String, TowerConfig> towers = loadTowers(towersIn);
            Map<String, EnemyConfig> enemies = loadEnemies(enemiesIn);
            LevelConfig level = loadLevel(levelIn);

            validate(towers, enemies, level);
            return new GameConfig(towers, enemies, level);
        } catch (IOException e) {
            throw new InvalidConfigException("Error reading classpath configuration resources", e);
        }
    }

    /**
     * Resolves configurations from either a file system directory or classpath location.
     *
     * @param location directory path or classpath resource location
     * @return the loaded and validated GameConfig
     */
    public GameConfig load(String location) {
        if (location == null) {
            return loadFromClasspath("");
        }
        Path path = Path.of(location);
        if (Files.isDirectory(path)) {
            return loadFromDirectory(path);
        }
        return loadFromClasspath(location);
    }

    public GameConfig load(Path directory) {
        return loadFromDirectory(directory);
    }

    public Map<String, TowerConfig> loadTowers(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return loadTowers(in);
        } catch (IOException e) {
            throw new InvalidConfigException("Failed to read towers configuration file: " + path, e);
        }
    }

    public Map<String, TowerConfig> loadTowers(InputStream in) {
        try {
            JsonNode rootNode = objectMapper.readTree(in);
            Map<String, TowerConfig> towers = new LinkedHashMap<>();
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    TowerConfig config = objectMapper.treeToValue(node, TowerConfig.class);
                    if (config.type() == null || config.type().isBlank()) {
                        throw new InvalidConfigException("Tower configuration must specify a non-empty 'type'");
                    }
                    towers.put(config.type(), config);
                }
            } else if (rootNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    TowerConfig config = objectMapper.treeToValue(entry.getValue(), TowerConfig.class);
                    String type = config.type() != null && !config.type().isBlank() ? config.type() : entry.getKey();
                    TowerConfig normalized = new TowerConfig(type, config.tier(), config.cost(), config.range(), config.damage(),
                            config.cooldown(), config.targets(), config.upgradeCost(), config.splashRadius(),
                            config.slowFraction(), config.slowDurationTicks(), config.special());
                    towers.put(type, normalized);
                }
            } else {
                throw new InvalidConfigException("towers.json must be a JSON array or object");
            }
            validateTowers(towers);
            return towers;
        } catch (IOException e) {
            throw new InvalidConfigException("Failed to parse towers JSON: " + e.getMessage(), e);
        }
    }

    public Map<String, EnemyConfig> loadEnemies(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return loadEnemies(in);
        } catch (IOException e) {
            throw new InvalidConfigException("Failed to read enemies configuration file: " + path, e);
        }
    }

    public Map<String, EnemyConfig> loadEnemies(InputStream in) {
        try {
            JsonNode rootNode = objectMapper.readTree(in);
            Map<String, EnemyConfig> enemies = new LinkedHashMap<>();
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    EnemyConfig config = objectMapper.treeToValue(node, EnemyConfig.class);
                    if (config.type() == null || config.type().isBlank()) {
                        throw new InvalidConfigException("Enemy configuration must specify a non-empty 'type'");
                    }
                    enemies.put(config.type(), config);
                }
            } else if (rootNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    EnemyConfig config = objectMapper.treeToValue(entry.getValue(), EnemyConfig.class);
                    String type = config.type() != null && !config.type().isBlank() ? config.type() : entry.getKey();
                    EnemyConfig normalized = new EnemyConfig(type, config.tier(), config.health(), config.speed(), config.armour(),
                            config.reward(), config.baseDamage(), config.evasionChance());
                    enemies.put(type, normalized);
                }
            } else {
                throw new InvalidConfigException("enemies.json must be a JSON array or object");
            }
            validateEnemies(enemies);
            return enemies;
        } catch (IOException e) {
            throw new InvalidConfigException("Failed to parse enemies JSON: " + e.getMessage(), e);
        }
    }

    public LevelConfig loadLevel(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return loadLevel(in);
        } catch (IOException e) {
            throw new InvalidConfigException("Failed to read level configuration file: " + path, e);
        }
    }

    public LevelConfig loadLevel(InputStream in) {
        try {
            JsonNode root = objectMapper.readTree(in);
            if (root == null || !root.isObject()) {
                throw new InvalidConfigException("level configuration must be a JSON object");
            }
            if (!root.has("startingGold")) {
                throw new InvalidConfigException("Level configuration is missing required 'startingGold'");
            }
            if (!root.has("startingBaseHealth")) {
                throw new InvalidConfigException("Level configuration is missing required 'startingBaseHealth'");
            }
            if (!root.has("prepPeriodTicks")) {
                throw new InvalidConfigException("Level configuration is missing required 'prepPeriodTicks'");
            }
            if (!root.has("waveClearBonus")) {
                throw new InvalidConfigException("Level configuration is missing required 'waveClearBonus'");
            }
            if (!root.has("maxTicks")) {
                throw new InvalidConfigException("Level configuration is missing required 'maxTicks'");
            }
            if (!root.has("spawnJitterTicks")) {
                throw new InvalidConfigException("Level configuration is missing required 'spawnJitterTicks'");
            }
            LevelConfig level = objectMapper.treeToValue(root, LevelConfig.class);
            validateLevel(level);
            return level;
        } catch (IOException e) {
            throw new InvalidConfigException("Failed to parse level JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Validates all requirements across towers, enemies, and level.
     */
    public void validate(Map<String, TowerConfig> towers, Map<String, EnemyConfig> enemies, LevelConfig level) {
        validateTowers(towers);
        validateEnemies(enemies);
        validateLevel(level);

        // Requirement 6: Every tower type referenced in level configuration must exist in towers.json
        if (level.allowedTowers() != null && !level.allowedTowers().isEmpty()) {
            for (String towerType : level.allowedTowers()) {
                if (towers == null || !towers.containsKey(towerType)) {
                    throw new InvalidConfigException("Level references unknown tower type: '" + towerType + "'. Allowed towers must exist in towers.json");
                }
            }
        }

        // Requirement 7: Every enemy type referenced in wave spawn groups must exist in enemies.json
        if (level.waves() != null) {
            for (WaveConfig wave : level.waves()) {
                if (wave.spawnGroups() != null) {
                    for (SpawnGroupConfig group : wave.spawnGroups()) {
                        if (enemies == null || !enemies.containsKey(group.enemyType())) {
                            throw new InvalidConfigException("Wave " + wave.waveNumber() + " spawn group references unknown enemy type: '"
                                    + group.enemyType() + "'. Enemy types must exist in enemies.json");
                        }
                    }
                }
            }
        }
    }

    public void validate(GameConfig gameConfig) {
        if (gameConfig == null) {
            throw new InvalidConfigException("GameConfig must not be null");
        }
        validate(gameConfig.towers(), gameConfig.enemies(), gameConfig.level());
    }

    public void validateTowers(Map<String, TowerConfig> towers) {
        if (towers == null || towers.isEmpty()) {
            throw new InvalidConfigException("Tower configurations must not be empty");
        }
        for (TowerConfig tower : towers.values()) {
            if (tower.type() == null || tower.type().isBlank()) {
                throw new InvalidConfigException("Tower type must not be null or blank");
            }
            if (tower.cost() <= 0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' must have positive cost, got: " + tower.cost());
            }
            if (tower.range() <= 0.0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' must have positive range, got: " + tower.range());
            }
            if (tower.damage() <= 0.0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' must have positive damage, got: " + tower.damage());
            }
            if (tower.cooldown() <= 0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' must have positive cooldown ticks, got: " + tower.cooldown());
            }
            if (tower.targets() == null) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' target capability must not be null");
            }
            if (tower.upgradeCost() < 0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' upgrade cost cannot be negative, got: " + tower.upgradeCost());
            }
            if (tower.splashRadius() < 0.0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' splash radius cannot be negative, got: " + tower.splashRadius());
            }
            if (tower.slowFraction() < 0.0 || tower.slowFraction() >= 1.0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' slow fraction must be in range [0.0, 1.0), got: " + tower.slowFraction());
            }
            if (tower.slowDurationTicks() < 0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' slow duration ticks cannot be negative, got: " + tower.slowDurationTicks());
            }
            if (tower.slowFraction() > 0.0 && tower.slowDurationTicks() <= 0) {
                throw new InvalidConfigException("Tower '" + tower.type() + "' with slow fraction must have positive slow duration ticks, got: " + tower.slowDurationTicks());
            }
        }
    }

    public void validateEnemies(Map<String, EnemyConfig> enemies) {
        if (enemies == null || enemies.isEmpty()) {
            throw new InvalidConfigException("Enemy configurations must not be empty");
        }
        for (EnemyConfig enemy : enemies.values()) {
            if (enemy.type() == null || enemy.type().isBlank()) {
                throw new InvalidConfigException("Enemy type must not be null or blank");
            }
            if (enemy.health() <= 0.0) {
                throw new InvalidConfigException("Enemy '" + enemy.type() + "' must have positive health, got: " + enemy.health());
            }
            if (enemy.speed() <= 0.0) {
                throw new InvalidConfigException("Enemy '" + enemy.type() + "' must have positive speed, got: " + enemy.speed());
            }
            if (enemy.reward() <= 0) {
                throw new InvalidConfigException("Enemy '" + enemy.type() + "' must have positive reward, got: " + enemy.reward());
            }
            if (enemy.baseDamage() <= 0) {
                throw new InvalidConfigException("Enemy '" + enemy.type() + "' must have positive base damage, got: " + enemy.baseDamage());
            }
            if (enemy.armour() < 0.0 || enemy.armour() > 1.0) {
                throw new InvalidConfigException("Enemy '" + enemy.type() + "' armour must be between 0.0 and 1.0 (0% to 100%), got: " + enemy.armour());
            }
            if (enemy.evasionChance() < 0.0 || enemy.evasionChance() > 1.0) {
                throw new InvalidConfigException("Enemy '" + enemy.type() + "' evasion chance must be between 0.0 and 1.0 (0% to 100%), got: " + enemy.evasionChance());
            }
        }
    }

    public void validateLevel(LevelConfig level) {
        if (level == null) {
            throw new InvalidConfigException("LevelConfig must not be null");
        }

        // Requirement 2: Map dimensions must be positive (GR-7)
        if (level.width() <= 0 || level.height() <= 0) {
            throw new InvalidConfigException("Map dimensions must be positive: width=" + level.width() + ", height=" + level.height());
        }

        // Starting economy, health, and preparation period must be positive (GR-4, GR-31)
        if (level.startingGold() == null) {
            throw new InvalidConfigException("Level configuration is missing required 'startingGold'");
        }
        if (level.startingGold() <= 0) {
            throw new InvalidConfigException("Starting gold must be positive, got: " + level.startingGold());
        }
        if (level.startingBaseHealth() == null) {
            throw new InvalidConfigException("Level configuration is missing required 'startingBaseHealth'");
        }
        if (level.startingBaseHealth() <= 0) {
            throw new InvalidConfigException("Starting base health must be positive, got: " + level.startingBaseHealth());
        }
        if (level.prepPeriodTicks() == null) {
            throw new InvalidConfigException("Level configuration is missing required 'prepPeriodTicks'");
        }
        if (level.prepPeriodTicks() <= 0) {
            throw new InvalidConfigException("Preparation period ticks must be positive, got: " + level.prepPeriodTicks());
        }
        if (level.waveClearBonus() == null) {
            throw new InvalidConfigException("Level configuration is missing required 'waveClearBonus'");
        }
        if (level.waveClearBonus() <= 0) {
            throw new InvalidConfigException("Wave clear bonus must be positive, got: " + level.waveClearBonus());
        }
        if (level.maxTicks() == null) {
            throw new InvalidConfigException("Level configuration is missing required 'maxTicks'");
        }
        if (level.maxTicks() <= 0) {
            throw new InvalidConfigException("Max ticks must be positive, got: " + level.maxTicks());
        }
        if (level.spawnJitterTicks() == null) {
            throw new InvalidConfigException("Level configuration is missing required 'spawnJitterTicks'");
        }
        if (level.spawnJitterTicks() <= 0) {
            throw new InvalidConfigException("Spawn jitter ticks must be positive, got: " + level.spawnJitterTicks());
        }

        // Check for contradictory path representations
        boolean hasWaypoints = level.waypoints() != null && !level.waypoints().isEmpty();
        boolean hasSegments = level.segments() != null && !level.segments().isEmpty();

        if (hasWaypoints && hasSegments) {
            List<PathSegment> derived = level.waypointsToSegments();
            if (!derived.equals(level.segments())) {
                throw new InvalidConfigException("Contradictory path configuration: 'waypoints' and 'segments' define different paths");
            }
        }

        if (!hasWaypoints && !hasSegments) {
            throw new InvalidConfigException("Level must define a valid path layout via waypoints or segments");
        }

        // Validate segments (covers both explicit segments and segments derived from waypoints)
        List<PathSegment> segmentsToValidate = level.toSegments();
        if (segmentsToValidate.isEmpty()) {
            throw new InvalidConfigException("Path must define at least one valid segment");
        }
        validateSegments(segmentsToValidate, level.width(), level.height());

        // Requirement 8: Wave spawn groups must have valid counts, intervals and delays (GR-28)
        if (level.waves() == null || level.waves().isEmpty()) {
            throw new InvalidConfigException("Level must contain at least one wave");
        }
        for (WaveConfig wave : level.waves()) {
            if (wave.spawnGroups() == null || wave.spawnGroups().isEmpty()) {
                throw new InvalidConfigException("Wave " + wave.waveNumber() + " must contain at least one spawn group");
            }
            for (SpawnGroupConfig group : wave.spawnGroups()) {
                if (group.enemyType() == null || group.enemyType().isBlank()) {
                    throw new InvalidConfigException("Wave " + wave.waveNumber() + " spawn group has null or blank enemy type");
                }
                if (group.count() <= 0) {
                    throw new InvalidConfigException("Spawn group for enemy '" + group.enemyType() + "' in wave "
                            + wave.waveNumber() + " must have positive count, got: " + group.count());
                }
                if (group.interval() <= 0) {
                    throw new InvalidConfigException("Spawn group for enemy '" + group.enemyType() + "' in wave "
                            + wave.waveNumber() + " must have positive interval, got: " + group.interval());
                }
                if (group.startDelay() < 0) {
                    throw new InvalidConfigException("Spawn group for enemy '" + group.enemyType() + "' in wave "
                            + wave.waveNumber() + " start delay cannot be negative, got: " + group.startDelay());
                }
            }
        }
    }

    private void validateSegments(List<PathSegment> segments, int width, int height) {
        for (int i = 0; i < segments.size(); i++) {
            PathSegment seg = segments.get(i);
            if (seg.start() == null || seg.end() == null) {
                throw new InvalidConfigException("Path segment " + i + " has null start or end position");
            }

            // Requirement 3: Waypoints / segment endpoints within grid
            Position start = seg.start();
            Position end = seg.end();
            if (start.x() < 0 || start.x() >= width || start.y() < 0 || start.y() >= height) {
                throw new InvalidConfigException("Segment " + i + " start (" + start.x() + ", " + start.y()
                        + ") lies outside grid bounds [0.." + (width - 1) + ", 0.." + (height - 1) + "]");
            }
            if (end.x() < 0 || end.x() >= width || end.y() < 0 || end.y() >= height) {
                throw new InvalidConfigException("Segment " + i + " end (" + end.x() + ", " + end.y()
                        + ") lies outside grid bounds [0.." + (width - 1) + ", 0.." + (height - 1) + "]");
            }

            if (start.equals(end)) {
                throw new InvalidConfigException("Segment " + i + " has zero length: (" + start.x() + ", " + start.y() + ")");
            }

            // Requirement 4: Axis-aligned (GR-8)
            if (start.x() != end.x() && start.y() != end.y()) {
                throw new InvalidConfigException("Path segment " + i + " from (" + start.x() + ", " + start.y()
                        + ") to (" + end.x() + ", " + end.y() + ") is not axis-aligned (diagonal segments not allowed)");
            }

            // Requirement 5: Consecutive path segments must be connected (GR-55)
            if (i > 0) {
                PathSegment prev = segments.get(i - 1);
                if (!prev.end().equals(start)) {
                    throw new InvalidConfigException("Consecutive path segments must be connected: segment " + (i - 1)
                            + " ends at (" + prev.end().x() + ", " + prev.end().y() + ") but segment " + i
                            + " starts at (" + start.x() + ", " + start.y() + ")");
                }
            }
        }
    }

    private InputStream getResourceStream(String resourcePath) {
        InputStream in = getClass().getResourceAsStream(resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath);
        if (in == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                in = cl.getResourceAsStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
            }
        }
        if (in == null) {
            in = ConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
        }
        return in;
    }
}
