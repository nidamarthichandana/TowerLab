package com.towerlab.engine.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.towerlab.engine.model.Position;

/**
 * Represents a straight axis-aligned path segment connecting two grid positions.
 *
 * @param start the segment starting position
 * @param end   the segment ending position
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PathSegment(
        @JsonAlias({"from", "start"}) Position start,
        @JsonAlias({"to", "end"}) Position end
) {
}
