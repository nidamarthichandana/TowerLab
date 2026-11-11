package com.towerlab.api.service;

import com.towerlab.api.dto.CatalogResponse;
import com.towerlab.api.dto.EnemyDto;
import com.towerlab.api.dto.LevelResponse;
import com.towerlab.api.dto.PointDto;
import com.towerlab.api.dto.TowerDto;
import com.towerlab.api.error.LevelNotFoundException;
import com.towerlab.engine.config.EnemyConfig;
import com.towerlab.engine.config.GameConfig;
import com.towerlab.engine.config.LevelConfig;
import com.towerlab.engine.config.TowerConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Service providing catalog information and level metadata.
 */
@Service
public class CatalogService {

    private final GameConfig gameConfig;

    public CatalogService(GameConfig gameConfig) {
        this.gameConfig = Objects.requireNonNull(gameConfig, "gameConfig must not be null");
    }

    /**
     * Retrieves the specified level configuration.
     *
     * @param id level identifier
     * @return LevelResponse
     * @throws LevelNotFoundException if the level does not exist
     */
    public LevelResponse getLevel(String id) {
        if (!"level1".equals(id)) {
            throw new LevelNotFoundException("Level not found: " + id);
        }

        LevelConfig level = gameConfig.level();
        List<PointDto> path = level.toWaypoints().stream()
                .map(pos -> new PointDto(pos.x(), pos.y()))
                .toList();

        return new LevelResponse(
                "level1",
                level.width(),
                level.height(),
                path,
                level.startingGold(),
                level.startingBaseHealth(),
                level.waves().size()
        );
    }

    /**
     * Retrieves all available MVP towers and enemies.
     *
     * @return CatalogResponse
     */
    public CatalogResponse getCatalog() {
        List<TowerDto> towers = gameConfig.towers().values().stream()
                .filter(TowerConfig::isMvp)
                .map(t -> new TowerDto(
                        t.type(),
                        t.cost(),
                        t.range(),
                        t.damage(),
                        t.cooldown(),
                        t.targets().name(),
                        t.special(),
                        t.upgradeCost()
                ))
                .toList();

        List<EnemyDto> enemies = gameConfig.enemies().values().stream()
                .filter(EnemyConfig::isMvp)
                .map(e -> new EnemyDto(
                        e.type(),
                        e.health(),
                        e.speed(),
                        e.armour(),
                        e.reward(),
                        e.baseDamage()
                ))
                .toList();

        return new CatalogResponse(towers, enemies);
    }
}
