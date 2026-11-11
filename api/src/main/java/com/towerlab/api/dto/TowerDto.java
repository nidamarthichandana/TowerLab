package com.towerlab.api.dto;

/**
 * Tower summary for the frontend catalog.
 *
 * @param type        tower name
 * @param cost        gold purchase cost
 * @param range       attack range in grid cells
 * @param damage      base attack damage per hit
 * @param cooldown    attack cooldown in ticks
 * @param targets     target type classification ("GROUND", "AIR", "BOTH")
 * @param special     special ability description
 * @param upgradeCost cost to upgrade
 */
public record TowerDto(
        String type,
        int cost,
        double range,
        double damage,
        int cooldown,
        String targets,
        String special,
        int upgradeCost
) {}
