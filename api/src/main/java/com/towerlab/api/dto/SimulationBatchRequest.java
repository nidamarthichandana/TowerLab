package com.towerlab.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload for batch simulation execution.
 *
 * @param levelId level identifier
 * @param seed    base random number generator seed
 * @param runs    number of simulation runs (1 to 1000)
 * @param actions list of player actions to execute
 */
public record SimulationBatchRequest(
        @NotBlank(message = "levelId is required")
        String levelId,

        @NotNull(message = "seed is required")
        Long seed,

        @NotNull(message = "runs is required")
        @Min(value = 1, message = "runs must be between 1 and 1000")
        @Max(value = 1000, message = "runs must be between 1 and 1000")
        Integer runs,

        @NotNull(message = "actions must not be null")
        @Size(max = 200, message = "actions list cannot exceed 200 items")
        @Valid
        List<ActionDto> actions
) {}
