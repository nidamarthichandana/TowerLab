package com.towerlab.engine.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable configuration record for a tower type.
 *
 * @param type         the unique tower identifier / name (e.g. "Cannon", "Sniper")
 * @param tier         the feature tier ("MVP" or "Stretch")
 * @param cost         purchase cost in gold (must be positive)
 * @param range        attack range in grid cells (must be positive)
 * @param damage       base attack damage per hit (must be positive)
 * @param cooldown     attack cooldown in integer ticks (must be positive, per GR-2)
 * @param targets      valid target types (e.g. "GROUND", "AIR", "BOTH")
 * @param upgradeCost  cost to upgrade the tower to level 2
 * @param splashRadius splash damage radius in grid cells (0.0 if single-target)
 * @param special      description of special effect or ability
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TowerConfig(
        @JsonAlias({"id", "towerType", "name"}) String type,
        @JsonAlias({"tier", "featureTier"}) String tier,
        @JsonAlias({"purchaseCost", "cost"}) int cost,
        @JsonAlias({"attackRange", "range"}) double range,
        @JsonAlias({"damage"}) double damage,
        @JsonAlias({"attackCooldown", "attackSpeed", "cooldown"}) int cooldown,
        @JsonAlias({"targets", "targetType"}) String targets,
        @JsonAlias({"upgradeCost"}) int upgradeCost,
        @JsonAlias({"splashRadius", "splash"}) double splashRadius,
        @JsonAlias({"special", "description"}) String special
) {
    public TowerConfig {
        tier = tier == null || tier.isBlank() ? "MVP" : tier.trim();
    }

    public TowerConfig(String type, int cost, double range, double damage, int cooldown) {
        this(type, "MVP", cost, range, damage, cooldown, "GROUND", 0, 0.0, null);
    }

    public TowerConfig(String type, String tier, int cost, double range, double damage, int cooldown) {
        this(type, tier, cost, range, damage, cooldown, "GROUND", 0, 0.0, null);
    }

    public boolean isMvp() {
        return "MVP".equalsIgnoreCase(tier);
    }

    public boolean isStretch() {
        return "Stretch".equalsIgnoreCase(tier);
    }
}
