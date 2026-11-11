package com.towerlab.api.dto;

/**
 * 2D grid coordinate point.
 *
 * @param x x-coordinate
 * @param y y-coordinate
 */
public record PointDto(
        int x,
        int y
) {}
