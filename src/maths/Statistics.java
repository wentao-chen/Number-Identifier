package maths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class Statistics {

    private Statistics() {
    }

    public static double getMean(Collection<Double> values) {
        double mean = 0;
        int count = 0;
        for (double d : values) {
            if (Double.isFinite(d)) {
                mean += d;
                count++;
            }
        }
        return mean / count;
    }

    public static double getStandardDeviation(Collection<Double> values) {
        double mean = getMean(values);
        double variance = 0;
        int count = 0;
        for (double d : values) {
            if (Double.isFinite(d)) {
                variance += Math.pow(d - mean, 2.0);
                count++;
            }
        }
        return Math.sqrt(variance / (count + 1));
    }

    public static double[] tukeyRangeTest(Collection<Double> values) {
        return tukeyRangeTest(1.5, values);
    }

    public static double[] tukeyRangeTest(double k, Collection<Double> values) {
        if (k < 0) throw new IllegalArgumentException("k(" + k + ") must be non negative");
        java.util.List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double q1 = getQuartileSorted(sorted, 1);
        double q3 = getQuartileSorted(sorted, 3);
        double iqr = q3 - q1;
        return new double[] {q1 - k * iqr, q3 + k * iqr};
    }

    public static double getMedian(Collection<Double> values) {
        return getQuartile(values, 2);
    }

    public static double getQuartile(Collection<Double> values, int quartile) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return getQuartileSorted(sorted, quartile);
    }

    public static double getQuartileSorted(java.util.List<Double> values, int quartile) {
        if (values.size() == 0) throw new IllegalArgumentException("No values");
        if (quartile <= 0) {
            return values.get(0);
        } else if (quartile >= 4) {
            return values.get(values.size() - 1);
        } else if (quartile == 2) {
            if (values.size() % 2 == 0) {
                return (values.get(values.size() / 2 - 1) + values.get(values.size() / 2)) / 2.0;
            } else {
                return values.get(values.size() / 2);
            }
        } else {
            if (values.size() % 2 == 0) {
                if (quartile == 1) {
                    return getQuartileSorted(values.subList(0, values.size() / 2), 2);
                } else {
                    return getQuartileSorted(values.subList(values.size() / 2, values.size()), 2);
                }
            } else if (values.size() % 4 == 1) {
                if (quartile == 1) {
                    return values.get(values.size() / 4 - 1) * 0.25 + values.get(values.size() / 4) * 0.75;
                } else {
                    return values.get(values.size() / 4 * 3) * 0.75 + values.get(values.size() / 4 * 3 + 1) * 0.25;
                }
            } else {
                if (quartile == 1) {
                    return values.get(values.size() / 4) * 0.75 + values.get(values.size() / 4 + 1) * 0.25;
                } else {
                    return values.get(values.size() / 4 * 3 + 1) * 0.25 + values.get(values.size() / 4 * 3 + 2) * 0.75;
                }
            }
        }
    }
}
