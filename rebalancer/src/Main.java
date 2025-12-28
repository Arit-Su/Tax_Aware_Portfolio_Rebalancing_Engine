import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import model.AssetClass;
import model.Portfolio;
import model.RebalancingResult;
import service.RebalancingService;
import repository.*;
public class Main {

    public static void main(String[] args) {
        System.out.println("Starting Tax-Aware Portfolio Rebalancing Engine...");

        // In a real application, prices would be fetched from a live data feed.
        final Map<AssetClass, BigDecimal> currentMarketPrices = Map.of(
            AssetClass.STOCKS, new BigDecimal("205.50"), // Simulating stock market growth
            AssetClass.BONDS, new BigDecimal("101.25")  // Simulating stable bond prices
        );

        // 1. Initialize dependencies
        PortfolioRepository portfolioRepository = new InMemoryPortfolioRepository();
        RebalancingService rebalancingService = new RebalancingService(currentMarketPrices);

        // 2. Fetch all portfolios
        List<Portfolio> allPortfolios = portfolioRepository.findAll();
        System.out.printf("Found %d portfolios to process.%n", allPortfolios.size());
        System.out.println("----------------------------------------------------");

        // 3. Process portfolios in batch
        List<CompletableFuture<RebalancingResult>> futures = rebalancingService.processPortfoliosInBatch(allPortfolios);

        // 4. Wait for all tasks to complete and collect results
        List<RebalancingResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // 5. Display the results for each portfolio
        results.forEach(Main::printResult);

        System.out.println("Rebalancing engine has completed its run.");
    }

    private static void printResult(RebalancingResult result) {
        System.out.printf("===== Rebalancing Plan for Portfolio #%d =====%n", result.portfolioId());
        if (result.orders().isEmpty()) {
            System.out.println("No rebalancing needed. Portfolio is within target allocation thresholds.");
        } else {
            System.out.println("Generated Trade Orders:");
            result.orders().forEach(order ->
                System.out.printf("  - %s %s: %.4f units for a total market value of $%.2f%n",
                    order.orderType(),
                    order.assetClass(),
                    order.quantity(),
                    order.marketValue().setScale(2, RoundingMode.HALF_UP))
            );
            System.out.printf("Total Realized Gain/Loss for Tax Purposes: $%.2f%n",
                result.totalRealizedGainLoss().setScale(2, RoundingMode.HALF_UP));
        }
        System.out.println("----------------------------------------------------");
    }
}