package service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import model.AssetPosition;
import model.TaxLot;

/**
 * Service responsible for the complex logic of selecting which tax lots to sell
 * to minimize tax liability, adhering to specified rules.
 */
public class TaxLotSelectionService {

    private static final long LONG_TERM_HOLDING_PERIOD_DAYS = 365;
    private static final int WASH_SALE_PERIOD_DAYS = 30;

    /**
     * Selects specific tax lots to sell to meet the required sell value.
     *
     * @param position The asset position from which to sell.
     * @param amountToSell The target market value to sell.
     * @param currentPrice The current market price of the asset.
     * @return A map of selected tax lots and the quantity to sell from each.
     */
    public Map<TaxLot, BigDecimal> selectLotsForSelling(AssetPosition position, BigDecimal amountToSell, BigDecimal currentPrice) {
        LocalDate today = LocalDate.now();

        // Prevent selling lots that would trigger the Wash Sale Rule.
        // This is a simplification: a full implementation would check purchases across the entire portfolio.
        List<TaxLot> eligibleLots = position.taxLots().stream()
            .filter(lot -> !isWashSale(lot, today))
            .sorted(getTaxLotComparator(currentPrice, today))
            .collect(Collectors.toList());

        BigDecimal remainingAmountToSell = amountToSell;
        Map<TaxLot, BigDecimal> lotsToSell = new java.util.LinkedHashMap<>();

        for (TaxLot lot : eligibleLots) {
            if (remainingAmountToSell.signum() <= 0) break;

            BigDecimal lotValue = lot.quantity().multiply(currentPrice);
            BigDecimal quantityToSell;

            if (lotValue.compareTo(remainingAmountToSell) <= 0) {
                // Sell the entire lot
                quantityToSell = lot.quantity();
                remainingAmountToSell = remainingAmountToSell.subtract(lotValue);
            } else {
                // Sell a partial lot
                quantityToSell = remainingAmountToSell.divide(currentPrice, 8, java.math.RoundingMode.DOWN);
                remainingAmountToSell = BigDecimal.ZERO;
            }

            if (quantityToSell.signum() > 0) {
                lotsToSell.put(lot, quantityToSell);
            }
        }

        return lotsToSell;
    }

    /**
     * Creates a comparator to sort tax lots based on the tax-minimization priority.
     * Priority:
     * 1. Short-Term Losses (largest loss first)
     * 2. Long-Term Losses (largest loss first)
     * 3. Long-Term Gains (smallest gain first)
     * 4. Short-Term Gains (smallest gain first) - Avoided if possible
     */
    private Comparator<TaxLot> getTaxLotComparator(BigDecimal currentPrice, LocalDate today) {
        return Comparator.comparingInt((TaxLot lot) -> getLotPriority(lot, currentPrice, today))
            .thenComparing((lot1, lot2) -> {
                BigDecimal gain1 = calculateGain(lot1, currentPrice);
                BigDecimal gain2 = calculateGain(lot2, currentPrice);
                // For losses (priority 1, 2), we want largest loss first (descending gain)
                // For gains (priority 3, 4), we want smallest gain first (ascending gain)
                return getLotPriority(lot1, currentPrice, today) <= 2 ? gain2.compareTo(gain1) : gain1.compareTo(gain2);
            });
    }

    private int getLotPriority(TaxLot lot, BigDecimal currentPrice, LocalDate today) {
        BigDecimal gain = calculateGain(lot, currentPrice);
        boolean isLongTerm = ChronoUnit.DAYS.between(lot.purchaseDate(), today) > LONG_TERM_HOLDING_PERIOD_DAYS;

        if (gain.signum() < 0) { // Loss
            return isLongTerm ? 2 : 1; // 1: Short-Term Loss, 2: Long-Term Loss
        } else { // Gain
            return isLongTerm ? 3 : 4; // 3: Long-Term Gain, 4: Short-Term Gain
        }
    }

    private BigDecimal calculateGain(TaxLot lot, BigDecimal currentPrice) {
        return currentPrice.subtract(lot.purchasePrice());
    }

    /**
     * Checks if selling a lot for a loss would violate the Wash Sale Rule.
     * This is a simplified check. A full implementation needs to check for purchases
     * of the same asset 30 days before or after the sale.
     */
    private boolean isWashSale(TaxLot lotToSell, LocalDate saleDate) {
        long daysSincePurchase = ChronoUnit.DAYS.between(lotToSell.purchaseDate(), saleDate);
        // This rule prevents selling for a loss if the same asset was purchased within 30 days.
        return daysSincePurchase <= WASH_SALE_PERIOD_DAYS;
    }
}