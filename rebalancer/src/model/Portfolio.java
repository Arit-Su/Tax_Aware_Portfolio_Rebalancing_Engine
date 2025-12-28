package model;

//package com.rebalancing.engine.model;

import java.util.Map;

/**
 * Represents a user's investment portfolio.
 *
 * @param portfolioId A unique identifier for the portfolio.
 * @param positions A map from asset class to the user's position in that asset.
 * @param targetAllocation A map defining the desired allocation percentage for each asset class.
 */
public record Portfolio(
    long portfolioId,
    Map<AssetClass, AssetPosition> positions,
    Map<AssetClass, Double> targetAllocation
) {
    public Portfolio {
        if (positions == null || targetAllocation == null) {
            throw new IllegalArgumentException("Positions and target allocation maps cannot be null.");
        }
        // Defensive copies to ensure immutability.
        positions = Map.copyOf(positions);
        targetAllocation = Map.copyOf(targetAllocation);
    }
}