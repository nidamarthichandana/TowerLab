package com.towerlab.engine.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed base interface for all game events in the simulation.
 * Serialized polymorphically via Jackson using the simple class name as the "type" discriminator.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WaveStarted.class, name = "WaveStarted"),
        @JsonSubTypes.Type(value = EnemySpawned.class, name = "EnemySpawned"),
        @JsonSubTypes.Type(value = EnemyMoved.class, name = "EnemyMoved"),
        @JsonSubTypes.Type(value = TowerBuilt.class, name = "TowerBuilt"),
        @JsonSubTypes.Type(value = TowerFired.class, name = "TowerFired"),
        @JsonSubTypes.Type(value = TowerMissed.class, name = "TowerMissed"),
        @JsonSubTypes.Type(value = EffectApplied.class, name = "EffectApplied"),
        @JsonSubTypes.Type(value = EnemyKilled.class, name = "EnemyKilled"),
        @JsonSubTypes.Type(value = LifeLost.class, name = "LifeLost"),
        @JsonSubTypes.Type(value = WaveCompleted.class, name = "WaveCompleted"),
        @JsonSubTypes.Type(value = ActionRejected.class, name = "ActionRejected"),
        @JsonSubTypes.Type(value = GameEnded.class, name = "GameEnded")
})
public sealed interface GameEvent permits
        WaveStarted,
        EnemySpawned,
        EnemyMoved,
        TowerBuilt,
        TowerFired,
        TowerMissed,
        EffectApplied,
        EnemyKilled,
        LifeLost,
        WaveCompleted,
        ActionRejected,
        GameEnded {

    /**
     * The simulation tick at which the event occurred.
     *
     * @return simulation tick number
     */
    int tick();
}
