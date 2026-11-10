package com.towerlab.engine.analytics;

/**
 * Rule-based balance finding.
 * Empty list in Phase 5A; field names strictly match the frontend JSON contract.
 *
 * @param rule            rule identifier (e.g. "GR-49")
 * @param metricValue     computed metric value triggering the rule
 * @param suggestedAction short suggested balancing action
 */
public record Finding(
        String rule,
        double metricValue,
        String suggestedAction
) {}
