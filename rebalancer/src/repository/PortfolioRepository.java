package repository;



import java.util.List;
import java.util.Optional;

import model.Portfolio;

/**
 * Interface defining the contract for accessing portfolio data.
 * Coding to an interface allows for flexible implementations (e.g., in-memory, database, API).
 */
public interface PortfolioRepository {
    /**
     * Finds a portfolio by its unique ID.
     *
     * @param portfolioId The ID of the portfolio.
     * @return An Optional containing the portfolio if found, otherwise empty.
     */
    Optional<Portfolio> findById(long portfolioId);

    /**
     * Retrieves all portfolios that require rebalancing.
     * In a real application, this would contain logic to query for portfolios
     * that have drifted significantly. For this simulation, it returns all.
     *
     * @return A list of all portfolios.
     */
    List<Portfolio> findAll();
}