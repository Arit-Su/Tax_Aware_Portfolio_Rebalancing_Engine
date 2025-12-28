package service;




import model.AssetClass;
import model.AssetPosition;
import model.OrderType;
import model.Portfolio;
import model.RebalancingResult;
import model.TaxLot;
import model.TradeOrder;
import util.BigDecimalUtil;
import util.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import exception.RebalancingException;
import static util.BigDecimalUtil.FINANCIAL_SCALE;
import static util.BigDecimalUtil.ROUNDING_MODE;
/**
 * Main service for orchestrating portfolio rebalancing.
 * This class handles the high-level logic of fetching portfolios, checking for drift,
 * and generating trade orders.
 */
public class RebalancingService {

    // Functional Constraints
    private static final double DRIFT_THRESHOLD = 0.05; // 5%
    private static final BigDecimal TRADE_MINIMUM = new BigDecimal("100.00");

    private final PortfolioCalculator portfolioCalculator;
    private final TaxLotSelectionService taxLotSelectionService;
    private final Map<AssetClass, BigDecimal> currentMarketPrices;

    public RebalancingService(Map<AssetClass, BigDecimal> currentMarketPrices) {
        this.portfolioCalculator = new PortfolioCalculator();
        this.taxLotSelectionService = new TaxLotSelectionService();
        this.currentMarketPrices = Objects.requireNonNull(currentMarketPrices, "Market prices cannot be null.");
    }

    /**
     * Processes a batch of portfolios concurrently to generate rebalancing plans.
     *
     * @param portfolios The list of portfolios to process.
     * @return A list of futures, each containing the rebalancing result for a portfolio.
     */
    public List<CompletableFuture<RebalancingResult>> processPortfoliosInBatch(List<Portfolio> portfolios) {
        // Using virtual threads for high concurrency without high resource cost.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return portfolios.stream()
                .map(portfolio -> CompletableFuture.supplyAsync(() -> rebalance(portfolio), executor)
                    .exceptionally(ex -> {
                        System.err.printf("Failed to rebalance portfolio %d: %s%n", portfolio.portfolioId(), ex.getMessage());
                        return new RebalancingResult(portfolio.portfolioId(), List.of(), BigDecimal.ZERO); // Return empty result on failure
                    }))
                .collect(Collectors.toList());
        }
    }

    /**
     * Rebalances a single portfolio.
     *
     * @param portfolio The portfolio to rebalance.
     * @return The result of the rebalancing, including orders and tax impact.
     */
    public RebalancingResult rebalance(Portfolio portfolio) {
        BigDecimal totalMarketValue = portfolioCalculator.calculateTotalMarketValue(portfolio, currentMarketPrices);
        Map<AssetClass, Double> currentAllocations = portfolioCalculator.calculateCurrentAllocations(portfolio, totalMarketValue, currentMarketPrices);

        List<TradeOrder> sellOrders = new ArrayList<>();
        List<TradeOrder> buyOrders = new ArrayList<>();
        BigDecimal cashFromSales = BigDecimal.ZERO;
        BigDecimal totalRealizedGainLoss = BigDecimal.ZERO;

        // Step 1: Identify and process underweight/overweight assets
        for (AssetClass assetClass : portfolio.targetAllocation().keySet()) {
            double target = portfolio.targetAllocation().get(assetClass);
            double current = currentAllocations.getOrDefault(assetClass, 0.0);
            double drift = current - target;

            if (Math.abs(drift) > DRIFT_THRESHOLD) {
                BigDecimal tradeAmount = totalMarketValue.multiply(BigDecimal.valueOf(Math.abs(drift)));

                if (drift > 0) { // Overweight -> Sell
                    if (BigDecimalUtil.isGreaterThanOrEqual(tradeAmount, TRADE_MINIMUM)) {
                        AssetPosition position = portfolio.positions().get(assetClass);
                        if (position == null) continue;

                        BigDecimal currentPrice = currentMarketPrices.get(assetClass);
                        Map<TaxLot, BigDecimal> lotsToSell = taxLotSelectionService.selectLotsForSelling(position, tradeAmount, currentPrice);

                        for (Map.Entry<TaxLot, BigDecimal> entry : lotsToSell.entrySet()) {
                            TaxLot lot = entry.getKey();
                            BigDecimal quantityToSell = entry.getValue();
                            BigDecimal saleValue = quantityToSell.multiply(currentPrice);

                            sellOrders.add(new TradeOrder(assetClass, OrderType.SELL, quantityToSell, saleValue));
                            cashFromSales = cashFromSales.add(saleValue);

                            BigDecimal costBasis = lot.purchasePrice().multiply(quantityToSell);
                            BigDecimal realizedGain = saleValue.subtract(costBasis);
                            totalRealizedGainLoss = totalRealizedGainLoss.add(realizedGain);
                        }
                    }
                }
            }
        }
        
        // Step 2: Use cash from sales to buy underweight assets
        for (AssetClass assetClass : portfolio.targetAllocation().keySet()) {
            double target = portfolio.targetAllocation().get(assetClass);
            double current = currentAllocations.getOrDefault(assetClass, 0.0);
            double drift = current - target;

            if (drift < 0 && Math.abs(drift) > DRIFT_THRESHOLD) { // Underweight -> Buy
                BigDecimal requiredAmount = totalMarketValue.multiply(BigDecimal.valueOf(Math.abs(drift)));
                BigDecimal amountToBuy = requiredAmount.min(cashFromSales);

                if (BigDecimalUtil.isGreaterThanOrEqual(amountToBuy, TRADE_MINIMUM)) {
                    BigDecimal currentPrice = currentMarketPrices.get(assetClass);
                    if (currentPrice == null || currentPrice.signum() == 0) {
                         throw new RebalancingException("Cannot execute buy order due to invalid price for " + assetClass);
                    }
                    BigDecimal quantityToBuy = amountToBuy.divide(currentPrice, FINANCIAL_SCALE, ROUNDING_MODE);
                    
                    buyOrders.add(new TradeOrder(assetClass, OrderType.BUY, quantityToBuy, amountToBuy));
                    cashFromSales = cashFromSales.subtract(amountToBuy);
                }
            }
        }

        List<TradeOrder> allOrders = new ArrayList<>(sellOrders);
        allOrders.addAll(buyOrders);
        return new RebalancingResult(portfolio.portfolioId(), allOrders, totalRealizedGainLoss);
    }
}