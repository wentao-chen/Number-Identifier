package main;

import maths.Statistics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ImageAnalysis {

    static int countLoops(BufferedImage image, Predicate<Integer> isFilled) {
        int count = 0;
        boolean[][] filledPixels = new boolean[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                filledPixels[y][x] = isFilled.test(image.getRGB(x, y));
            }
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!filledPixels[y][x]) {
                    Set<Point> fillArea = floodFillArea(filledPixels, x, y, false);
                    if (fillArea.size() > 0) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    static BufferedImage reduceStrokeWidth(BufferedImage image) {
        if (image.getWidth() == 0 || image.getHeight() == 0) {
            return image;
        }
        BufferedImage outImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, outImage.getWidth(), outImage.getHeight());

        class CellVector2D {
            int x;
            int y;
            double vX;
            double vY;

            public CellVector2D(int x, int y, double vX, double vY) {
                this.x = x;
                this.y = y;
                this.vX = vX;
                this.vY = vY;
            }

            public double getAngle() {
                return Math.atan2(this.vY, this.vX);
            }

            public double getMagnitude() {
                return Math.sqrt(this.vX * this.vX + this.vY * this.vY);
            }

            private double distanceToNextCell(CellVector2D[][] cells, double angle) {
                CellVector2D oppositeCell = getOppositeCell(cells, angle);
                if (oppositeCell == null) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    return Math.sqrt(Math.pow(this.x - oppositeCell.x, 2.0) + Math.pow(this.y - oppositeCell.y, 2.0));
                }
            }

            private CellVector2D getOppositeCell(CellVector2D[][] cells, double angle) {
                angle = (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
                // Check horizontal intersections
                int horizontalIntersectionIncrement = 0;
                double horizontalDistanceToNextCell = Double.POSITIVE_INFINITY;
                CellVector2D horizontalIntersectionCell = null;
                if (angle > 0 && angle < Math.PI) {
                    horizontalIntersectionIncrement = 1;
                } else if (angle > Math.PI && angle < Math.PI * 2) {
                    horizontalIntersectionIncrement = -1;
                }
                if (horizontalIntersectionIncrement != 0) {
                    double perpendicularIncrement = horizontalIntersectionIncrement / Math.tan(angle);
                    double currentX = this.x + perpendicularIncrement / 2.0;
                    int nextCellX = (int) Math.round(currentX);
                    int nextCellY = this.y + horizontalIntersectionIncrement;
                    while (nextCellY >= 0 && nextCellY < cells.length && nextCellX >= 0 && nextCellX < cells[nextCellY].length) {
                        if (Math.abs(currentX % 1 - 0.5) <= 0.01) {
                            nextCellX = (int) Math.floor(currentX);
                            if (nextCellX >= 0 && nextCellX < cells[nextCellY].length && cells[nextCellY][nextCellX] != null) {
                                horizontalDistanceToNextCell =
                                        Math.pow(nextCellX - this.x, 2.0) + Math.pow(nextCellY - this.y, 2.0);
                                horizontalIntersectionCell = cells[nextCellY][nextCellX];
                                if (horizontalDistanceToNextCell >= 1.5)
                                    break;
                            }
                            nextCellX = (int) Math.ceil(currentX);
                            if (nextCellX >= 0 && nextCellX < cells[nextCellY].length && cells[nextCellY][nextCellX] != null) {
                                horizontalDistanceToNextCell =
                                        Math.pow(nextCellX - this.x, 2.0) + Math.pow(nextCellY - this.y, 2.0);
                                horizontalIntersectionCell = cells[nextCellY][nextCellX];
                                if (horizontalDistanceToNextCell >= 1.5)
                                    break;
                            }
                        } else if (cells[nextCellY][nextCellX] != null) {
                            horizontalDistanceToNextCell =
                                    Math.pow(nextCellX - this.x, 2.0) + Math.pow(nextCellY - this.y, 2.0);
                            horizontalIntersectionCell = cells[nextCellY][nextCellX];
                            if (horizontalDistanceToNextCell >= 1.5)
                                break;
                        }
                        currentX += perpendicularIncrement;
                        nextCellX = (int) Math.round(currentX);
                        nextCellY += horizontalIntersectionIncrement;
                    }
                }
                // Check Vertical intersections
                int verticalIntersectionIncrement = 0;
                double verticalDistanceToNextCell = Double.POSITIVE_INFINITY;
                CellVector2D verticalIntersectionCell = null;
                if (angle < Math.PI * 0.5 || angle > Math.PI * 1.5) {
                    verticalIntersectionIncrement = 1;
                } else if (angle > Math.PI * 0.5 && angle < Math.PI * 1.5) {
                    verticalIntersectionIncrement = -1;
                }
                if (verticalIntersectionIncrement != 0) {
                    double perpendicularIncrement = verticalIntersectionIncrement * Math.tan(angle);
                    int nextCellX = this.x + verticalIntersectionIncrement;
                    double currentY = this.y + perpendicularIncrement / 2.0;
                    int nextCellY = (int) Math.round(currentY);
                    while (nextCellY >= 0 && nextCellY < cells.length && nextCellX >= 0 && nextCellX < cells[nextCellY].length) {
                        if (Math.abs(currentY % 1 - 0.5) <= 0.01) {
                            nextCellY = (int) Math.floor(currentY);
                            if (nextCellY >= 0 && nextCellY < cells.length && cells[nextCellY][nextCellX] != null) {
                                verticalDistanceToNextCell =
                                        Math.pow(nextCellX - this.x, 2.0) + Math.pow(nextCellY - this.y, 2.0);
                                verticalIntersectionCell = cells[nextCellY][nextCellX];
                                if (verticalDistanceToNextCell >= 1.5)
                                    break;
                            }
                            nextCellY = (int) Math.ceil(currentY);
                            if (nextCellY >= 0 && nextCellY < cells.length && cells[nextCellY][nextCellX] != null) {
                                verticalDistanceToNextCell =
                                        Math.pow(nextCellX - this.x, 2.0) + Math.pow(nextCellY - this.y, 2.0);
                                verticalIntersectionCell = cells[nextCellY][nextCellX];
                                if (verticalDistanceToNextCell >= 1.5)
                                    break;
                            }
                        } else if (cells[nextCellY][nextCellX] != null) {
                            verticalDistanceToNextCell =
                                    Math.pow(nextCellX - this.x, 2.0) + Math.pow(nextCellY - this.y, 2.0);
                            verticalIntersectionCell = cells[nextCellY][nextCellX];
                            if (verticalDistanceToNextCell >= 1.5)
                                break;
                        }
                        nextCellX += verticalIntersectionIncrement;
                        currentY += perpendicularIncrement;
                        nextCellY = (int) Math.round(currentY);
                    }
                }
                if (Double.isFinite(horizontalDistanceToNextCell) || Double.isFinite(verticalDistanceToNextCell)) {
                    return horizontalDistanceToNextCell < verticalDistanceToNextCell || Double.isFinite(horizontalDistanceToNextCell) ? horizontalIntersectionCell
                            : verticalIntersectionCell;
                } else {
                    return null;
                }
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                CellVector2D that = (CellVector2D) o;
                if (x != that.x) {
                    return false;
                }
                return y == that.y;
            }

            @Override
            public int hashCode() {
                int result = x;
                result = 31 * result + y;
                return result;
            }
        }

        CellVector2D[][] cellData = new CellVector2D[image.getHeight()][];
        for (int y = 0; y < image.getHeight(); y++) {
            cellData[y] = new CellVector2D[image.getWidth()];
            for (int x = 0; x < image.getWidth(); x++) {
                double[] sobel = sobelOperator(image, x, y);
                cellData[y][x] = new CellVector2D(x, y, sobel[0], sobel[1]);
            }
        }
        CellVector2D[][] vecLoopCells = new CellVector2D[cellData.length][cellData[0].length];
        Map<CellVector2D, Double> distances = new HashMap<>();

        // Search for edges (an edge is currently defined as an empty node with at least 2 of the 8 neighboring nodes being empty)
        Predicate<Integer> isFilledPixel = i -> (i & 0x00FFFFFF) != 0xFFFFFF;
        for (int y = 0; y < vecLoopCells.length; y++) {
            for (int x = 0; x < vecLoopCells[y].length; x++) {
                vecLoopCells[y][x] = null;
                if (!isFilledPixel.test(image.getRGB(x, y))) {
                    int emptyNeighborCount = 0;
                    for (int y2 = y - 1; y2 <= y + 1 && emptyNeighborCount < 2; y2++) {
                        if (y2 >= 0 && y2 < cellData.length) {
                            for (int x2 = x - 1; x2 <= x + 1; x2++) {
                                if (x2 >= 0 && x2 < cellData[y2].length && isFilledPixel.test(image.getRGB(x2, y2))) {
                                    emptyNeighborCount++;
                                    if (emptyNeighborCount >= 2) {
                                        vecLoopCells[y][x] = cellData[y][x];
                                        // outImage.setRGB(x, y, 0xFF000000);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (CellVector2D[] row : vecLoopCells) {
            for (CellVector2D v : row) {
                if (v != null) {
                    double d1 = v.distanceToNextCell(vecLoopCells, v.getAngle());
                    double d2 = v.distanceToNextCell(vecLoopCells, v.getAngle() + Math.PI);
                    distances.put(v, d1 < d2 ? d1 : -d2);
                }
            }
        }
        double[] outliersRange = Statistics.tukeyRangeTest(distances.values().stream().mapToDouble(Math::abs).boxed().collect(Collectors.toList()));
        Map<CellVector2D, Double> filteredDistances = distances.entrySet().stream()
                .filter(e -> Math.abs(e.getValue()) >= outliersRange[0] && Math.abs(e.getValue()) <= outliersRange[1])
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Collection<Double> filteredDistancesAbs = filteredDistances.values().stream().mapToDouble(Math::abs).boxed().collect(Collectors.toList());
        double meanDistance = Statistics.getMean(filteredDistancesAbs);
        double sdDistance = Statistics.getStandardDeviation(filteredDistancesAbs);
        if (meanDistance < 2.5) {
            return image;
        }
        Set<CellVector2D> done = new HashSet<>();
        int groupIDAssigner = 1;
        int[][] groupID = new int[cellData.length][cellData[0].length];
        for (CellVector2D v : filteredDistances.keySet()) {
            double angle = v.getAngle();
            double d = distances.get(v) / 2.0;
            if (done.contains(v)) continue;
            int x = (int) Math.round(v.x + d * Math.cos(angle));
            int y = (int) Math.round(v.y + d * Math.sin(angle));
            groupID[y][x] = groupIDAssigner++;
            // outImage.setRGB(x, y, 0xFFFF0000);
            // outImage.setRGB(v.x, v.y, 0xFF00FF00);
            done.add(v);
            CellVector2D opposite = v.getOppositeCell(vecLoopCells, angle + (d > 0 ? 0: Math.PI));
            if (opposite != null && distances.containsKey(opposite) && Math.abs((opposite.getAngle() % Math.PI + Math.PI) % Math.PI - (angle % Math.PI + Math.PI) % Math.PI) <= Math.PI / 6.0) {
                // outImage.setRGB(opposite.x, opposite.y, 0xFF0000FF);
                done.add(opposite);
            }
        }
        Map<Integer, Set<Point>> groupIDsMap = new HashMap<>();
        for (int y = 0; y < groupID.length; y++) {
            for (int x = 0; x < groupID[y].length; x++) {
                floodFillArea(groupID, x, y);
                if (groupID[y][x] != 0) {
                    Set<Point> set = groupIDsMap.get(groupID[y][x]);
                    if (set != null) {
                        set.add(new Point(x, y));
                    } else {
                        set = new HashSet<>();
                        set.add(new Point(x, y));
                        groupIDsMap.put(groupID[y][x], set);
                    }
                }
            }
        }
        while (groupIDsMap.size() > 1) {
            Iterator<Integer> groupIDsMapIterator = groupIDsMap.keySet().iterator();
            Integer id1 = groupIDsMapIterator.hasNext() ? groupIDsMapIterator.next() : null;
            if (id1 != null) {
                Point p1 = null;
                Point p2 = null;
                double minimumDistance = Double.POSITIVE_INFINITY;
                Set<Point> id1Points = groupIDsMap.get(id1);
                if (id1Points != null) {
                    for (Point pID1 : id1Points) {
                        for (int id2 : groupIDsMap.keySet()) {
                            if (id2 != id1) {
                                for (Point pID2 : groupIDsMap.get(id2)) {
                                    double distance = Math.pow(pID1.x - pID2.x, 2.0) + Math.pow(pID1.y - pID2.y, 2.0);
                                    if (distance < minimumDistance) {
                                        minimumDistance = distance;
                                        p1 = pID1;
                                        p2 = pID2;
                                    }
                                }
                            }
                        }
                    }
                }
                if (p1 != null) {
                    int id2 = groupID[p2.y][p2.x];
                    double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
                    double incrementX = Math.cos(angle);
                    double incrementY = Math.sin(angle);
                    double currentX = p1.x + incrementX;
                    double currentY = p1.y + incrementY;
                    while (Math.sqrt(Math.pow(currentX - p2.x, 2.0) + Math.pow(currentY - p2.y, 2.0)) >= 1) {
                        int x = (int) Math.round(currentX);
                        int y = (int) Math.round(currentY);
                        groupID[y][x] = id1;
                        currentX += incrementX;
                        currentY += incrementY;
                    }
                    for (int y = 0; y < groupID.length; y++) {
                        for (int x = 0; x < groupID[y].length; x++) {
                            if (groupID[y][x] == id2) {
                                groupID[y][x] = id1;
                            }
                        }
                    }
                    Set<Point> id2Points = groupIDsMap.get(id2);
                    id1Points.addAll(id2Points);
                    groupIDsMap.remove(id2);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        for (int y = 0; y < groupID.length; y++) {
            for (int x = 0; x < groupID[y].length; x++) {
                if (groupID[y][x] != 0) {
                    outImage.setRGB(x, y, 0xFF000000);
                }
            }
        }
        return outImage;
    }

    private static void floodFillArea(int[][] groupID, int x, int y) {
        int fillID = groupID[y][x];
        if (fillID != 0) {
            java.util.List<Point> toFill = new ArrayList<>();
            toFill.add(new Point(x, y));
            while (toFill.size() > 0) {
                Point current = toFill.get(0);
                toFill.remove(current);
                if (current.y >= 0 && current.y < groupID.length && current.x >= 0 && current.x < groupID[current.y].length && groupID[current.y][current.x] != 0) {
                    groupID[current.y][current.x] = fillID;
                    if (current.x >= 1 && groupID[current.y][current.x - 1] != 0 && groupID[current.y][current.x - 1] != fillID) {
                        toFill.add(new Point(current.x - 1, current.y));
                    }
                    if (current.x < groupID[current.y].length - 1 && groupID[current.y][current.x + 1] != 0 && groupID[current.y][current.x + 1] != fillID) {
                        toFill.add(new Point(current.x + 1, current.y));
                    }
                    if (current.y >= 1) {
                        if (groupID[current.y - 1][current.x] != 0 && groupID[current.y - 1][current.x] != fillID) {
                            toFill.add(new Point(current.x, current.y - 1));
                        }
                        if (current.x >= 1 && groupID[current.y - 1][current.x - 1] != 0 && groupID[current.y - 1][current.x - 1] != fillID) {
                            toFill.add(new Point(current.x - 1, current.y - 1));
                        }
                        if (current.x < groupID[current.y - 1].length - 1 && groupID[current.y - 1][current.x + 1] != 0 && groupID[current.y - 1][current.x + 1] != fillID) {
                            toFill.add(new Point(current.x + 1, current.y - 1));
                        }
                    }
                    if (current.y < groupID.length - 1) {
                        if (groupID[current.y + 1][current.x] != 0 && groupID[current.y + 1][current.x] != fillID) {
                            toFill.add(new Point(current.x, current.y + 1));
                        }
                        if (current.x >= 1 && groupID[current.y + 1][current.x - 1] != 0 && groupID[current.y + 1][current.x - 1] != fillID) {
                            toFill.add(new Point(current.x - 1, current.y + 1));
                        }
                        if (current.x < groupID[current.y + 1].length - 1 && groupID[current.y + 1][current.x + 1] != 0 && groupID[current.y + 1][current.x + 1] != fillID) {
                            toFill.add(new Point(current.x + 1, current.y + 1));
                        }
                    }
                }
            }
        }
    }

    private static Set<Point> floodFillArea(boolean[][] isFilled, int x, int y, boolean includeOutside) {
        Set<Point> filled = new HashSet<>();
        if (!isFilled[y][x]) {
            boolean isOutside = false;
            java.util.List<Point> toFill = new ArrayList<>();
            toFill.add(new Point(x, y));
            while (toFill.size() > 0) {
                Point current = toFill.get(0);
                toFill.remove(current);
                if (current.y >= 0 && current.y < isFilled.length && current.x >= 0 && current.x < isFilled[current.y].length && !isFilled[current.y][current.x]) {
                    if (current.y == 0 || current.y == isFilled.length - 1 || current.x == 0 || current.x == isFilled[current.y - 1].length - 1) {
                        isOutside = true;
                    }
                    isFilled[current.y][current.x] = true;
                    filled.add(current);
                    toFill.add(new Point(current.x - 1, current.y));
                    toFill.add(new Point(current.x + 1, current.y));
                    toFill.add(new Point(current.x, current.y - 1));
                    toFill.add(new Point(current.x, current.y + 1));
                }
            }
            if (isOutside && !includeOutside) {
                return new HashSet<>();
            }
        }
        return filled;
    }

    static BufferedImage fillLoops(BufferedImage image, Color fillColor, Predicate<Integer> isFilled, Predicate<Set<Point>> shouldFill) {
        boolean[][] filledPixels = new boolean[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                filledPixels[y][x] = isFilled.test(image.getRGB(x, y));
            }
        }
        java.util.List<Set<Point>> areas = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!filledPixels[y][x]) {
                    Set<Point> fillArea = floodFillArea(filledPixels, x, y, false);
                    if (fillArea.size() > 0) {
                        areas.add(fillArea);
                    }
                }
            }
        }
        BufferedImage outImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        areas.stream()
                .filter(shouldFill)
                .forEach(a -> a
                        .forEach(p -> outImage.setRGB(p.x, p.y, fillColor.getRGB())));
        return outImage;
    }

    static BufferedImage filter(BufferedImage image, ImageFilter filter, int filterSize) {
        BufferedImage outImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, outImage.getWidth(), outImage.getHeight());

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                outImage.setRGB(x, y, filter.filter(image, x, y, filterSize).getRGB());
            }
        }
        return outImage;
    }

    static BufferedImage filter(BufferedImage image, double[][] filter) {
        return filter(image, (image1, x, y, size) -> {
            double sumR = 0;
            double sumG = 0;
            double sumB = 0;
            for (int y1 = 0; y1 < filter.length; y1++) {
                for (int x1 = 0; x1 < filter[y1].length; x1++) {
                    int x2 = x + x1 - ((filter[y1].length - 1) / 2);
                    int y2 = y + y1 - ((filter.length - 1) / 2);
                    if (x2 >= 0 && y2 >= 0 && x2 < image1.getWidth() && y2 < image1.getHeight()) {
                        Color c = new Color(image1.getRGB(x2, y2), false);
                        sumR += c.getRed() * filter[y1][x1];
                        sumG += c.getGreen() * filter[y1][x1];
                        sumB += c.getBlue() * filter[y1][x1];
                    }
                }
            }
            sumR = Math.max(Math.min(sumR, 255), 0);
            sumG = Math.max(Math.min(sumG, 255), 0);
            sumB = Math.max(Math.min(sumB, 255), 0);
            return new Color((int) sumR, (int) sumG, (int) sumB);
        }, 1);
    }

    static BufferedImage blackOrWhite(BufferedImage image, double intensityThreshold) {
        BufferedImage outImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (getIntensity(new Color(image.getRGB(x, y))) >= intensityThreshold) {
                    outImage.setRGB(x, y, 0xFFFFFFFF);
                } else {
                    outImage.setRGB(x, y, 0xFF000000);
                }
            }
        }
        return outImage;
    }

    static BufferedImage add(BufferedImage image1, BufferedImage image2) {
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) throw new IllegalArgumentException("images are different sizes");
        BufferedImage outImage = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, outImage.getWidth(), outImage.getHeight());

        for (int y = 0; y < image1.getHeight(); y++) {
            for (int x = 0; x < image1.getWidth(); x++) {
                Color c1 = new Color(image1.getRGB(x, y));
                Color c2 = new Color(image2.getRGB(x, y));
                outImage.setRGB(x, y, new Color(Math.min(c1.getRed() + c2.getRed(), 255), Math.min(c1.getGreen() + c2.getGreen(), 255), Math.min(c1.getBlue() + c2.getBlue(), 255)).getRGB());
            }
        }
        return outImage;
    }

    @FunctionalInterface
    interface ImageFilter {
        Color filter(BufferedImage image, int x, int y, int size);
    }

    static double getIntensity(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 256.0;
    }

    public static ImageFilter FLIP_FILTER = (image, x, y, size) -> {
        Color c = new Color(image.getRGB(x, y), false);
        return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
    };

    public static ImageFilter GRAYSCALE_FILTER = (image, x, y, size) -> {
        double intensity = getIntensity(new Color(image.getRGB(x, y), false)) * 256;
        return new Color((int) intensity, (int) intensity, (int) intensity);
    };

    public static ImageFilter BLUR_FILTER = (image, x, y, size) -> {
        double sum = 0;
        int count = 0;
        for (int y2 = y - ((size - 1) / 2); y2 <= y + size / 2; y2++) {
            for (int x2 = x - ((size - 1) / 2); x2 <= x + size / 2; x2++) {
                if (x2 >= 0 && y2 >= 0 && x2 < image.getWidth() && y2 < image.getHeight()) {
                    sum += getIntensity(new Color(image.getRGB(x2, y2), false)) * 256;
                    count++;
                }
            }
        }
        double intensity = count > 0 ? sum / count : 0;
        return new Color((int) intensity, (int) intensity, (int) intensity);
    };

    public static ImageFilter BLUR_COLOR_FILTER = (image, x, y, size) -> {
        double sumR = 0;
        double sumG = 0;
        double sumB = 0;
        int count = 0;
        for (int y2 = y - ((size - 1) / 2); y2 <= y + size / 2; y2++) {
            for (int x2 = x - ((size - 1) / 2); x2 <= x + size / 2; x2++) {
                if (x2 >= 0 && y2 >= 0 && x2 < image.getWidth() && y2 < image.getHeight()) {
                    sumR += new Color(image.getRGB(x2, y2), false).getRed();
                    sumG += new Color(image.getRGB(x2, y2), false).getGreen();
                    sumB += new Color(image.getRGB(x2, y2), false).getBlue();
                    count++;
                }
            }
        }
        double intensityR = count > 0 ? sumR / count : 0;
        double intensityG = count > 0 ? sumG / count : 0;
        double intensityB = count > 0 ? sumB / count : 0;
        return new Color((int) intensityR, (int) intensityG, (int) intensityB);
    };

    public static ImageFilter WEIGHTED_BLUR_FILTER = (image, x, y, size) -> {
        double sum = 0;
        double count = 0;
        for (int y2 = y - ((size - 1) / 2); y2 <= y + size / 2; y2++) {
            for (int x2 = x - ((size - 1) / 2); x2 <= x + size / 2; x2++) {
                if (x2 >= 0 && y2 >= 0 && x2 < image.getWidth() && y2 < image.getHeight()) {
                    double weight = 1 + Math.sqrt(Math.pow(x2 - x, 2) + Math.pow(y2 - y, 2));
                    sum += getIntensity(new Color(image.getRGB(x2, y2), false)) * 256 / weight;
                    count += 1 / weight;
                }
            }
        }
        double intensity = count > 0 ? sum / count : 0;
        return new Color((int) intensity, (int) intensity, (int) intensity);
    };

    private static double[] sobelOperator(BufferedImage image, int x, int y) {
        double sumX = 0;
        double sumY = 0;
        double[][] filterX = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };
        double[][] filterY = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };
        for (int y1 = -1; y1 <= 1; y1++) {
            for (int x1 = -1; x1 <= 1; x1++) {
                int x2 = x + x1;
                int y2 = y + y1;
                if (x2 >= 0 && y2 >= 0 && x2 < image.getWidth() && y2 < image.getHeight()) {
                    sumX += getIntensity(new Color(image.getRGB(x2, y2), false)) * filterX[y1 + 1][x1 + 1];
                    sumY += getIntensity(new Color(image.getRGB(x2, y2), false)) * filterY[y1 + 1][x1 + 1];
                }
            }
        }
        return new double[] {sumX, sumY};
    }

    public static ImageFilter SOBEL_FILTER = (image, x, y, size) -> {
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumYX = 0;
        double[][] filterX = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };
        double[][] filterY = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };
        double[][] filterXY = {
                {-2, -1, 0},
                {-1, 0, 1},
                {0, 1, 2}
        };
        double[][] filterYX = {
                {0, 1, 2},
                {-1, 0, 1},
                {-2, -1, 0}
        };
        for (int y1 = -1; y1 <= 1; y1++) {
            for (int x1 = -1; x1 <= 1; x1++) {
                int x2 = x + x1;
                int y2 = y + y1;
                if (x2 >= 0 && y2 >= 0 && x2 < image.getWidth() && y2 < image.getHeight()) {
                    sumX += getIntensity(new Color(image.getRGB(x2, y2), false)) * filterX[y1 + 1][x1 + 1];
                    sumY += getIntensity(new Color(image.getRGB(x2, y2), false)) * filterY[y1 + 1][x1 + 1];
                    sumXY += getIntensity(new Color(image.getRGB(x2, y2), false)) * filterXY[y1 + 1][x1 + 1];
                    sumYX += getIntensity(new Color(image.getRGB(x2, y2), false)) * filterYX[y1 + 1][x1 + 1];
                }
            }
        }
        double intensity1 = Math.sqrt(sumX * sumX + sumY * sumY) / Math.sqrt(32) * 256;
        double intensity2 = Math.sqrt(sumXY * sumXY + sumYX * sumYX) / Math.sqrt(32) * 256;
        double intensity = (intensity1 + intensity2) / 2.0;
        return new Color((int) intensity, (int) intensity, (int) intensity);
    };

    public static ImageFilter GAUSSIAN_FILTER = (image, x, y, size) -> {
        double sum = 0;
        double[][] filter = {
                {2, 4, 5, 4, 2},
                {4, 9, 12, 9, 4},
                {5, 12, 15, 12, 5},
                {4, 9, 12, 9, 4},
                {2, 4, 5, 4, 2}
        };
        for (int y1 = -2; y1 <= 2; y1++) {
            for (int x1 = -2; x1 <= 2; x1++) {
                int x2 = x + x1;
                int y2 = y + y1;
                if (x2 >= 0 && y2 >= 0 && x2 < image.getWidth() && y2 < image.getHeight()) {
                    sum += getIntensity(new Color(image.getRGB(x2, y2), false)) * filter[y1 + 2][x1 + 2];
                }
            }
        }
        double filterSum = 0;
        for (double[] row : filter) {
            for (double col : row) {
                filterSum += col;
            }
        }
        sum /= 159.0;
        sum *= 256.0;
        return new Color((int) sum, (int) sum, (int) sum);
    };

    static final double[][][] SHARPENING_FILTERS = {
        {
            {-1, -1, -1},
            {-1, 9, -1},
            {-1, -1, -1}
        },{
            {-1, -1, -1, -1, -1},
            {-1, -3, -3, -3, -1},
            {-1, -3, 41, -3, -1},
            {-1, -3, -3, -3, -1},
            {-1, -1, -1, -1, -1}
        },{
            {-1, -1, -1, -1, -1, -1, -1},
            {-1, -3, -3, -3, -3, -3, -1},
            {-1, -3, -5, -5, -5, -3, -1},
            {-1, -3, -5, 113, -5, -3, -1},
            {-1, -3, -5, -5, -5, -3, -1},
            {-1, -3, -3, -3, -3, -3, -1},
            {-1, -1, -1, -1, -1, -1, -1},
        }
    };
}
