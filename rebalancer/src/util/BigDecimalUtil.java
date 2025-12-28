package util;



import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for safe and consistent BigDecimal comparisons.
 * Direct comparison with .equals() can fail if scales are different (e.g., 5.0 vs 5.00),
 * so compareTo() is the correct method to use.
 */
public final class BigDecimalUtil {

    public static final int FINANCIAL_SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private BigDecimalUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Checks if the first value is greater than the second.
     * @return true if value1 > value2
     */
    public static boolean isGreaterThan(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) > 0;
    }

    /**
     * Checks if the first value is greater than or equal to the second.
     * @return true if value1 >= value2
     */
    public static boolean isGreaterThanOrEqual(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) >= 0;
    }

    /**
     * Checks if the first value is less than the second.
     * @return true if value1 < value2
     */
    public static boolean isLessThan(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) < 0;
    }
}