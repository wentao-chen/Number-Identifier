package maths;

public class ScoreNormalizer {

    private ScoreNormalizer() {
    }

    private static double baseNormalizerInverse(double x, double toleratedDecreaseRate) {
        return Math.pow(1.0 - Math.tan(0.75 * Math.PI * (x - 2.0 / 3.0)), Math.pow(Math.E, -Math.tan(Math.PI * Math.sqrt(1.0 - toleratedDecreaseRate) - Math.PI / 2.0)));
    }

    private static double baseNormalizer(double value, double center, double tolerance, double toleranceValue, double toleratedDecreaseRate) {
        double toleranceFactor = Math.pow(Math.E, Math.tan(Math.PI * Math.sqrt(1.0 - toleratedDecreaseRate)- Math.PI / 2.0));
        return Math.atan(1.0 - Math.pow(baseNormalizerInverse(toleranceValue, toleratedDecreaseRate) * Math.abs(value - center) / tolerance, toleranceFactor)) / (0.75 * Math.PI) + 2.0 / 3.0;
    }

    /**
     * Returns a value in [0.0, 1.0] representing how close the value is relative to {@code center}.<br>
     *     This function is strictly increasing when {@code value} approaches {@code center}.<br>
     * @param value the value in the interval (-inf, inf) to be evaluated based on how close it is relative to {@code center}
     * @param center the value in which the function evaluates to 1.0 in the interval (-inf, inf)
     * @param tolerance a strictly positive value (0.0, inf) in which {@code value} is considered close if the distance from {@code center} is less than or equal to it
     * @param toleranceValue the value of the function in the interval (0.0, 1.0) when evaluated at a point that is {@code tolerance} from the {@code center}
     * @param toleratedDecreaseRate a value in the interval (0.0, 1.0) representing the behavior of the function near {@code center}.
     *                              A rate approaching 1.0 will cause a sharp increase when near {@code center}
     *                              while a rate approaching 0.0 will cause a sharp increase near the tolerance edges.
     *                              A rate approaching 0.0 will result in a values within the tolerance range to have a near perfect (1.0) value returned
     * @param unToleratedDecreaseRate a value greater or equal to 0 [0.0, inf) representing the decrease rate for values outside the tolerance range
     *                                where a rate of 0.0 represents no difference in the function if no decrease factor is applied and a tolerance value did not exist
     * @param unToleratedDecreaseFactor a value greater or equal to 1 [1.0, inf) applied for values outside the tolerance range
     *                                where a factor of 1.0 represents no difference in the function if no decrease rate is applied and a tolerance value did not exist
     * @return a normalized value in the range of [0.0, 1.0]
     */
    public static double normalize(double value, double center, double tolerance, double toleranceValue, double toleratedDecreaseRate, double unToleratedDecreaseRate, double unToleratedDecreaseFactor) {
        double baseValue = baseNormalizer(value, center, tolerance, toleranceValue, toleratedDecreaseRate);
        if (Math.abs(value - center) <= tolerance) {
            return baseValue;
        } else {
            return baseValue / (unToleratedDecreaseFactor * Math.pow(Math.abs(value - center) / tolerance, unToleratedDecreaseRate));
        }
    }

    /**
     * Returns a value in [0.0, 1.0] representing how close the value is relative to {@code center}.<br>
     *     This function is strictly increasing when {@code value} approaches {@code center}.<br>
     * @param value the value in the interval (-inf, inf) to be evaluated based on how close it is relative to {@code center}
     * @param center the value in which the function evaluates to 1.0 in the interval (-inf, inf)
     * @param tolerance a strictly positive value (0.0, inf) in which {@code value} is considered close if the distance from {@code center} is less than or equal to it
     * @param toleranceValue the value of the function in the interval (0.0, 1.0) when evaluated at a point that is {@code tolerance} from the {@code center}
     * @param toleratedDecreaseRate a value in the interval (0.0, 1.0) representing the behavior of the function near {@code center}.
     *                              A rate approaching 1.0 will cause a sharp increase when near {@code center}
     *                              while a rate approaching 0.0 will cause a sharp increase near the tolerance edges.
     *                              A rate approaching 0.0 will result in a values within the tolerance range to have a near perfect (1.0) value returned
     * @param unToleratedDecreaseRate a value greater or equal to 0 [0.0, inf) representing the decrease rate for values outside the tolerance range
     *                                where a rate of 0.0 represents no difference in the function if no decrease factor is applied and a tolerance value did not exist
     * @param unToleratedDecreaseFactor a value greater or equal to 1 [1.0, inf) applied for values outside the tolerance range
     *                                where a factor of 1.0 represents no difference in the function if no decrease rate is applied and a tolerance value did not exist
     * @param negativeDecreaseFactor a value greater or equal to 1 [1.0, inf) applied for values less than center
     *                                where a factor of 1.0 represents no change in the function for values less than center compared to values greater or equal to center
     * @return a normalized value in the range of [0.0, 1.0]
     * @see #normalize(double, double, double, double, double, double, double)
     */
    public static double normalizePositive(double value, double center, double tolerance, double toleranceValue, double toleratedDecreaseRate, double unToleratedDecreaseRate, double unToleratedDecreaseFactor, double negativeDecreaseFactor) {
        double baseValue = normalize(value, center, tolerance, toleranceValue, toleratedDecreaseRate, unToleratedDecreaseRate, unToleratedDecreaseFactor);
        if (value >= center) {
            return baseValue;
        } else {
            return baseValue / negativeDecreaseFactor;
        }
    }

    /**
     * Returns a value in [0.0, 1.0] representing how close the value is relative to {@code center}.<br>
     *     This function is strictly increasing when {@code value} approaches {@code center}.<br>
     * @param value the value in the interval (-inf, inf) to be evaluated based on how close it is relative to {@code center}
     * @param center the value in which the function evaluates to 1.0 in the interval (-inf, inf)
     * @param tolerance a strictly positive value (0.0, inf) in which {@code value} is considered close if the distance from {@code center} is less than or equal to it
     * @param toleranceValue the value of the function in the interval (0.0, 1.0) when evaluated at a point that is {@code tolerance} from the {@code center}
     * @param toleratedDecreaseRate a value in the interval (0.0, 1.0) representing the behavior of the function near {@code center}.
     *                              A rate approaching 1.0 will cause a sharp increase when near {@code center}
     *                              while a rate approaching 0.0 will cause a sharp increase near the tolerance edges.
     *                              A rate approaching 0.0 will result in a values within the tolerance range to have a near perfect (1.0) value returned
     * @param unToleratedDecreaseRate a value greater or equal to 0 [0.0, inf) representing the decrease rate for values outside the tolerance range
     *                                where a rate of 0.0 represents no difference in the function if no decrease factor is applied and a tolerance value did not exist
     * @param unToleratedDecreaseFactor a value greater or equal to 1 [1.0, inf) applied for values outside the tolerance range
     *                                where a factor of 1.0 represents no difference in the function if no decrease rate is applied and a tolerance value did not exist
     * @param positiveDecreaseFactor a value greater or equal to 1 [1.0, inf) applied for values less than center
     *                                where a factor of 1.0 represents no change in the function for values greater than center compared to values less or equal to center
     * @return a normalized value in the range of [0.0, 1.0]
     * @see #normalize(double, double, double, double, double, double, double)
     */
    public static double normalizeNegative(double value, double center, double tolerance, double toleranceValue, double toleratedDecreaseRate, double unToleratedDecreaseRate, double unToleratedDecreaseFactor, double positiveDecreaseFactor) {
        double baseValue = normalize(value, center, tolerance, toleranceValue, toleratedDecreaseRate, unToleratedDecreaseRate, unToleratedDecreaseFactor);
        if (value <= center) {
            return baseValue;
        } else {
            return baseValue / positiveDecreaseFactor;
        }
    }
}
