package com.towerlab.api.dto;

import java.util.List;

/**
 * Representation of a level configuration for frontend display.
 *
 * @param id               level identifier (e.g. "level1")
 * @param gridWidth        width in cells
 * @param gridHeight       height in cells
 * @param path             list of path waypoints
 * @param startGold        initial gold balance
 * @param startBaseHealth  initial player base health
 * @param waveCount        total number of waves
 */
public record LevelResponse(
        String id,
        int gridWidth,
        int gridHeight,
        List<PointDto> path,
        int startGold,
        int startBaseHealth,
        int waveCount
) {}
