package service;




import java.math.BigDecimal;
import java.util.Map;

import model.AssetClass;

/**
 * Provides current market prices for assets.
 * RATIONALE: This class abstracts the source of market data. In a production
 * system, the implementation of the getCurrentMarketPrices() method would be
 * changed to call a real financial data API (e.g., via HTTP). The rest of the
 * application would not need to change, as it only depends on this class's contract.
 */
public class MarketPriceProvider {

    /**
     * Fetches the current market prices.
     * For this assignment, it returns a hardcoded map.
     *
     * @return A map of asset classes to their current market prices.
     */
    public Map<AssetClass, BigDecimal> getCurrentMarketPrices() {
        // In a real application, this would be a call to a live data feed API.
        // e.g., return apiClient.getPrices("STOCKS", "BONDS");
        return Map.of(
            AssetClass.STOCKS, new BigDecimal("205.50"), // Simulating stock market growth
            AssetClass.BONDS, new BigDecimal("101.25")  // Simulating stable bond prices
        );
    }
}