package model;

//package com.rebalancing.engine.model;

import java.math.BigDecimal;

/**
 * Represents a single buy or sell order to be executed.
 *
 * @param assetClass The asset class to trade.
 * @param orderType The type of order (BUY or SELL).
 * @param quantity The number of units to trade.
 * @param marketValue The total market value of the trade.
 */
public record TradeOrder(
    AssetClass assetClass,
    OrderType orderType,
    BigDecimal quantity,
    BigDecimal marketValue
) {
    public TradeOrder {
        if (assetClass == null || orderType == null || quantity == null || marketValue == null) {
            throw new IllegalArgumentException("Trade order arguments cannot be null.");
        }
    }
}