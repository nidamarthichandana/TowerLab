package com.towerlab.api.dto;

import java.util.List;

/**
 * Catalog response listing all available MVP towers and enemies.
 *
 * @param towers  available MVP towers
 * @param enemies available MVP enemies
 */
public record CatalogResponse(
        List<TowerDto> towers,
        List<EnemyDto> enemies
) {}
