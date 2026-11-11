package com.towerlab.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload for single-run simulation playback.
 *
 * @param levelId level identifier
 * @param seed    random number generator seed
 * @param actions list of player actions to execute
 */
public record SimulationPlayRequest(
        @NotBlank(message = "levelId is required")
        String levelId,

        @NotNull(message = "seed is required")
        Long seed,

        @NotNull(message = "actions must not be null")
        @Size(max = 200, message = "actions list cannot exceed 200 items")
        @Valid
        List<ActionDto> actions
) {}
