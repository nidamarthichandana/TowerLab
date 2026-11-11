package com.towerlab.api.error;

/**
 * Standard error response envelope.
 *
 * @param error   the error code or category
 * @param message description of the error
 */
public record ApiError(
        String error,
        String message
) {}
