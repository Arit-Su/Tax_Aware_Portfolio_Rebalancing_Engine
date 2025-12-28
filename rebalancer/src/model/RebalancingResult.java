package model;

//package com.rebalancing.engine.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Encapsulates the results of a rebalancing operation for a single portfolio.
 *
 * @param portfolioId The ID of the portfolio that was rebalanced.
 * @param orders The list of generated trade orders.
 * @param totalRealizedGainLoss The total tax impact (gain or loss) of the sell orders.
 */
public record RebalancingResult(
    long portfolioId,
    List<TradeOrder> orders,
    BigDecimal totalRealizedGainLoss
) {
    public RebalancingResult {
        if (orders == null || totalRealizedGainLoss == null) {
            throw new IllegalArgumentException("Orders and realized gain/loss cannot be null.");
        }
        orders = List.copyOf(orders);
    }
}
