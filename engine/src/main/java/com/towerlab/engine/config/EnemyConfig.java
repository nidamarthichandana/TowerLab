package com.towerlab.engine.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable configuration record for an enemy type.
 *
 * @param type          the unique enemy identifier / name (e.g. "Basic", "Fast", "Armored")
 * @param tier          the feature tier ("MVP" or "Stretch")
 * @param health        maximum health (must be positive)
 * @param speed         movement speed in cells per tick (must be positive, per GR-2)
 * @param armour        damage reduction factor in [0.0, 1.0] (per GR-18)
 * @param reward        gold bounty awarded upon defeat (must be positive, per GR-17)
 * @param baseDamage    damage dealt to player base if enemy leaks (must be positive, per GR-16)
 * @param evasionChance chance in [0.0, 1.0] to evade tower attacks (per GR-19)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnemyConfig(
        @JsonAlias({"id", "enemyType", "name"}) String type,
        @JsonAlias({"tier", "featureTier"}) String tier,
        @JsonAlias({"maxHealth", "health"}) double health,
        @JsonAlias({"movementSpeed", "speed"}) double speed,
        @JsonAlias({"armor", "armour"}) double armour,
        @JsonAlias({"reward", "goldReward", "bounty"}) int reward,
        @JsonAlias({"damage", "baseDamage"}) int baseDamage,
        @JsonAlias({"evasionChance", "evasion"}) double evasionChance
) {
    public EnemyConfig {
        tier = tier == null || tier.isBlank() ? "MVP" : tier.trim();
    }

    public EnemyConfig(String type, double health, double speed, int reward, int baseDamage) {
        this(type, "MVP", health, speed, 0.0, reward, baseDamage, 0.0);
    }

    public boolean isMvp() {
        return "MVP".equalsIgnoreCase(tier);
    }

    public boolean isStretch() {
        return "Stretch".equalsIgnoreCase(tier);
    }
}
