



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import model.AssetClass;
import model.AssetPosition;
import model.Portfolio;
import model.RebalancingResult;
import model.TaxLot;
import repository.InMemoryPortfolioRepository;
import service.MarketPriceProvider;
import service.RebalancingService;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting Tax-Aware Portfolio Rebalancing Engine...");

        // --- Configuration Loading ---
        // RATIONALE: In a production app, these values would come from a config file,
        // environment variables, or a configuration server.
        final double driftThreshold = 0.05;
        final BigDecimal tradeMinimum = new BigDecimal("100.00");

        // --- Dependency Initialization ---
        MarketPriceProvider priceProvider = new MarketPriceProvider();
        InMemoryPortfolioRepository portfolioRepository = new InMemoryPortfolioRepository();

        // --- Data Loading ---
        // RATIONALE: This simulates loading data from a persistent source (like a DB).
        // The repository is no longer responsible for creating its own data.
        loadSampleData(portfolioRepository);

        // --- Service Execution ---
        final Map<AssetClass, BigDecimal> currentMarketPrices = priceProvider.getCurrentMarketPrices();
        
        // The service is now created with its dependencies and configuration injected.
        RebalancingService rebalancingService = new RebalancingService(
            currentMarketPrices,
            driftThreshold,
            tradeMinimum
        );

        List<Portfolio> allPortfolios = portfolioRepository.findAll();
        System.out.printf("Found %d portfolios to process.%n", allPortfolios.size());
        System.out.println("----------------------------------------------------");

        List<CompletableFuture<RebalancingResult>> futures = rebalancingService.processPortfoliosInBatch(allPortfolios);
        List<RebalancingResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

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

    /**
     * Loads sample portfolio data into the repository.
     * This method acts as a stand-in for a data migration or ETL process.
     * @param repository The repository to load data into.
     */
    private static void loadSampleData(InMemoryPortfolioRepository repository) {
        // --- Portfolio 1: Significantly drifted, requires rebalancing ---
        var stockLots1 = List.of(
            new TaxLot(LocalDate.now().minusYears(2), new BigDecimal("100.00"), new BigDecimal("350")),
            new TaxLot(LocalDate.now().minusMonths(6), new BigDecimal("150.00"), new BigDecimal("100"))
        );
        var bondLots1 = List.of(new TaxLot(LocalDate.now().minusYears(3), new BigDecimal("95.00"), new BigDecimal("300")));
        repository.save(new Portfolio(1L, Map.of(AssetClass.STOCKS, new AssetPosition(AssetClass.STOCKS, stockLots1), AssetClass.BONDS, new AssetPosition(AssetClass.BONDS, bondLots1)), Map.of(AssetClass.STOCKS, 0.60, AssetClass.BONDS, 0.40)));

        // --- Portfolio 2: Minor drift, within threshold ---
        var stockLots2 = List.of(new TaxLot(LocalDate.now().minusYears(1).minusMonths(1), new BigDecimal("50.00"), new BigDecimal("1000")));
        var bondLots2 = List.of(new TaxLot(LocalDate.now().minusYears(1), new BigDecimal("100.00"), new BigDecimal("480")));
        repository.save(new Portfolio(2L, Map.of(AssetClass.STOCKS, new AssetPosition(AssetClass.STOCKS, stockLots2), AssetClass.BONDS, new AssetPosition(AssetClass.BONDS, bondLots2)), Map.of(AssetClass.STOCKS, 0.50, AssetClass.BONDS, 0.50)));

        // --- Portfolio 3: With tax loss harvesting opportunities ---
        var stockLots3 = List.of(
            new TaxLot(LocalDate.now().minusDays(90), new BigDecimal("220.00"), new BigDecimal("100")),
            new TaxLot(LocalDate.now().minusDays(20), new BigDecimal("210.00"), new BigDecimal("50")),
            new TaxLot(LocalDate.now().minusYears(2), new BigDecimal("100.00"), new BigDecimal("300"))
        );
        var bondLots3 = List.of(new TaxLot(LocalDate.now().minusYears(4), new BigDecimal("98.00"), new BigDecimal("150")));
        repository.save(new Portfolio(3L, Map.of(AssetClass.STOCKS, new AssetPosition(AssetClass.STOCKS, stockLots3), AssetClass.BONDS, new AssetPosition(AssetClass.BONDS, bondLots3)), Map.of(AssetClass.STOCKS, 0.40, AssetClass.BONDS, 0.60)));
    }
}