package service;
import model.Portfolio;
import model.*;

import model.AssetClass;
import model.AssetPosition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import static util.BigDecimalUtil.FINANCIAL_SCALE;
import static util.BigDecimalUtil.ROUNDING_MODE;

/**
 * A service dedicated to performing calculations on portfolios, such as
 * determining total market value and current allocations.
 * Separating calculations into its own class follows the Single Responsibility Principle.
 */
public class PortfolioCalculator {

    /**
     * Calculates the total market value of all assets in a portfolio.
     *
     * @param portfolio The portfolio to evaluate.
     * @param currentMarketPrices A map of current market prices for each asset class.
     * @return The total market value as a BigDecimal.
     */
    public BigDecimal calculateTotalMarketValue(Portfolio portfolio, Map<AssetClass, BigDecimal> currentMarketPrices) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(currentMarketPrices, "Market prices cannot be null");

        return portfolio.positions().values().stream()
            .map(position -> calculatePositionMarketValue(position, currentMarketPrices))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the market value of a single asset position.
     *
     * @param position The asset position.
     * @param currentMarketPrices A map of current market prices.
     * @return The market value of the position.
     */
    public BigDecimal calculatePositionMarketValue(AssetPosition position, Map<AssetClass, BigDecimal> currentMarketPrices) {
        BigDecimal currentPrice = currentMarketPrices.get(position.assetClass());
        if (currentPrice == null) {
            throw new IllegalStateException("Missing market price for asset class: " + position.assetClass());
        }
        BigDecimal totalQuantity = position.taxLots().stream()
            .map(TaxLot::quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalQuantity.multiply(currentPrice);
    }

    /**
     * Calculates the current allocation percentage for each asset class in the portfolio.
     *
     * @param portfolio The portfolio.
     * @param totalMarketValue The total market value of the portfolio.
     * @param currentMarketPrices Current market prices.
     * @return A map from asset class to its current allocation percentage.
     */
    public Map<AssetClass, Double> calculateCurrentAllocations(Portfolio portfolio, BigDecimal totalMarketValue, Map<AssetClass, BigDecimal> currentMarketPrices) {
        if (totalMarketValue.signum() == 0) {
            return portfolio.targetAllocation().keySet().stream()
                .collect(Collectors.toMap(ac -> ac, ac -> 0.0));
        }

        return portfolio.positions().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    BigDecimal positionValue = calculatePositionMarketValue(entry.getValue(), currentMarketPrices);
                    return positionValue.divide(totalMarketValue, FINANCIAL_SCALE, ROUNDING_MODE).doubleValue();
                }
            ));
    }
}