package repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import model.AssetClass;
import model.AssetPosition;
import model.Portfolio;
import model.TaxLot;

/**
 * An in-memory implementation of the PortfolioRepository for demonstration purposes.
 * This class simulates a database by holding portfolio data in a map.
 */
public class InMemoryPortfolioRepository implements PortfolioRepository {

    private final Map<Long, Portfolio> portfolios = new ConcurrentHashMap<>();

    public InMemoryPortfolioRepository() {
        // Initialize with sample data
        initializeSampleData();
    }

    @Override
    public Optional<Portfolio> findById(long portfolioId) {
        return Optional.ofNullable(portfolios.get(portfolioId));
    }

    @Override
    public List<Portfolio> findAll() {
        return List.copyOf(portfolios.values());
    }

    private void initializeSampleData() {
        // --- Portfolio 1: Significantly drifted, requires rebalancing ---
        var stockLots1 = List.of(
            new TaxLot(LocalDate.now().minusYears(2), new BigDecimal("100.00"), new BigDecimal("350")), // Long-term gain
            new TaxLot(LocalDate.now().minusMonths(6), new BigDecimal("150.00"), new BigDecimal("100")) // Short-term gain
        );
        var bondLots1 = List.of(
            new TaxLot(LocalDate.now().minusYears(3), new BigDecimal("95.00"), new BigDecimal("300"))
        );

        Portfolio portfolio1 = new Portfolio(
            1L,
            Map.of(
                AssetClass.STOCKS, new AssetPosition(AssetClass.STOCKS, stockLots1),
                AssetClass.BONDS, new AssetPosition(AssetClass.BONDS, bondLots1)
            ),
            Map.of(
                AssetClass.STOCKS, 0.60,
                AssetClass.BONDS, 0.40
            )
        );
        portfolios.put(1L, portfolio1);

        // --- Portfolio 2: Minor drift, within threshold, no rebalancing needed ---
        var stockLots2 = List.of(
            new TaxLot(LocalDate.now().minusYears(1).minusMonths(1), new BigDecimal("50.00"), new BigDecimal("1000"))
        );
        var bondLots2 = List.of(
            new TaxLot(LocalDate.now().minusYears(1), new BigDecimal("100.00"), new BigDecimal("480"))
        );
        Portfolio portfolio2 = new Portfolio(
            2L,
            Map.of(
                AssetClass.STOCKS, new AssetPosition(AssetClass.STOCKS, stockLots2),
                AssetClass.BONDS, new AssetPosition(AssetClass.BONDS, bondLots2)
            ),
            Map.of(
                AssetClass.STOCKS, 0.50,
                AssetClass.BONDS, 0.50
            )
        );
        portfolios.put(2L, portfolio2);

        // --- Portfolio 3: With tax loss harvesting opportunities ---
        var stockLots3 = List.of(
            new TaxLot(LocalDate.now().minusDays(90), new BigDecimal("220.00"), new BigDecimal("100")), // Short-term loss
            new TaxLot(LocalDate.now().minusDays(20), new BigDecimal("210.00"), new BigDecimal("50")), // Wash sale rule candidate
            new TaxLot(LocalDate.now().minusYears(2), new BigDecimal("100.00"), new BigDecimal("300")) // Long-term gain
        );
        var bondLots3 = List.of(
            new TaxLot(LocalDate.now().minusYears(4), new BigDecimal("98.00"), new BigDecimal("150"))
        );
        Portfolio portfolio3 = new Portfolio(
            3L,
            Map.of(
                AssetClass.STOCKS, new AssetPosition(AssetClass.STOCKS, stockLots3),
                AssetClass.BONDS, new AssetPosition(AssetClass.BONDS, bondLots3)
            ),
            Map.of(
                AssetClass.STOCKS, 0.40,
                AssetClass.BONDS, 0.60
            )
        );
        portfolios.put(3L, portfolio3);
    }
}