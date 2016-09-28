package main;


import maths.DoubleFunction;
import maths.PolynomialRegression;
import maths.RationalPolynomial;
import maths.ScoreNormalizer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BasicCharacterPattern implements CharacterPattern {

    @Override
    public final double getCertainty(PixelNodeGraph graph) {
        return getCertainty(new GraphDataSet(graph));
    }

    protected final double getCertainty(GraphDataSet dataSet) {
        return getCertainty(getCertaintyFactors(dataSet));
    }

    protected static double getCertainty(CertaintyFactor[] factors) {
        double certainty = 1;
        for (CertaintyFactor f : factors) {
            certainty *= f.getScore();
        }
        return certainty;
    }

    protected abstract CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet);

    protected static class CertaintyFactor {
        private final String name;
        private final double score;
        private final double value;
        public CertaintyFactor(String name, double score, double value) {
            this.name = name;
            this.score = score;
            this.value = value;
        }
        public String getName() {
            return this.name;
        }
        public double getScore() {
            return this.score;
        }
        public double getValue() {
            return this.value;
        }
    }

    protected static class GraphDataSet {
        private final PixelNodeGraph graph;
        private final Set<PixelNodeGraph.PixelNode> intersectionNodes;
        private final Set<PixelNodeGraph.OpenSegment> segments;
        private final double totalSegmentsDistance;
        private final Set<Set<PixelNodeGraph.OpenSegment>> loops;
        private final Map<Set<PixelNodeGraph.OpenSegment>, Double> loopsDistance;
        private final double maxCurve;
        private final PixelNodeGraph.OpenSegment longestSegment;
        private final PolynomialRegression[] longestSegmentRegressions;
        private final RationalPolynomial longestSegmentRegressionsDerivative;
        private final RationalPolynomial longestSegmentRegressions2ndDerivative;
        private final int longestSegmentRegressionSignChanges;
        private final DoubleFunction longestSegmentCurvature;
        private final int[] longestSegmentCurvaturePeaksAndTroughs;
        private final Double longestSegmentStartAngle;
        private final Double longestSegmentEndAngle;

        GraphDataSet(PixelNodeGraph graph) {
            this.graph = graph;
            intersectionNodes = graph.getVertices(3, Integer.MAX_VALUE);
            this.segments = graph.getOpenSegments();
            totalSegmentsDistance = segments.stream().mapToDouble(PixelNodeGraph.OpenSegment::getDistance).sum();
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (PixelNodeGraph.PixelNode n : getGraph().getNodes()) {
                double x = n.getLocationX();
                double y = n.getLocationY();
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            double width = maxX - minX;
            double height = maxY - minY;
            this.loops = graph.getLoop(20, segments).stream()
                    .filter(s -> s.stream().mapToInt(PixelNodeGraph.OpenSegment::getNodeLength).sum() > Math.min(width * height * 0.0002, 5))
                    .collect(Collectors.toSet());
            loopsDistance = new IdentityHashMap<>();
            for (Set<PixelNodeGraph.OpenSegment> loop : this.loops) {
                loopsDistance.put(loop, loop.stream().mapToDouble(PixelNodeGraph.OpenSegment::getDistance).sum());
            }
            double maxCurve = 0;
            for (PixelNodeGraph.OpenSegment s : segments) {
                double value = s.getCurvature3Mod();
                if (!Double.isNaN(value)) {
                    maxCurve = Math.max(maxCurve, value);
                }
            }
            this.maxCurve = maxCurve;

            longestSegment = getSegments().stream()
                    .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                    .orElse(null);
            if (longestSegment != null) {
                longestSegmentRegressions = longestSegment.getRegressionCurves(4);
                double length = longestSegment.getDistance();
                longestSegmentRegressionsDerivative = PixelNodeGraph.OpenSegment.getRegressionCurvesDerivative(
                        longestSegmentRegressions[0].getPolynomial(), longestSegmentRegressions[1].getPolynomial());
                longestSegmentRegressions2ndDerivative = longestSegmentRegressionsDerivative.differentiate();
                longestSegmentRegressionSignChanges = longestSegmentRegressions2ndDerivative
                        .getSignChangesPoints(length * 0.01, length * 0.99).size();
                longestSegmentCurvature = PixelNodeGraph.OpenSegment.getCurvatureFunction(
                        longestSegmentRegressions[0].getPolynomial(), longestSegmentRegressions[1].getPolynomial());
                longestSegmentCurvaturePeaksAndTroughs = DoubleFunction.getPeaksAndTroughsCount(
                        longestSegmentCurvature, longestSegment
                                .getDistance() * 0.1, longestSegment.getDistance() * 0.9, (int) Math.ceil(longestSegment.getDistance()) * 2);
                double startT = length * 0.05;
                double endT = length * 0.95;
                longestSegmentStartAngle = Math.atan2(longestSegmentRegressionsDerivative.getNumerator().evaluate(startT), longestSegmentRegressionsDerivative
                        .getDenominator().evaluate(startT));
                longestSegmentEndAngle = Math.atan2(longestSegmentRegressionsDerivative.getNumerator().evaluate(endT), longestSegmentRegressionsDerivative
                        .getDenominator().evaluate(endT));
            } else {
                longestSegmentRegressions = null;
                longestSegmentRegressionsDerivative = null;
                longestSegmentRegressions2ndDerivative = null;
                longestSegmentRegressionSignChanges = -1;
                longestSegmentCurvature = null;
                longestSegmentCurvaturePeaksAndTroughs = null;
                longestSegmentStartAngle = null;
                longestSegmentEndAngle = null;
            }
        }
        public PixelNodeGraph getGraph() {
            return graph;
        }

        public Set<PixelNodeGraph.PixelNode> getIntersectionNodes() {
            return intersectionNodes;
        }

        public Set<PixelNodeGraph.OpenSegment> getSegments() {
            return segments;
        }

        public double getTotalSegmentsDistance() {
            return totalSegmentsDistance;
        }

        public Set<Set<PixelNodeGraph.OpenSegment>> getLoops() {
            return loops;
        }

        Map<Set<PixelNodeGraph.OpenSegment>, Double> getLoopsDistance() {
            return loopsDistance;
        }

        public double getMaxCurve() {
            return maxCurve;
        }

        double getSegmentMax(Function<PixelNodeGraph.OpenSegment, Double> function) {
            double max = Double.NEGATIVE_INFINITY;
            for (PixelNodeGraph.OpenSegment s : this.segments) {
                Double value = function.apply(s);
                if (value != null && value > max) {
                    max = value;
                }
            }
            return max;
        }

        PixelNodeGraph.OpenSegment getLongestSegment() {
            return longestSegment;
        }

        PolynomialRegression[] getLongestSegmentRegressions() {
            return longestSegmentRegressions;
        }

        int getLongestSegmentRegressionSignChanges() {
            return longestSegmentRegressionSignChanges;
        }

        DoubleFunction getLongestSegmentCurvature() {
            return longestSegmentCurvature;
        }

        int[] getLongestSegmentCurvaturePeaksAndTroughs() {
            return longestSegmentCurvaturePeaksAndTroughs;
        }

        Double getLongestSegmentStartAngle() {
            return longestSegmentStartAngle;
        }

        Double getLongestSegmentEndAngle() {
            return longestSegmentEndAngle;
        }

        double[] curvePositiveToNegative() {
            RationalPolynomial curve2 = longestSegmentRegressions2ndDerivative;
            if (curve2 != null) {
                double curveLength = longestSegment.getDistance();
                List<Double> points = curve2.getSignChangesPoints(0, curveLength);
                points.add(0, 0.0);
                int positiveInFirstHalf = 0;
                int totalInFirstHalf = 0;
                int positiveInSecondHalf = 0;
                int totalInSecondHalf = 0;
                for (double d : points) {
                    double value = curve2.evaluate(d);
                    if (!Double.isNaN(value)) {
                        if (d < curveLength / 2.0) {
                            if (value > 0) {
                                positiveInFirstHalf++;
                            }
                            totalInFirstHalf++;
                        } else {
                            if (value > 0) {
                                positiveInSecondHalf++;
                            }
                            totalInSecondHalf++;
                        }
                    }
                }
                if (totalInFirstHalf == 0) {
                    if (curve2.evaluate(curveLength * 0.25) > 0) {
                        positiveInFirstHalf++;
                    }
                    totalInFirstHalf++;
                }
                if (totalInSecondHalf == 0) {
                    if (curve2.evaluate(curveLength * 0.75) > 0) {
                        positiveInSecondHalf++;
                    }
                    totalInSecondHalf++;
                }
                double firstHalf = (double) positiveInFirstHalf / totalInFirstHalf;
                double secondHalf = (double) positiveInSecondHalf / totalInSecondHalf;
                return new double[] {
                        firstHalf == 0 && secondHalf == 0 ? 0.5 : Math.atan(firstHalf / secondHalf) * 2.0 / Math.PI,
                        (double) (positiveInFirstHalf + positiveInSecondHalf) / (totalInFirstHalf + totalInSecondHalf)
                };
            }
            return null;
        }

        double[] curvePositiveToNegative2() {
            DoubleFunction curve = longestSegmentCurvature;
            if (curve != null) {
                double curveLength = longestSegment.getDistance();
                double[] extrema = DoubleFunction.getLocalExtremaApproximation(curve, 0, curveLength, (int) Math.ceil(curveLength) * 2);
                int positiveInFirstHalf = 0;
                int totalInFirstHalf = 0;
                int positiveInSecondHalf = 0;
                int totalInSecondHalf = 0;
                for (double d : extrema) {
                    double value = curve.evaluate(d);
                    if (!Double.isNaN(value)) {
                        if (d < curveLength / 2.0) {
                            if (value > 0) {
                                positiveInFirstHalf++;
                            }
                            totalInFirstHalf++;
                        } else {
                            if (value > 0) {
                                positiveInSecondHalf++;
                            }
                            totalInSecondHalf++;
                        }
                    }
                }
                if (totalInFirstHalf == 0) {
                    if (curve.evaluate(curveLength * 0.25) > 0) {
                        positiveInFirstHalf++;
                    }
                    totalInFirstHalf++;
                }
                if (totalInSecondHalf == 0) {
                    if (curve.evaluate(curveLength * 0.75) > 0) {
                        positiveInSecondHalf++;
                    }
                    totalInSecondHalf++;
                }
                double firstHalf = (double) positiveInFirstHalf / totalInFirstHalf;
                double secondHalf = (double) positiveInSecondHalf / totalInSecondHalf;
                return new double[] {
                        firstHalf == 0 && secondHalf == 0 ? 0.5 : Math.atan(firstHalf / secondHalf) * 2.0 / Math.PI,
                        (double) (positiveInFirstHalf + positiveInSecondHalf) / (totalInFirstHalf + totalInSecondHalf)
                };
            }
            return null;
        }

        List<CertaintyFactor> sShapeCertainty() {
            int loopsCount = this.loops.size();
            double loopsFactor = ScoreNormalizer.normalize(loopsCount, 0, 1.0, 0.95, 0.38, 0, 1.0);
            if (loopsCount > 0) {
                double largestLoopSize = this.loopsDistance.values().stream().max(Double::compare).orElse(0.0);
                loopsFactor *= 1.0 - Math.pow(largestLoopSize / this.totalSegmentsDistance, 1.5);
            }
            double turns = longestSegmentRegressionSignChanges;
            double turnsFactor = ScoreNormalizer.normalize(turns, 1.0, 2.5, 0.7, 0.4, 0, 1.0);
            double maxSkew90 = PixelNodeGraph.OpenSegment.getSkewness(getGraph().getNodes(), Math.PI / 2.0);
            double skew90Factor = ScoreNormalizer.normalize(maxSkew90, 0.25, 1.0, 0.6, 0.35, 0, 1.0);
            double maxSkew0 = PixelNodeGraph.OpenSegment.getSkewness(getGraph().getNodes(), 0);
            double skew0Factor = ScoreNormalizer.normalize(maxSkew0, 0, 0.85, 0.7, 0.35, 0, 1.0);
            int peaksCount = 0;
            int troughsCount = 0;
            double peaksFactor = 0.01;
            double troughsFactor = 0.01;
            if (longestSegmentRegressions != null) {
                peaksCount = longestSegmentCurvaturePeaksAndTroughs[0];
                troughsCount = longestSegmentCurvaturePeaksAndTroughs[1];
                if (getSegments().size() == 0) {
                    peaksFactor = ScoreNormalizer.normalize(longestSegmentCurvaturePeaksAndTroughs[0], 1.0, 0.7, 0.4, 0.4, 0, 1.0);
                    troughsFactor = ScoreNormalizer.normalize(longestSegmentCurvaturePeaksAndTroughs[1], 1.0, 0.7, 0.4, 0.4, 0, 1.0);
                } else {
                    peaksFactor = ScoreNormalizer.normalizePositive(longestSegmentCurvaturePeaksAndTroughs[0], 1.0, 1.7, 0.4, 0.3, 0, 1.0, 10.0);
                    troughsFactor = ScoreNormalizer.normalize(longestSegmentCurvaturePeaksAndTroughs[1], 1.0, 2.2, 0.4, 0.4, 0, 1.0);
                }
            }
            List<CertaintyFactor> factors = new ArrayList<>();
            factors.add(new CertaintyFactor("Loops", loopsFactor, loopsCount));
            factors.add(new CertaintyFactor("Turns", turnsFactor, turns));
            factors.add(new CertaintyFactor("Skew 90", skew90Factor, maxSkew90));
            factors.add(new CertaintyFactor("Skew 0", skew0Factor, maxSkew0));
            factors.add(new CertaintyFactor("Curve Peaks", peaksFactor, peaksCount));
            factors.add(new CertaintyFactor("Curve Troughs", troughsFactor, troughsCount));
            return factors;
        }

        static double getEndAngle(PixelNodeGraph.OpenSegment segment, int regressionDegree, boolean end1) {
            return getEndAngle(segment, regressionDegree, end1, null);
        }

        private static double getEndAngle(PixelNodeGraph.OpenSegment segment, int regressionDegree, boolean end1, Double minimumRegressionR2) {
            PolynomialRegression[] regressions = segment.getRegressionCurves(regressionDegree);
            while (minimumRegressionR2 != null && Math.min(regressions[0].getR2(), regressions[1].getR2()) < minimumRegressionR2) {
                regressionDegree++;
                regressions = segment.getRegressionCurves(regressionDegree);
            }
            return getEndAngle(regressions[0], regressions[1], segment, end1);
        }

        static double getEndAngle(PolynomialRegression regressionX, PolynomialRegression regressionY, PixelNodeGraph.OpenSegment segment, boolean end1) {
            double distanceToEnd1 = segment.getEndNode1().distanceTo(regressionX.getPolynomial().evaluate(0), regressionY.getPolynomial().evaluate(0));
            double distanceToEnd2 = segment.getEndNode2().distanceTo(regressionX.getPolynomial().evaluate(0), regressionY.getPolynomial().evaluate(0));
            double freeEndPosition = segment.getDistance() * 0.025;
            boolean invertAngle = true;
            if ((distanceToEnd1 < distanceToEnd2) != end1) {
                freeEndPosition = segment.getDistance() * 0.975;
                invertAngle = false;
            }
            RationalPolynomial derivative = PixelNodeGraph.OpenSegment.getRegressionCurvesDerivative(regressionX.getPolynomial(), regressionY.getPolynomial());
            double freeEndAngle;
            if (invertAngle) {
                freeEndAngle = Math.atan2(-derivative.getNumerator().evaluate(freeEndPosition), -derivative.getDenominator().evaluate(freeEndPosition));
            } else {
                freeEndAngle = Math.atan2(derivative.getNumerator().evaluate(freeEndPosition), derivative.getDenominator().evaluate(freeEndPosition));
            }
            return freeEndAngle;
        }

        static double getFreeEndAngle(PixelNodeGraph.OpenSegment segment, int regressionDegree) {
            return getFreeEndAngle(segment, regressionDegree, null);
        }

        static double getFreeEndAngle(PixelNodeGraph.OpenSegment segment, int regressionDegree, Double minimumRegressionR2) {
            return getEndAngle(segment, regressionDegree, segment.getEndNode1().getConnectedNodesCount() == 1, minimumRegressionR2);
        }
    }
}