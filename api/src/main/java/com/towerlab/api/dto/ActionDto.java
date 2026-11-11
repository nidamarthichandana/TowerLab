package com.towerlab.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object representing a player action.
 *
 * @param type          action type ("BUILD_TOWER")
 * @param wave          target wave index
 * @param towerType     tower type to build
 * @param x             grid x-coordinate
 * @param y             grid y-coordinate
 * @param targetingMode targeting mode name (e.g. "FIRST", "LAST", "CLOSEST", "STRONGEST", "WEAKEST")
 */
public record ActionDto(
        @NotBlank(message = "type is required")
        String type,

        @NotNull(message = "wave is required")
        Integer wave,

        String towerType,

        Integer x,

        Integer y,

        String targetingMode
) {}
