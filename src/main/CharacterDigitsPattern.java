package main;

import maths.DoubleFunction;
import maths.PolynomialRegression;
import maths.ScoreNormalizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class CharacterDigitsPattern {

    private CharacterDigitsPattern() {
    }

    public static final BasicNumberCharacterPattern CHARACTER_ZERO = new BasicNumberCharacterPattern(0) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoopsDistance().keySet().stream()
                    .filter(loop -> dataSet.getLoopsDistance().get(loop) >= dataSet.getTotalSegmentsDistance() * 0.1)
                    .collect(Collectors.toSet());
            double largestLoopSize = dataSet.getLoopsDistance().values().stream().max(Double::compare).orElse(Double.NaN);
            double loopsWeightedCount = loops.stream().mapToDouble(loop -> dataSet.getLoopsDistance().get(loop) / largestLoopSize).sum();
            double loopsWeightedCountFactor = loopsWeightedCount == 0 || Double.isNaN(loopsWeightedCount) ? 0.05 : ScoreNormalizer.normalize(loopsWeightedCount, 1.0, 1.0, 0.3, 0.4, 0, 1.0);
            double maxSkew90 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI / 2.0);
            double skew90Factor = ScoreNormalizer.normalize(maxSkew90, 0, 0.25, 0.6, 0.4, 10.0, 1.0);
            double maxSkew0 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), 0.0);
            double skew0Factor = ScoreNormalizer.normalize(maxSkew0, 0, 0.32, 0.4, 0.55, 10.0, 1.0);
            double maxCurve = dataSet.getMaxCurve();
            double curveFactor = ScoreNormalizer.normalize(maxCurve, 3.5, 3.0, 0.85, 0.3, 0, 1.0);
            int turns = dataSet.getLongestSegmentRegressionSignChanges();
            double turnsFactor = ScoreNormalizer.normalize(turns, 0, 1, 0.7, 0.5, 0, 1.0);
            return new CertaintyFactor[] {
                    new CertaintyFactor("Loops-Weighted", loopsWeightedCountFactor, loopsWeightedCount),
                    new CertaintyFactor("Skew 90", skew90Factor, maxSkew90),
                    new CertaintyFactor("Skew 0", skew0Factor, maxSkew0),
                    new CertaintyFactor("Curvature", curveFactor, maxCurve),
                    new CertaintyFactor("Turns", turnsFactor, turns),
            };
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_ONE = new BasicNumberCharacterPattern(1) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            double maxCurve = dataSet.getMaxCurve();
            double maxCurveFactor = ScoreNormalizer.normalize(maxCurve, 0, 2.1, 0.2, 0.2, 0, 1.0);
            double horizontalDeviation = dataSet.getSegmentMax(s -> s.getDeviation(0.0) / s.getDistance());
            double horizontalDeviationNormalized = ScoreNormalizer.normalize(horizontalDeviation, 0, 0.1, 0.85, 0.3, 0, 1.0);
            Set<PixelNodeGraph.OpenSegment> segments = dataSet.getSegments();
            Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoops();
            return new CertaintyFactor[] {
                    new CertaintyFactor("Segments", Math.pow(segments.size(), -1.5), segments.size()),
                    new CertaintyFactor("Max Curvature", maxCurveFactor, maxCurve),
                    new CertaintyFactor("Horizontal Deviation", horizontalDeviationNormalized, horizontalDeviation),
                    new CertaintyFactor("Loops", Math.pow(loops.size() + 1, -3.0), loops.size()),
            };
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_TWO = new BasicNumberCharacterPattern(2) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            double startAngle = Double.NaN;
            double endAngle = Double.NaN;
            double startAngleFactor = 0.01;
            double endAngleFactor = 0.01;
            PolynomialRegression[] longestSegmentRegressions = dataSet.getLongestSegmentRegressions();
            PixelNodeGraph.OpenSegment longestSegment = dataSet.getLongestSegment();
            Double longestSegmentStartAngle = dataSet.getLongestSegmentStartAngle();
            Double longestSegmentEndAngle = dataSet.getLongestSegmentEndAngle();
            if (longestSegmentRegressions != null && longestSegmentStartAngle != null && longestSegmentEndAngle != null) {
                boolean startsAtTop = longestSegmentRegressions[1].getPolynomial().evaluate(0) < longestSegmentRegressions[1].getPolynomial().evaluate(longestSegment.getDistance());

                startAngle = longestSegmentStartAngle;
                endAngle = longestSegmentEndAngle;
                if (!startsAtTop) {
                    startAngle = longestSegmentEndAngle + Math.PI;
                    if (startAngle > Math.PI) {
                        startAngle -= Math.PI * 2.0;
                    }
                    endAngle = longestSegmentStartAngle + Math.PI;
                    if (endAngle > Math.PI) {
                        endAngle -= Math.PI * 2.0;
                    }
                }

                startAngleFactor = ScoreNormalizer.normalize(startAngle, -Math.PI / 4.0, Math.PI / 2.0, 0.9, 0.35, 10.0, 1.0);
                if (dataSet.getSegments().size() == 1) {
                    endAngleFactor = ScoreNormalizer.normalize(endAngle, 0, Math.PI / 3.0, 0.8, 0.3, 10.0, 1.0);
                } else {
                    Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoops();
                    PixelNodeGraph.OpenSegment longestNonLoop = dataSet.getSegments()
                            .stream().filter(s -> s != dataSet.getLongestSegment() && s.isEdge() && !loops.stream().anyMatch(l -> l.contains(s)))
                            .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                            .orElse(null);
                    if (longestNonLoop != null) {
                        endAngle = GraphDataSet.getFreeEndAngle(longestNonLoop, 2);
                        endAngleFactor = ScoreNormalizer.normalize(endAngle, 0, Math.PI / 3.0, 0.8, 0.3, 10.0, 1.0);
                    } else {
                        endAngleFactor = 1.0;
                    }
                }
            }
            double[] curvePositive = dataSet.curvePositiveToNegative();
            double curveTotalFactor = curvePositive != null ? ScoreNormalizer.normalize(curvePositive[1], 1.0, 0.2, 0.35, 0.3, 0, 1.0) : 0.05;
            double curvePositiveFactor = (curvePositive != null ? ScoreNormalizer.normalize(curvePositive[0], 0.8, 0.5, 0.2, 0.45, 0, 1.0) : 0.05);
            List<CertaintyFactor> factors = dataSet.sShapeCertainty();
            factors.add(new CertaintyFactor("Curve Direction", Math.max(curveTotalFactor, curvePositiveFactor), curvePositive != null ? curveTotalFactor > curvePositiveFactor ? curvePositive[1] : curvePositive[0] : Double.NaN));
            factors.add(new CertaintyFactor("Segment End Angle 1", startAngleFactor, Math.toDegrees(startAngle)));
            factors.add(new CertaintyFactor("Segment End Angle 2", endAngleFactor, Math.toDegrees(endAngle)));
            return factors.toArray(new CertaintyFactor[factors.size()]);
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_THREE = new BasicNumberCharacterPattern(3) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            Set<PixelNodeGraph.OpenSegment> segments = dataSet.getSegments();
            PixelNodeGraph.OpenSegment bottomMostSegment = segments.stream()
                    .filter(PixelNodeGraph.OpenSegment::isEdge)
                    .max((s1, s2) -> Double.compare(Math.max(s1.getEndNode1().getLocationY(), s1.getEndNode2().getLocationY()), Math.max(s2.getEndNode1().getLocationY(), s2.getEndNode2().getLocationY())))
                    .orElse(null);
            PixelNodeGraph.OpenSegment topMostSegment = segments.stream()
                    .filter(PixelNodeGraph.OpenSegment::isEdge)
                    .min((s1, s2) -> Double.compare(Math.min(s1.getEndNode1().getLocationY(), s1.getEndNode2().getLocationY()), Math.min(s2.getEndNode1().getLocationY(), s2.getEndNode2().getLocationY())))
                    .orElse(null);
            PolynomialRegression[] bottomSegmentRegressions = null;
            PolynomialRegression[] topSegmentRegressions = null;
            Double bottomAngle = null;
            Double topAngle = null;
            if (bottomMostSegment != null && bottomMostSegment == topMostSegment) {
                PolynomialRegression[] regressions = bottomMostSegment.getRegressionCurves(5);
                bottomAngle = GraphDataSet.getEndAngle(regressions[0], regressions[1], bottomMostSegment, bottomMostSegment.getEndNode1().getLocationY() > bottomMostSegment.getEndNode2().getLocationY());
                topAngle = GraphDataSet.getEndAngle(regressions[0], regressions[1], topMostSegment, topMostSegment.getEndNode1().getLocationY() < topMostSegment.getEndNode2().getLocationY());
            } else {
                if (bottomMostSegment != null) {
                    bottomSegmentRegressions = bottomMostSegment.getRegressionCurves(3);
                    bottomAngle = GraphDataSet.getEndAngle(bottomSegmentRegressions[0], bottomSegmentRegressions[1], bottomMostSegment, bottomMostSegment.getEndNode1().getLocationY() > bottomMostSegment.getEndNode2().getLocationY());
                }
                if (topMostSegment != null) {
                    topSegmentRegressions = topMostSegment.getRegressionCurves(3);
                    topAngle = GraphDataSet.getEndAngle(topSegmentRegressions[0], topSegmentRegressions[1], topMostSegment, topMostSegment.getEndNode1().getLocationY() < topMostSegment.getEndNode2().getLocationY());
                }
            }
            double bottomAngleFactor = 0.01;
            double topAngleFactor = 0.01;
            if (bottomAngle != null) {
                bottomAngle = (bottomAngle + (2.0 * Math.PI)) % (2.0 * Math.PI);
                bottomAngleFactor =  ScoreNormalizer.normalize(bottomAngle, 7.0 * Math.PI / 8.0, Math.PI / 2.0, 0.85, 0.28, 0, 1.0);
            }
            if (topAngle != null) {
                topAngle = (topAngle + (2.0 * Math.PI)) % (2.0 * Math.PI);
                topAngleFactor =  ScoreNormalizer.normalize(topAngle, 9.0 * Math.PI / 8.0, Math.PI / 2.0, 0.85, 0.28, 0, 1.0);
            }
            Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoops();
            int loopsCount = loops.size();
            double loopsFactor = ScoreNormalizer.normalize(loopsCount, 0, 1.0, 0.99, 0.38, 0, 1.0);
            int peaksTroughsDifference = 0;
            double peaksTroughsFactor = 0.8;
            if (dataSet.getSegments().size() == 1 && loopsCount == 0) {
                int[] peaksAndTroughs = dataSet.getLongestSegmentCurvaturePeaksAndTroughs();
                if (peaksAndTroughs != null) {
                    int most = Math.max(peaksAndTroughs[0], peaksAndTroughs[1]);
                    int least = Math.min(peaksAndTroughs[0], peaksAndTroughs[1]);
                    peaksTroughsDifference = most - least;
                    double mostFactor = ScoreNormalizer.normalizePositive(most, 2.0, 2.0, 0.7, 0.4, 0, 1.0, 10.0);
                    double leastFactor = ScoreNormalizer.normalizePositive(least, 1.0, 1.0, 0.9, 0.4, 0, 1.0, 10.0);
                    double differenceFactor = ScoreNormalizer.normalizePositive(peaksTroughsDifference, 1.0, 3.0, 0.6, 0.45, 0, 1.0, 3.0);
                    peaksTroughsFactor = mostFactor * leastFactor * differenceFactor;
                }
            } else if (loopsCount >= 0 && loopsCount <= 2) {
                if (bottomMostSegment != null && bottomSegmentRegressions != null && topSegmentRegressions != null) {
                    double bottomSegmentDistance = bottomMostSegment.getDistance();
                    double topSegmentDistance = topMostSegment.getDistance();
                    DoubleFunction bottomCurve = PixelNodeGraph.OpenSegment.getCurvatureFunction(bottomSegmentRegressions[0].getPolynomial(), bottomSegmentRegressions[1].getPolynomial());
                    DoubleFunction topCurve = PixelNodeGraph.OpenSegment.getCurvatureFunction(topSegmentRegressions[0].getPolynomial(), topSegmentRegressions[1].getPolynomial());
                    int[] bottomPeaksTroughs = DoubleFunction.getPeaksAndTroughsCount(bottomCurve, bottomSegmentDistance * 0.1, bottomSegmentDistance * 0.9, (int) Math.ceil(bottomSegmentDistance) * 2);
                    int[] topPeaksTroughs = DoubleFunction.getPeaksAndTroughsCount(topCurve, topSegmentDistance * 0.1, topSegmentDistance * 0.9, (int) Math.ceil(topSegmentDistance) * 2);
                    double bottomCurvature = PixelNodeGraph.OpenSegment.getCurvature3Mod(bottomMostSegment.getEndNode1().distanceTo(bottomMostSegment.getEndNode2()), bottomSegmentDistance);
                    double topCurvature = PixelNodeGraph.OpenSegment.getCurvature3Mod(topMostSegment.getEndNode1().distanceTo(topMostSegment.getEndNode2()), topSegmentDistance);
                    peaksTroughsFactor = ScoreNormalizer.normalize(bottomCurvature, 3.5, 1.2, 0.7, 0.3, 0, 1.0);
                    peaksTroughsFactor *= ScoreNormalizer.normalizePositive(Math.max(bottomPeaksTroughs[0], bottomPeaksTroughs[1]), 1.0, 1.5, 0.5, 0.4, 0, 1.0, 3.0);
                    peaksTroughsFactor *= ScoreNormalizer.normalize(topCurvature, 3.5, 1.2, 0.7, 0.3, 0, 1.0);
                    peaksTroughsFactor *= ScoreNormalizer.normalizePositive(Math.max(topPeaksTroughs[0], topPeaksTroughs[1]), 1.0, 1.5, 0.5, 0.4, 0, 1.0, 3.0);
                }
            }
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (PixelNodeGraph.PixelNode n : dataSet.getGraph().getNodes()) {
                double x = n.getLocationX();
                double y = n.getLocationY();
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            double width = maxX - minX;
            double height = maxY - minY;
            double maxSkew90 = Double.NaN;
            double skew90Factor = 0.01;
            double maxSkew180 = Double.NaN;
            double skew180Factor = 0.01;
            if (width >= 0 && height >= 0) {
                maxSkew90 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI / 2.0);
                skew90Factor = ScoreNormalizer.normalize(maxSkew90, 0, 0.35, 0.9, 0.25, 0, 1);
                maxSkew180 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI) / height * width;
                skew180Factor = ScoreNormalizer.normalize(maxSkew180, 0.35, 0.4, 0.8, 0.25, 0, 1);
            }
            double longestLoopDistance = loops.stream()
                    .mapToDouble(l -> l.stream().mapToDouble(PixelNodeGraph.OpenSegment::getDistance).sum())
                    .max().orElse(Double.NaN);
            double segmentToSegmentRatio = Double.NaN;
            double segmentToSegmentFactor = 1;
            double loopToSegmentRatio1 = Double.NaN;
            double loopToSegmentFactor1 = 1;
            double loopToSegmentRatio2 = Double.NaN;
            double loopToSegmentFactor2 = 1;
            Set<PixelNodeGraph.OpenSegment> nonLoops = segments.stream()
                    .filter(s -> s.isEdge() && !loops.stream().anyMatch(l -> l.contains(s))).collect(Collectors.toSet());
            PixelNodeGraph.OpenSegment longestNonLoop = nonLoops.stream()
                    .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                    .orElse(null);
            if (longestNonLoop != null) {
                PixelNodeGraph.OpenSegment longestNonLoop2 = nonLoops.stream()
                        .filter(s -> s != longestNonLoop)
                        .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                        .orElse(null);
                loopToSegmentRatio1 = longestLoopDistance / longestNonLoop.getDistance();
                if (longestNonLoop2 != null) {
                    loopToSegmentRatio2 = longestLoopDistance / longestNonLoop2.getDistance();
                    segmentToSegmentRatio = Math.log(longestNonLoop.getDistance() / longestNonLoop2.getDistance());
                }
                if (!Double.isNaN(loopToSegmentRatio1)) {
                    loopToSegmentFactor1 = ScoreNormalizer.normalize(loopToSegmentRatio1, 1.0, 1.8, 0.85, 0.3, 0, 1.0);
                }
                if (!Double.isNaN(loopToSegmentRatio2)) {
                    loopToSegmentFactor2 = ScoreNormalizer.normalize(loopToSegmentRatio2, 1.0, 1.8, 0.85, 0.3, 0, 1.0);
                }
                if (!Double.isNaN(segmentToSegmentRatio)) {
                    segmentToSegmentFactor = ScoreNormalizer.normalize(segmentToSegmentRatio, 0.0, 2.0, 0.8, 0.3, 0, 1.0);
                }
            }
            return new CertaintyFactor[] {
                    new CertaintyFactor("Segment End Angle Bottom", bottomAngleFactor, bottomAngle != null ? Math.toDegrees(bottomAngle) : Double.NaN),
                    new CertaintyFactor("Segment End Angle Top", topAngleFactor, topAngle != null ? Math.toDegrees(topAngle) : Double.NaN),
                    new CertaintyFactor("Loops", loopsFactor, loopsCount),
                    new CertaintyFactor("Skew 90", skew90Factor, maxSkew90),
                    new CertaintyFactor("Skew 180", skew180Factor, maxSkew180),
                    new CertaintyFactor("Turns", peaksTroughsFactor, peaksTroughsDifference),
                    new CertaintyFactor("Loop Segment Ratio 1", loopToSegmentFactor1, loopToSegmentRatio1),
                    new CertaintyFactor("Loop Segment Ratio 2", loopToSegmentFactor2, loopToSegmentRatio2),
                    new CertaintyFactor("Segment Segment Ratio", segmentToSegmentFactor, segmentToSegmentRatio),
            };
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_FOUR = new BasicNumberCharacterPattern(4) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            PixelNodeGraph.OpenSegment bottomMostSegment = dataSet.getSegments().stream()
                    .filter(PixelNodeGraph.OpenSegment::isEdge)
                    .max((s1, s2) -> Double.compare(Math.max(s1.getEndNode1().getLocationY(), s1.getEndNode2().getLocationY()), Math.max(s2.getEndNode1().getLocationY(), s2.getEndNode2().getLocationY())))
                    .orElse(null);
            PixelNodeGraph.OpenSegment rightMostSegment = dataSet.getSegments().stream()
                    .filter(PixelNodeGraph.OpenSegment::isEdge)
                    .max((s1, s2) -> Double.compare(Math.max(s1.getEndNode1().getLocationX(), s1.getEndNode2().getLocationX()), Math.max(s2.getEndNode1().getLocationX(), s2.getEndNode2().getLocationX())))
                    .orElse(null);
            PixelNodeGraph.OpenSegment leftMostSegment = dataSet.getLongestSegment();
            int loops = dataSet.getLoops().size();
            double loopsFactor = ScoreNormalizer.normalizeNegative(loops, 1.0, 1.0, 0.999, 0.2, 0, 1.0, 1.05);
            double maxSkew0 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI * 0);
            double maxSkew45 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI * 0.25);
            double maxSkew90 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI * 0.5);
            double maxSkew135 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI * 0.75);
            double bottomAngle = Double.NaN;
            double bottomAngleFactor = 0.01;
            if (bottomMostSegment != null) {
                PolynomialRegression[] regressions = bottomMostSegment.getRegressionCurves(1);
                bottomAngle = GraphDataSet.getEndAngle(regressions[0], regressions[1], bottomMostSegment, bottomMostSegment.getEndNode1().getConnectedNodesCount() == 1);
                bottomAngleFactor = Math.max(regressions[0].getR2(), regressions[1].getR2()) * ScoreNormalizer.normalize(bottomAngle, Math.PI / 2.0, Math.PI / 6.0, 0.9, 0.3, 0, 1.0);
            }
            double rightAngle = Double.NaN;
            double rightAngleFactor = 0.01;
            if (rightMostSegment != null) {
                rightAngle = GraphDataSet.getFreeEndAngle(rightMostSegment, 1);
                rightAngleFactor = ScoreNormalizer.normalize(rightAngle, 0, Math.PI / 6.0, 0.9, 0.3, 0, 1.0);
            }
            double[] maxCurvesLeft = null;
            int extremaCount = 0;
            double extremaCountFactor = 0;
            if (leftMostSegment != null) {
                boolean hasLoop = dataSet.getLoops().stream().anyMatch(s -> s.contains(dataSet.getLongestSegment()));
                double segmentDistance = leftMostSegment.getDistance();
                PolynomialRegression[] leftMostSegmentRegressions = leftMostSegment.getRegressionCurves(4);
                DoubleFunction curvature = PixelNodeGraph.OpenSegment.getCurvatureFunction(leftMostSegmentRegressions[0].getPolynomial(), leftMostSegmentRegressions[1].getPolynomial());
                double[] extrema = DoubleFunction.getLocalExtremaApproximation(curvature, segmentDistance * 0.05, segmentDistance * 0.95, (int) Math.ceil(segmentDistance) * 2);
                extremaCount = extrema.length - (hasLoop ? 1 : 0);
                extremaCountFactor = ScoreNormalizer.normalizePositive(extremaCount, 1.0, 2.5, 0.4, 0.23, 0, 1.0, 10.0);
                if (extrema.length > 0) {
                    double maxExtremaX = segmentDistance;
                    double maxExtremaY = Double.NEGATIVE_INFINITY;
                    double maxExtremaX2 = segmentDistance;
                    double maxExtremaY2 = Double.NEGATIVE_INFINITY;
                    for (double m : extrema) {
                        double y = Math.abs(curvature.evaluate(m));
                        if (y > maxExtremaY) {
                            maxExtremaX2 = maxExtremaX;
                            maxExtremaY2 = maxExtremaY;
                            maxExtremaX = m;
                            maxExtremaY = y;
                        } else if (y > maxExtremaY2) {
                            maxExtremaX2 = m;
                            maxExtremaY2 = y;
                        }
                    }
                    if (maxExtremaX > maxExtremaX2) {
                        double temp = maxExtremaX;
                        maxExtremaX = maxExtremaX2;
                        maxExtremaX2 = temp;
                    }
                    double linearRegression1Start = maxExtremaX * 0.2;
                    double linearRegression1End = maxExtremaX * 0.8;
                    double linearRegression2Start = maxExtremaX + (maxExtremaX2 - maxExtremaX) * 0.2;
                    double linearRegression2End = maxExtremaX + (maxExtremaX2 - maxExtremaX) * 0.8;
                    double linearRegression3Start = maxExtremaX2 + (segmentDistance - maxExtremaX2) * 0.2;
                    Double linearRegression3End = maxExtremaX2 + (segmentDistance - maxExtremaX2) * 0.8;
                    if (linearRegression3End - linearRegression3Start < segmentDistance * 0.05 || linearRegression2End - linearRegression2Start < segmentDistance * 0.05) {
                        linearRegression2End = linearRegression3End;
                        linearRegression3End = null;
                    } else if (linearRegression1End - linearRegression1Start < segmentDistance * 0.05) {
                        linearRegression1Start = linearRegression2Start;
                        linearRegression1End = linearRegression2End;
                        linearRegression2Start = linearRegression3Start;
                        linearRegression2End = linearRegression3End;
                        linearRegression3End = null;
                    }
                    boolean successfulRegression = true;
                    PolynomialRegression[] linearRegressions1 = null;
                    PolynomialRegression[] linearRegressions2 = null;
                    PolynomialRegression[] linearRegressions3 = null;
                    try {
                        linearRegressions1 = leftMostSegment.getRegressionCurves(2, linearRegression1Start, linearRegression1End);
                        linearRegressions2 = leftMostSegment.getRegressionCurves(2, linearRegression2Start, linearRegression2End);
                        linearRegressions3 = extrema.length >= 2 && linearRegression3End != null ? leftMostSegment.getRegressionCurves(2, linearRegression3Start, linearRegression3End) : null;
                    } catch (IllegalArgumentException e) {
                        successfulRegression = false;
                    }
                    if (successfulRegression && linearRegressions1 != null && linearRegressions2 != null) {
                        extremaCountFactor *= Math.min(Math.max(
                                Math.max(linearRegressions1[0].getR2(), linearRegressions1[1].getR2())
                                        * Math.max(linearRegressions2[0].getR2(), linearRegressions2[1].getR2())
                                        * (linearRegressions3 != null ? Math.max(linearRegressions3[0].getR2(), linearRegressions3[1].getR2()) : 1)
                                , 0), 1.0);
                        try {
                            double directDistance1 = Math.sqrt(Math.pow(linearRegressions1[0].getPolynomial().evaluate(linearRegression1End) - linearRegressions1[0].getPolynomial().evaluate(linearRegression1Start), 2.0)
                                    + Math.pow(linearRegressions1[1].getPolynomial().evaluate(linearRegression1End) - linearRegressions1[1].getPolynomial().evaluate(linearRegression1Start), 2.0));
                            double curveDistance1 = linearRegression1End - linearRegression1Start;
                            double directDistance2 = Math.sqrt(Math.pow(linearRegressions2[0].getPolynomial().evaluate(linearRegression2End) - linearRegressions2[0].getPolynomial().evaluate(linearRegression2Start), 2.0)
                                    + Math.pow(linearRegressions2[1].getPolynomial().evaluate(linearRegression2End) - linearRegressions2[1].getPolynomial().evaluate(linearRegression2Start), 2.0));
                            double curveDistance2 = linearRegression2End - linearRegression2Start;
                            double directDistance3;
                            double curveDistance3;
                            if (linearRegressions3 != null) {
                                directDistance3 = Math.sqrt(Math.pow(linearRegressions3[0].getPolynomial().evaluate(linearRegression3End) - linearRegressions3[0].getPolynomial().evaluate(linearRegression3Start), 2.0)
                                        + Math.pow(linearRegressions3[1].getPolynomial().evaluate(linearRegression3End) - linearRegressions3[1].getPolynomial().evaluate(linearRegression3Start), 2.0));
                                curveDistance3 = linearRegression3End - linearRegression3Start;
                            } else {
                                directDistance3 = Double.NaN;
                                curveDistance3 = Double.NaN;
                            }
                            double subSegment1Curve;
                            if (Math.abs(directDistance1 - curveDistance1) < Math.max(Math.abs(directDistance1), Math.abs(curveDistance1)) * 0.01) {
                                subSegment1Curve = 0;
                            } else {
                                subSegment1Curve = PixelNodeGraph.OpenSegment.getCurvature3Mod(directDistance1, curveDistance1);
                            }
                            double subSegment2Curve;
                            if (Math.abs(directDistance2 - curveDistance2) < Math.max(Math.abs(directDistance2), Math.abs(curveDistance2)) * 0.01) {
                                subSegment2Curve = 0;
                            } else {
                                subSegment2Curve = PixelNodeGraph.OpenSegment.getCurvature3Mod(directDistance2, curveDistance2);
                            }
                            double subSegment3Curve;
                            if (Double.isNaN(directDistance3) || Double.isNaN(curveDistance3)) {
                                subSegment3Curve = Double.NaN;
                            } else if (Math.abs(directDistance3 - curveDistance3) < Math.max(Math.abs(directDistance3), Math.abs(curveDistance3)) * 0.01) {
                                subSegment3Curve = 0;
                            } else {
                                subSegment3Curve = PixelNodeGraph.OpenSegment.getCurvature3Mod(directDistance3, curveDistance3);
                            }
                            maxCurvesLeft = Double.isNaN(subSegment3Curve) ? new double[] {subSegment1Curve, subSegment2Curve} : new double[] {subSegment1Curve, subSegment2Curve, subSegment3Curve};
                        } catch (IllegalArgumentException e) {
                            maxCurvesLeft = null;
                        }
                    } else {
                        extremaCountFactor *= 0.01;
                    }
                } else {
                    extremaCountFactor *= extremaCountFactor * extremaCountFactor;
                }

            }
            double maxCurveLeft;
            double maxCurveLeftFactor;
            if (maxCurvesLeft != null) {
                maxCurveLeft = 1.0;
                maxCurveLeftFactor = 1.0;
                for (double d : maxCurvesLeft) {
                    maxCurveLeft *= d;
                    maxCurveLeftFactor *= ScoreNormalizer.normalize(d, 0, 1.6, 0.9, 0.25, 0, 1.0);
                }
            } else {
                maxCurveLeft = dataSet.getMaxCurve();
                maxCurveLeftFactor = (1.0 - Math.pow(2.0 * Math.atan(maxCurveLeft - 2.5) / Math.PI, 2.0));
            }
            return new CertaintyFactor[] {
                    new CertaintyFactor("Loops", loopsFactor, loops),
                    new CertaintyFactor("Segment End Angle Bottom", bottomAngleFactor, Math.toDegrees(bottomAngle)),
                    new CertaintyFactor("Segment End Angle Right", rightAngleFactor, Math.toDegrees(rightAngle)),
                    new CertaintyFactor("Turns", extremaCountFactor, extremaCount),
                    new CertaintyFactor("Max Curvature Left", maxCurveLeftFactor, maxCurveLeft),
            };
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_FIVE = new BasicNumberCharacterPattern(5) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            double[] curvePositive = dataSet.curvePositiveToNegative2();
            double curvePositiveFactor = (curvePositive != null ? ScoreNormalizer.normalize(curvePositive[0], 0.2, 0.5, 0.2, 0.45, 0, 1.0) : 0.05);

            Double bottomAngle = null;
            Double topAngle = null;
            if (dataSet.getLongestSegment() != null) {
                boolean isMultipleSegments = false;
                Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoops();
                PixelNodeGraph.OpenSegment longestNonLoop = dataSet.getSegments()
                        .stream().filter(s -> s != dataSet.getLongestSegment() && s.isEdge() && !loops.stream().anyMatch(l -> l.contains(s)))
                        .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                        .orElse(null);
                if (longestNonLoop != null) {
                    if (Math.max(dataSet.getLongestSegment().getEndNode1().getLocationX(), dataSet.getLongestSegment().getEndNode2().getLocationX())
                            < Math.max(longestNonLoop.getEndNode1().getLocationX(), longestNonLoop.getEndNode2().getLocationX())) {
                        isMultipleSegments = true;
                    }
                }

                PolynomialRegression[] longestSegmentRegressions = dataSet.getLongestSegmentRegressions();
                Double longestSegmentStartAngle = dataSet.getLongestSegmentStartAngle();
                Double longestSegmentEndAngle = dataSet.getLongestSegmentEndAngle();
                if (!isMultipleSegments && longestSegmentStartAngle != null && longestSegmentRegressions != null) {
                    boolean startAtBottom = longestSegmentRegressions[1].getPolynomial().evaluate(0) > longestSegmentRegressions[1].getPolynomial().evaluate(dataSet.getLongestSegment()
                            .getDistance());
                    if (startAtBottom) {
                        bottomAngle = longestSegmentStartAngle + Math.PI;
                        topAngle = longestSegmentEndAngle;
                    } else {
                        topAngle = longestSegmentStartAngle + Math.PI;
                        if (topAngle > Math.PI) {
                            topAngle -= Math.PI * 2;
                        }
                        bottomAngle = longestSegmentEndAngle;
                    }
                } else if (isMultipleSegments) {
                    boolean longestAtBottom = Math.max(dataSet.getLongestSegment()
                            .getEndNode1().getLocationY(), dataSet.getLongestSegment().getEndNode2().getLocationY())
                            > Math.max(longestNonLoop.getEndNode1().getLocationY(), longestNonLoop.getEndNode2().getLocationY());
                    if (longestAtBottom) {
                        bottomAngle = GraphDataSet.getFreeEndAngle(dataSet.getLongestSegment(), 2, 0.8);
                        topAngle = GraphDataSet.getFreeEndAngle(longestNonLoop, 2, 0.8);
                    } else {
                        topAngle = GraphDataSet.getFreeEndAngle(dataSet.getLongestSegment(), 2, 0.8);
                        bottomAngle = GraphDataSet.getFreeEndAngle(longestNonLoop, 2, 0.8);
                    }
                }
            }
            double bottomAngleFactor = 0.01;
            double topAngleFactor = 0.01;
            if (bottomAngle != null && topAngle != null) {
                bottomAngle = (bottomAngle + Math.PI * 2) % (Math.PI * 2);
                bottomAngleFactor = ScoreNormalizer.normalize(bottomAngle, 13.0 / 12.0 * Math.PI, 5.0 / 12.0 * Math.PI, 0.7, 0.2, 10.0, 1.0);
                topAngleFactor = ScoreNormalizer.normalize(topAngle, 0, Math.PI / 3.0, 0.3, 0.3, 0, 1.0);
            }
            List<CertaintyFactor> factors = dataSet.sShapeCertainty();
            factors.add(new CertaintyFactor("Curve Direction", curvePositiveFactor, curvePositive != null ? curvePositive[0] : Double.NaN));
            factors.add(new CertaintyFactor("Segment End Angle Bottom", bottomAngleFactor, bottomAngle != null ? Math.toDegrees(bottomAngle) : Double.NaN));
            factors.add(new CertaintyFactor("Segment End Angle Top", topAngleFactor, topAngle != null ? Math.toDegrees(topAngle) : Double.NaN));
            return factors.toArray(new CertaintyFactor[factors.size()]);
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_SIX = new BasicNumberCharacterPattern(6) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoopsDistance().keySet().stream()
                    .filter(loop -> dataSet.getLoopsDistance().get(loop) >= dataSet.getTotalSegmentsDistance() * 0.1)
                    .collect(Collectors.toSet());
            double loopsFactor = ScoreNormalizer.normalizePositive(loops.size(), 1.0, 0.67, 0.8, 0.47, 0, 1.0, 10.0);
            double maxSkew270 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI * 3.0 / 2.0);
            double maxSkewFactor = ScoreNormalizer.normalize(maxSkew270, 0.5, 0.5, 0.8, 0.22, 0, 1.0);

            PixelNodeGraph.OpenSegment longestNonLoop = dataSet.getSegments()
                    .stream().filter(s -> s.isEdge() && !loops.stream().anyMatch(l -> l.contains(s)))
                    .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                    .orElse(null);
            double curvatureEstimation = Double.NaN;
            double curvatureEstimationFactor = 0.01;
            double freeEndAngle = Double.NaN;
            double freeEndAngleFactor = 0.01;
            if (longestNonLoop != null) {
                curvatureEstimation = longestNonLoop.getCurvature3Mod();
                curvatureEstimationFactor = ScoreNormalizer.normalize(curvatureEstimation, 2.4, 2.0, 0.6, 0.3, 0, 1.0);

                freeEndAngle = GraphDataSet.getFreeEndAngle(longestNonLoop, 2);
                freeEndAngleFactor = ScoreNormalizer.normalize(freeEndAngle, 0, Math.PI / 2.0, 0.9, 0.3, 10.0, 1.0);
            }
            PixelNodeGraph.OpenSegment longestRemainingSegment = dataSet.getSegments()
                    .stream().filter(s -> s.isEdge() && s != longestNonLoop && !loops.stream().anyMatch(l -> l.contains(s)))
                    .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                    .orElse(null);
            double remainingSegmentsRatio = Double.NaN;
            double remainingSegmentsFactor = 1;
            if (longestNonLoop != null && longestRemainingSegment != null) {
                remainingSegmentsRatio = longestRemainingSegment.getDistance() / longestNonLoop.getDistance();
                remainingSegmentsFactor = ScoreNormalizer.normalize(remainingSegmentsRatio, 0, 0.6, 0.5, 0.3, 0, 1.0);
            }
            double longestLoopDistance = loops.stream()
                    .mapToDouble(l -> l.stream().mapToDouble(PixelNodeGraph.OpenSegment::getDistance).sum())
                    .max().orElse(Double.NaN);
            double loopToSegmentRatio = Double.NaN;
            double loopToSegmentFactor = 0.01;
            if (!Double.isNaN(longestLoopDistance) && longestNonLoop != null) {
                loopToSegmentRatio = longestLoopDistance / longestNonLoop.getDistance();
                loopToSegmentFactor = ScoreNormalizer.normalize(loopToSegmentRatio, 2.75, 1.75, 0.85, 0.2, 0, 1.0);
            }
            return new CertaintyFactor[] {
                    new CertaintyFactor("Loops", loopsFactor, loops.size()),
                    new CertaintyFactor("Max Skew", maxSkewFactor, maxSkew270),
                    new CertaintyFactor("Curvature", curvatureEstimationFactor, curvatureEstimation),
                    new CertaintyFactor("Segment End Angle", freeEndAngleFactor, Math.toDegrees(freeEndAngle)),
                    new CertaintyFactor("Segments Ratio", remainingSegmentsFactor, remainingSegmentsRatio),
                    new CertaintyFactor("Loop-Segment Ratio", loopToSegmentFactor, loopToSegmentRatio),
            };
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_SEVEN = new BasicNumberCharacterPattern(7) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            double maxSkew90 = dataSet.getSegmentMax(s -> s.getSkewness(Math.PI / 2.0));
            double maxSkew120 = dataSet.getSegmentMax(s -> s.getSkewness(Math.PI * 2.0 / 3.0));
            double maxSkew180 = dataSet.getSegmentMax(s -> s.getSkewness(Math.PI));
            // double skew90Normalized = ScoreNormalizer.normalize(maxSkew90, 0.65, 0.6, 0.4, 0.4, 3.0, 1.0);
            // skew90Normalized /= maxSkew90 >= 0 ? 1.0 : 4.0;
            double skew120Normalized = ScoreNormalizer.normalize(maxSkew120, 0.65, 0.65, 0.4, 0.25, 3.0, 1.0);
            skew120Normalized /= maxSkew120 >= 0 ? 1.0 : 4.0;
            // double skew180Normalized = ScoreNormalizer.normalize(maxSkew180, 0.9, 0.85, 0.7, 0.25, 0, 1.0);
            // skew180Normalized /= maxSkew180 >= 0 ? 1.0 : 4.0;

            double[] maxCurves = null;
            int extremaCount = 0;
            double extremaCountFactor = 0;
            DoubleFunction longestSegmentCurvature = dataSet.getLongestSegmentCurvature();
            if (longestSegmentCurvature != null) {
                double segmentDistance = dataSet.getLongestSegment().getDistance();
                double[] extrema = DoubleFunction.getLocalExtremaApproximation(longestSegmentCurvature, 0, segmentDistance, (int) Math.ceil(segmentDistance) * 2);
                extremaCount = extrema.length;
                extremaCountFactor = ScoreNormalizer.normalizePositive(extremaCount, 1.0, 2.5, 0.4, 0.23, 0, 1.0, 10.0);
                if (extrema.length > 0) {
                    double maxExtremaX = extrema[0];
                    double maxExtremaY = longestSegmentCurvature.evaluate(maxExtremaX);
                    for (int i = 1; i < extrema.length; i++) {
                        double y = longestSegmentCurvature.evaluate(extrema[i]);
                        if (y > maxExtremaY) {
                            maxExtremaX = extrema[i];
                            maxExtremaY = y;
                        }
                    }
                    double linearRegression1Start = maxExtremaX * 0.2;
                    double linearRegression1End = maxExtremaX * 0.8;
                    double linearRegression2Start = maxExtremaX + (segmentDistance - maxExtremaX) * 0.2;
                    double linearRegression2End = maxExtremaX + (segmentDistance - maxExtremaX) * 0.8;
                    boolean successfulRegression = true;
                    PolynomialRegression[] linearRegressions1 = null;
                    PolynomialRegression[] linearRegressions2 = null;
                    try {
                        linearRegressions1 = dataSet.getLongestSegment()
                                .getRegressionCurves(1, linearRegression1Start, linearRegression1End);
                        linearRegressions2 = dataSet.getLongestSegment()
                                .getRegressionCurves(1, linearRegression2Start, linearRegression2End);
                    } catch (IllegalArgumentException e) {
                        successfulRegression = false;
                    }
                    if (successfulRegression && linearRegressions1 != null && linearRegressions2 != null) {
                        extremaCountFactor *= Math.max(linearRegressions1[0].getR2(), linearRegressions1[1].getR2()) * Math.max(linearRegressions2[0].getR2(), linearRegressions2[1].getR2());
                        try {
                            double directDistance1 = Math.sqrt(Math.pow(linearRegressions1[0].getPolynomial().evaluate(linearRegression1End) - linearRegressions1[0].getPolynomial().evaluate(linearRegression1Start), 2.0)
                                    + Math.pow(linearRegressions1[1].getPolynomial().evaluate(linearRegression1End) - linearRegressions1[1].getPolynomial().evaluate(linearRegression1Start), 2.0));
                            double curveDistance1 = linearRegression1End - linearRegression1Start;
                            double directDistance2 = Math.sqrt(Math.pow(linearRegressions2[0].getPolynomial().evaluate(linearRegression2End) - linearRegressions2[0].getPolynomial().evaluate(linearRegression2Start), 2.0)
                                    + Math.pow(linearRegressions2[1].getPolynomial().evaluate(linearRegression2End) - linearRegressions2[1].getPolynomial().evaluate(linearRegression2Start), 2.0));
                            double curveDistance2 = linearRegression2End - linearRegression2Start;
                            double subSegment1Curve;
                            if (Math.abs(directDistance1 - curveDistance1) < Math.max(Math.abs(directDistance1), Math.abs(curveDistance1)) * 0.01) {
                                subSegment1Curve = 0;
                            } else {
                                subSegment1Curve = PixelNodeGraph.OpenSegment.getCurvature3Mod(directDistance1, curveDistance1);
                            }
                            double subSegment2Curve;
                            if (Math.abs(directDistance2 - curveDistance2) < Math.max(Math.abs(directDistance2), Math.abs(curveDistance2)) * 0.01) {
                                subSegment2Curve = 0;
                            } else {
                                subSegment2Curve = PixelNodeGraph.OpenSegment.getCurvature3Mod(directDistance2, curveDistance2);
                            }
                            maxCurves = new double[] {subSegment1Curve, subSegment2Curve};
                        } catch (IllegalArgumentException e) {
                            maxCurves = null;
                        }
                    } else {
                        extremaCountFactor *= 0.01;
                    }
                } else {
                    extremaCountFactor *= extremaCountFactor * extremaCountFactor;
                }

            }
            double maxCurve = Double.NaN;
            double maxCurveFactor;
            if (maxCurves != null) {
                maxCurve = maxCurves[0] * maxCurves[1];
                maxCurveFactor = ScoreNormalizer.normalize(maxCurves[0], 0, 1.6, 0.9, 0.25, 0, 1.0);
                maxCurveFactor *= ScoreNormalizer.normalize(maxCurves[1], 0, 1.6, 0.9, 0.25, 0, 1.0);
            } else {
                maxCurveFactor = (1.0 - Math.pow(2.0 * Math.atan(dataSet.getMaxCurve() - 2.5) / Math.PI, 2.0));
            }

            long loops = dataSet.getLoops().size();
            Set<PixelNodeGraph.OpenSegment> segments = dataSet.getSegments();
            int segmentsCount = segments.size();
            double segmentsFactor = ScoreNormalizer.normalizePositive(segmentsCount, 1.0, 1.7, 0.4, 0.3, 0, 1.0, 10.0);

            PixelNodeGraph.OpenSegment bottomSegment = segments.stream().max((s1, s2) -> Double.compare(
                    Math.max(s1.getEndNode1().getLocationY(), s1.getEndNode2().getLocationY()),
                    Math.max(s2.getEndNode1().getLocationY(), s2.getEndNode2().getLocationY())
            )).orElse(null);
            double bottomAngle = Double.NaN;
            double bottomAngleFactor = 0.01;
            if (bottomSegment != null) {
                Boolean end1 = null;
                if (bottomSegment.getEndNode1().getConnectedNodesCount() == 1 && bottomSegment.getEndNode2().getConnectedNodesCount() == 1) {
                    end1 = bottomSegment.getEndNode1().getLocationY() > bottomSegment.getEndNode2().getLocationY();
                } else if (bottomSegment.getEndNode1().getConnectedNodesCount() == 1) {
                    end1 = true;
                } else if (bottomSegment.getEndNode2().getConnectedNodesCount() == 1) {
                    end1 = false;
                }
                if (end1 != null) {
                    bottomAngle = (GraphDataSet.getEndAngle(bottomSegment, 3, end1) % (Math.PI * 2.0) + Math.PI * 2.0) % (Math.PI * 2.0);
                    bottomAngleFactor = ScoreNormalizer.normalize(bottomAngle, 0.55 * Math.PI, Math.PI / 3.0, 0.9, 0.25, 0, 1.0);
                }
            }
            PixelNodeGraph.OpenSegment topSegment = segments.stream().min((s1, s2) -> Double.compare(
                    Math.min(s1.getEndNode1().getLocationY(), s1.getEndNode2().getLocationY()),
                    Math.min(s2.getEndNode1().getLocationY(), s2.getEndNode2().getLocationY())
            )).orElse(null);
            double topAngle = Double.NaN;
            double topAngleFactor = 0.01;
            if (topSegment != null) {
                Boolean end1 = null;
                if (topSegment.getEndNode1().getConnectedNodesCount() == 1 && topSegment.getEndNode2().getConnectedNodesCount() == 1) {
                    if (topSegment == bottomSegment) {
                        end1 = topSegment.getEndNode1().getLocationY() < topSegment.getEndNode2().getLocationY();
                    } else {
                        end1 = topSegment.getEndNode1().getLocationX() + topSegment.getEndNode1().getLocationY() < topSegment.getEndNode2().getLocationX() + topSegment.getEndNode2().getLocationY();
                    }
                } else if (topSegment.getEndNode1().getConnectedNodesCount() == 1) {
                    end1 = true;
                } else if (topSegment.getEndNode2().getConnectedNodesCount() == 1) {
                    end1 = false;
                }
                if (end1 != null) {
                    topAngle = (GraphDataSet.getEndAngle(topSegment, 3, end1) % (Math.PI * 2.0) + Math.PI * 2.0) % (Math.PI * 2.0);
                    topAngleFactor = ScoreNormalizer.normalize(topAngle, Math.PI, Math.PI / 4.0, 0.8, 0.25, 0, 1.0);
                }
            }
            return new CertaintyFactor[] {
                    // new CertaintyFactor("Segments", segmentsFactor, segmentsCount),
                    new CertaintyFactor("Max Curvature", maxCurveFactor, maxCurve),
                    new CertaintyFactor("Loops", Math.pow(loops + 1, -3.0), loops),
                    // new CertaintyFactor("Skew 90", skew90Normalized, maxSkew90),
                    new CertaintyFactor("Skew 120", skew120Normalized, maxSkew120),
                    // new CertaintyFactor("Skew 180", skew180Normalized, maxSkew180),
                    new CertaintyFactor("Turns", extremaCountFactor, extremaCount),
                    new CertaintyFactor("Top Angle", topAngleFactor, Math.toDegrees(topAngle)),
                    new CertaintyFactor("Bottom Angle", bottomAngleFactor, Math.toDegrees(bottomAngle)),
            };
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_EIGHT = new BasicNumberCharacterPattern(8) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoopsDistance().keySet().stream()
                    .filter(loop -> dataSet.getLoopsDistance().get(loop) >= dataSet.getTotalSegmentsDistance() * 0.1)
                    .collect(Collectors.toSet());
            Set<PixelNodeGraph.OpenSegment> largestLoop = loops.stream().max((loop1, loop2) -> Double.compare(dataSet.getLoopsDistance()
                    .get(loop1), dataSet.getLoopsDistance().get(loop2))).orElse(null);
            Set<PixelNodeGraph.OpenSegment> largest2Loop = loops.stream().filter(loop -> loop != largestLoop).max((loop1, loop2) -> Double.compare(dataSet.getLoopsDistance()
                    .get(loop1), dataSet.getLoopsDistance().get(loop2))).orElse(null);
            Double largest2LoopSize = dataSet.getLoopsDistance().get(largest2Loop);
            double loopsWeightedCount = largest2LoopSize == null ? 0.01 : loops.stream().filter(loop -> loop != largestLoop).mapToDouble(loop -> dataSet.getLoopsDistance()
                    .get(loop) / largest2LoopSize).sum();
            double loopsWeightedCountFactor = loopsWeightedCount == 0 || Double.isNaN(loopsWeightedCount) ? 0.01 : ScoreNormalizer.normalize(loopsWeightedCount, 1.0, 1.0, 0.3, 0.4, 0, 1.0);
            double largestLoopsRatio = largestLoop != null && largest2Loop != null ? dataSet.getLoopsDistance().get(largestLoop) / dataSet.getLoopsDistance()
                    .get(largest2Loop) : Double.NaN;
            double largestLoopsRatioFactor = Double.isNaN(largestLoopsRatio) ? 0.01 : ScoreNormalizer.normalize(largestLoopsRatio, 1.0, 1.8, 0.4, 0.35, 0, 1.0);

            double maxSkew0 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), 0);
            double maxSkew0Factor = ScoreNormalizer.normalize(maxSkew0, 0, 1.5, 0.6, 0.5, 0, 1.0);
            double maxSkew90 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI / 2.0);
            double maxSkew90Factor = ScoreNormalizer.normalize(maxSkew90, 0, 1.5, 0.6, 0.5, 0, 1.0);
            return new CertaintyFactor[] {
                    new CertaintyFactor("Loops-Weighted", loopsWeightedCountFactor, loopsWeightedCount),
                    new CertaintyFactor("Largest Loops Ratio", largestLoopsRatioFactor, largestLoopsRatio),
                    new CertaintyFactor("Skew 0", maxSkew0Factor, maxSkew0),
                    new CertaintyFactor("Skew 90", maxSkew90Factor, maxSkew90),
            };
        }
    };

    public static final BasicNumberCharacterPattern CHARACTER_NINE = new BasicNumberCharacterPattern(9) {
        @Override
        protected CertaintyFactor[] getCertaintyFactors(GraphDataSet dataSet) {
            Set<Set<PixelNodeGraph.OpenSegment>> loops = dataSet.getLoopsDistance().keySet().stream()
                    .filter(loop -> dataSet.getLoopsDistance().get(loop) >= dataSet.getTotalSegmentsDistance() * 0.1)
                    .collect(Collectors.toSet());
            double loopsFactor = ScoreNormalizer.normalizePositive(loops.size(), 1.0, 0.67, 0.8, 0.47, 0, 1.0, 10.0);
            double maxSkew90 = PixelNodeGraph.OpenSegment.getSkewness(dataSet.getGraph().getNodes(), Math.PI / 2.0);
            double maxSkewFactor = ScoreNormalizer.normalize(maxSkew90, 0.35, 0.35, 0.8, 0.22, 0, 1.0);

            PixelNodeGraph.OpenSegment longestNonLoop = dataSet.getSegments()
                    .stream().filter(s -> s.isEdge() && !loops.stream().anyMatch(l -> l.contains(s)))
                    .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                    .orElse(null);
            double curvatureEstimation = Double.NaN;
            double curvatureEstimationFactor = 0.01;
            double freeEndAngle = Double.NaN;
            double freeEndAngleFactor = 0.01;
            if (longestNonLoop != null) {
                curvatureEstimation = longestNonLoop.getCurvature3Mod();
                curvatureEstimationFactor = ScoreNormalizer.normalize(curvatureEstimation, 2.4, 2.0, 0.6, 0.3, 0, 1.0);

                freeEndAngle = GraphDataSet.getFreeEndAngle(longestNonLoop, 2);
                freeEndAngle = (freeEndAngle + (Math.PI * 2.0)) % (Math.PI * 2.0);
                freeEndAngleFactor = ScoreNormalizer.normalize(freeEndAngle, 11.0 / 12.0 * Math.PI, 7.0 / 12.0 * Math.PI, 0.8, 0.2, 0, 1.0);
            }
            PixelNodeGraph.OpenSegment longestRemainingSegment = dataSet.getSegments()
                    .stream().filter(s -> s.isEdge() && s != longestNonLoop && !loops.stream().anyMatch(l -> l.contains(s)))
                    .max((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                    .orElse(null);
            double remainingSegmentsRatio = Double.NaN;
            double remainingSegmentsFactor = 1;
            if (longestNonLoop != null && longestRemainingSegment != null) {
                remainingSegmentsRatio = longestRemainingSegment.getDistance() / longestNonLoop.getDistance();
                remainingSegmentsFactor = ScoreNormalizer.normalize(remainingSegmentsRatio, 0, 0.6, 0.5, 0.3, 0, 1.0);
            }
            double longestLoopDistance = loops.stream()
                    .mapToDouble(l -> l.stream().mapToDouble(PixelNodeGraph.OpenSegment::getDistance).sum())
                    .max().orElse(Double.NaN);
            double loopToSegmentRatio = Double.NaN;
            double loopToSegmentFactor = 0.01;
            if (!Double.isNaN(longestLoopDistance) && longestNonLoop != null) {
                loopToSegmentRatio = longestLoopDistance / longestNonLoop.getDistance();
                loopToSegmentFactor = ScoreNormalizer.normalize(loopToSegmentRatio, 2.75, 1.75, 0.85, 0.2, 0, 1.0);
            }
            return new CertaintyFactor[] {
                    new CertaintyFactor("Loops", loopsFactor, loops.size()),
                    new CertaintyFactor("Max Skew", maxSkewFactor, maxSkew90),
                    new CertaintyFactor("Curvature", curvatureEstimationFactor, curvatureEstimation),
                    new CertaintyFactor("Segment End Angle", freeEndAngleFactor, Math.toDegrees(freeEndAngle)),
                    new CertaintyFactor("Segments Ratio", remainingSegmentsFactor, remainingSegmentsRatio),
                    new CertaintyFactor("Loop-Segment Ratio", loopToSegmentFactor, loopToSegmentRatio),
            };
        }
    };

    public static final List<BasicNumberCharacterPattern> CHARACTERS_DIGITS_SET = Collections.unmodifiableList(
            Arrays.asList(CHARACTER_ZERO, CHARACTER_ONE, CHARACTER_TWO, CHARACTER_THREE, CHARACTER_FOUR,
                    CHARACTER_FIVE, CHARACTER_SIX, CHARACTER_SEVEN, CHARACTER_EIGHT, CHARACTER_NINE
            ));
}
