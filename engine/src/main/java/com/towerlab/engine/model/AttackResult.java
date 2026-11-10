package com.towerlab.engine.model;

import java.util.List;

/**
 * Represents the outcome of a tower's attack update in a simulation tick.
 *
 * @param towerId the identifier of the attacking tower
 * @param fired   true if the tower fired during this tick
 * @param hits    immutable list of individual hit results
 */
public record AttackResult(
        int towerId,
        boolean fired,
        List<HitResult> hits
) {
    public AttackResult {
        hits = hits == null ? List.of() : List.copyOf(hits);
    }

    /**
     * Creates an AttackResult representing a tower that did not fire.
     *
     * @param towerId the identifier of the tower
     * @return non-firing AttackResult
     */
    public static AttackResult none(int towerId) {
        return new AttackResult(towerId, false, List.of());
    }
}
