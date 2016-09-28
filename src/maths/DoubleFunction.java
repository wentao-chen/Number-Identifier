package maths;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public interface DoubleFunction extends Function<Double, Double> {

    double evaluate(double x);

    @Override
    default Double apply(Double x) {
        return evaluate(x);
    }

    static double newtonsMethod(DifferentiableFunction function, double y, double initialGuess, double maxError, int maxIterations) {
        return newtonsMethod(function, function.differentiate(), y, initialGuess, maxError, maxIterations);
    }

    static double newtonsMethod(Function<Double, Double> function, Function<Double, Double> derivative, double y, double initialGuess, double maxError, int maxIterations) {
        Double[] lastTwoGuesses = new Double[2];
        Double previousResult = null;
        for (int i = 0; i < maxIterations && (previousResult == null || Math.abs(previousResult - y) > maxError); i++) {
            previousResult = function.apply(initialGuess);
            initialGuess = initialGuess - (previousResult - y) / derivative.apply(initialGuess);
            if ((lastTwoGuesses[0] != null && Math.abs(initialGuess - lastTwoGuesses[0]) <= maxError) || (lastTwoGuesses[1] != null && Math.abs(initialGuess - lastTwoGuesses[1]) <= maxError)) {
                break;
            } else {
                lastTwoGuesses[0] = lastTwoGuesses[1];
                lastTwoGuesses[1] = initialGuess;
            }
        }
        return initialGuess;
    }

    static int[] getPeaksAndTroughsCount(Function<Double, Double> function, double x1, double x2, int n) {
        Boolean increasing = null;
        int peaks = 0;
        int troughs = 0;
        double lastValue = function.apply(x1);
        for (int i = 1; i <= n; i++) {
            double dx = x1 + i * (x2 - x1) / n;
            double y = function.apply(dx);

            int compare = Double.compare(y, lastValue);
            if (compare < 0 && increasing != null && increasing) {
                peaks++;
            } else if (compare > 0 && increasing != null && !increasing) {
                troughs++;
            }
            if (compare != 0) {
                increasing = compare > 0;
            }
            lastValue = y;
        }
        if (peaks == 0 && troughs == 0) {
            return new int[] {peaks, troughs, increasing != null ? increasing ? 1 : -1 : 0};
        } else {
            return new int[] {peaks, troughs};
        }
    }

    static double getMax(Function<Double, Double> function, double x1, double x2, int n) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= n; i++) {
            double dx = x1 + i * (x2 - x1) / n;
            double y = function.apply(dx);
            max = Math.max(max, y);
        }
        return max;
    }

    static double[] getLocalExtremaApproximation(Function<Double, Double> function, double x1, double x2, int n) {
        List<Double> extrema = new ArrayList<>();
        Boolean increasing = null;
        double lastValue = function.apply(x1);
        for (int i = 1; i <= n; i++) {
            double dx = x1 + i * (x2 - x1) / n;
            double y = function.apply(dx);

            int compare = Double.compare(y, lastValue);
            if ((compare < 0 && increasing != null && increasing) || (compare > 0 && increasing != null && !increasing)) {
                extrema.add(x1 + (i - 0.5) * (x2 - x1) / n);
            }
            if (compare != 0) {
                increasing = compare > 0;
            }
            lastValue = y;
        }
        return PolynomialRegression.toPrimitiveArray(extrema);
    }
}
