package model;

//package com.rebalancing.engine.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents the total position in a single asset class within a portfolio,
 * composed of multiple tax lots.
 *
 * @param assetClass The type of asset (e.g., STOCKS, BONDS).
 * @param taxLots A list of all tax lots that make up this position.
 */
public record AssetPosition(
    AssetClass assetClass,
    List<TaxLot> taxLots
) {
    public AssetPosition {
        if (assetClass == null || taxLots == null) {
            throw new IllegalArgumentException("Asset class and tax lots cannot be null.");
        }
        // Defensive copy to ensure the list within the record is immutable.
        taxLots = List.copyOf(taxLots);
    }
}