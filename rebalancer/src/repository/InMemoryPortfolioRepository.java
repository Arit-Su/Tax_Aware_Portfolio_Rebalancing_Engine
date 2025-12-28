package repository;



import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import model.Portfolio;

/**
 * An in-memory implementation of the PortfolioRepository.
 * RATIONALE FOR CHANGE: The repository is now a passive storage mechanism.
 * It no longer creates its own data, adhering to the Single Responsibility Principle.
 * The data is now loaded into it by the application's entry point, simulating
 * how data would be fetched from a database and loaded into the application's memory.
 */
public class InMemoryPortfolioRepository implements PortfolioRepository {

    private final Map<Long, Portfolio> portfolios = new ConcurrentHashMap<>();

    @Override
    public Optional<Portfolio> findById(long portfolioId) {
        return Optional.ofNullable(portfolios.get(portfolioId));
    }

    @Override
    public List<Portfolio> findAll() {
        return List.copyOf(portfolios.values());
    }

    /**
     * Saves a portfolio to the in-memory store.
     * In a real DB implementation, this would be an insert/update operation.
     * @param portfolio The portfolio to save.
     */
    public void save(Portfolio portfolio) {
        if (portfolio != null) {
            portfolios.put(portfolio.portfolioId(), portfolio);
        }
    }
}