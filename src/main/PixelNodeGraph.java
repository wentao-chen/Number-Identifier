package main;


import maths.DoubleFunction;
import maths.Polynomial;
import maths.PolynomialRegression;
import maths.RationalPolynomial;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class PixelNodeGraph {
    private final List<PixelNode> nodes = new ArrayList<>();

    public static BufferedImage toImage(Collection<OpenSegment> segments, int width, int height) {
        BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, outImage.getWidth(), outImage.getHeight());

        for (OpenSegment s : segments) {
            Color segmentColor = new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256));
            for (PixelNode n : s.getNodes()) {
                n.drawToImage(outImage, n.connectedNodes.size() != 2 ? Color.BLACK : segmentColor);
            }
        }
        return outImage;
    }

    public BufferedImage toImage(int width, int height) {
        BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = outImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, outImage.getWidth(), outImage.getHeight());
        for (PixelNode n : this.nodes) {
            n.drawToImage(outImage);
        }
        return outImage;
    }

    public static PixelNodeGraph createGraphFromImage(BufferedImage image, Predicate<Integer> isFilled) {
        PixelNode[][] nodes = new PixelNode[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isFilled.test(image.getRGB(x, y))) {
                    nodes[y][x] = new PixelNode(x, y);
                    if (x > 0 && nodes[y][x - 1] != null) {
                        PixelNode.connect(nodes[y][x - 1], nodes[y][x]);
                    }
                    if (y > 0 && nodes[y - 1][x] != null) {
                        PixelNode.connect(nodes[y - 1][x], nodes[y][x]);
                    }
                }
            }
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (nodes[y][x] != null) {
                    if (x > 0 && y > 0 && nodes[y - 1][x] == null && nodes[y][x - 1] == null && nodes[y - 1][x - 1] != null) {
                        PixelNode.connect(nodes[y - 1][x - 1], nodes[y][x]);
                    }
                    if (x > 0 && y < image.getHeight() - 1 && nodes[y + 1][x] == null && nodes[y][x - 1] == null && nodes[y + 1][x - 1] != null) {
                        PixelNode.connect(nodes[y + 1][x - 1], nodes[y][x]);
                    }
                    if (x < image.getWidth() - 1 && y > 0 && nodes[y - 1][x] == null && nodes[y][x + 1] == null && nodes[y - 1][x + 1] != null) {
                        PixelNode.connect(nodes[y - 1][x + 1], nodes[y][x]);
                    }
                    if (x < image.getWidth() - 1 && y < image.getHeight() - 1 && nodes[y + 1][x] == null && nodes[y][x + 1] == null && nodes[y + 1][x + 1] != null) {
                        PixelNode.connect(nodes[y + 1][x + 1], nodes[y][x]);
                    }
                }
            }
        }

        PixelNodeGraph graph = new PixelNodeGraph();
        for (PixelNode[] row : nodes) {
            for (PixelNode n : row) {
                if (n != null) {
                    graph.nodes.add(n);
                }
            }
        }
        return graph;
    }

    private static class PixelNodesRect {
        private double width;
        private double height;
        private Set<PixelNode> nodes;
        private PixelNodesRect(double width, double height, Set<PixelNode> nodes) {
            this.width = width;
            this.height = height;
            this.nodes = nodes;
        }
    }

    private static class IntegerLocation {
        private int locationX;
        private int locationY;
        private IntegerLocation(int x, int y) {
            this.locationX = x;
            this.locationY = y;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IntegerLocation that = (IntegerLocation) o;
            if (locationX != that.locationX) {
                return false;
            }
            return locationY == that.locationY;
        }
        @Override
        public int hashCode() {
            int result = locationX;
            result = 31 * result + locationY;
            return result;
        }
    }

    private PixelNodesRect getRect(PixelNode node, double gridWidth, double gridHeight, double offsetX, double offsetY) {
        double left = node.getLocationX() + offsetX;
        double top = node.getLocationY() + offsetY;
        Set<IntegerLocation> nodeGridLocations = new HashSet<>();
        for (PixelNode n : this.nodes) {
            double x = n.getLocationX();
            double y = n.getLocationY();
            if (x >= left && y >= top) {
                IntegerLocation location = new IntegerLocation((int) Math.floor((x - left) / gridWidth), (int) Math.floor((y - top) / gridHeight));
                nodeGridLocations.add(location);
            }
        }
        int largestRectWidth = 0;
        int largestRectHeight = 0;
        int maxRectWidth = 0;
        int rectWidth = Integer.MAX_VALUE;
        int rectHeight = 0;
        for (int y = 0; rectWidth > 0; y++) {
            rectHeight = y;
            if (largestRectWidth * largestRectHeight < rectWidth * rectHeight) {
                largestRectWidth = rectWidth;
                largestRectHeight = rectHeight;
            }
            for (int x = 0; x < rectWidth; x++) {
                if (!nodeGridLocations.contains(new IntegerLocation(x, y))) {
                    if (y == 0) {
                        maxRectWidth = x;
                    }
                    rectWidth = x;
                    if (largestRectWidth * largestRectHeight < rectWidth * rectHeight) {
                        largestRectWidth = rectWidth;
                        largestRectHeight = rectHeight;
                    }
                    break;
                }
            }
        }
        if (largestRectWidth == maxRectWidth - 1) {
            largestRectWidth = maxRectWidth - 2;
        }
        if (largestRectHeight == rectHeight - 1) {
            largestRectHeight = rectHeight - 2;
        }
        Set<PixelNode> nodes = new HashSet<>();
        for (PixelNode n : this.nodes) {
            int x = (int) Math.floor((n.getLocationX() - left) / gridWidth);
            int y = (int) Math.floor((n.getLocationY() - top) / gridHeight);
            if (x >= 0 && y >= 0 && x < largestRectWidth && y < largestRectHeight) {
                nodes.add(n);
            }
        }
        return new PixelNodesRect(largestRectWidth, largestRectHeight, nodes);
    }

    public Rectangle2D getBoundingRectangle() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = 0;
        double maxY = 0;
        for (PixelNode n : this.nodes) {
            minX = Math.min(minX, n.getLocationX());
            minY = Math.min(minY, n.getLocationY());
            maxX = Math.max(maxX, n.getLocationX());
            maxY = Math.max(maxY, n.getLocationY());
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    public void replaceRectangles() {
        for (int i = 0; i < this.nodes.size(); i++) {
            PixelNode n = this.nodes.get(i);
            PixelNodesRect rect = getRect(n, 1, 1, -0.5, -0.5);
            if (rect.width > 1 && rect.height > 1) {
                double centerX = 0;
                double centerY = 0;
                Set<PixelNode> connections = new HashSet<>();
                for (PixelNode n2 : rect.nodes) {
                    centerX += n2.getLocationX();
                    centerY += n2.getLocationY();
                    connections.addAll(n2.connectedNodes);
                    remove(n2);
                }
                i = Math.max(i - rect.nodes.size() + 1, 0);
                connections.removeAll(rect.nodes);
                PixelNode newNode = new PixelNode(centerX / rect.nodes.size(), centerY / rect.nodes.size());
                for (PixelNode c : connections) {
                    PixelNode.connect(newNode, c);
                }
                this.nodes.add(0, newNode);
            }
        }
    }

    public void replaceNodes(Collection<PixelNode> nodes) {
        double centerX = 0;
        double centerY = 0;
        for (PixelNode n2 : nodes) {
            centerX += n2.getLocationX();
            centerY += n2.getLocationY();
        }
        replaceNodes(nodes, new PixelNode(centerX / nodes.size(), centerY / nodes.size()));
    }

    public void replaceNodes(Collection<PixelNode> nodes, PixelNode newNode) {
        Set<PixelNode> connections = new HashSet<>();
        for (PixelNode n : nodes) {
            connections.addAll(n.connectedNodes);
            remove(n);
        }
        connections.removeAll(nodes);
        connections.remove(newNode);
        for (PixelNode c : connections) {
            PixelNode.connect(newNode, c);
        }
        this.nodes.add(0, newNode);
    }

    public void replaceShortConnectorSegments() {
        Set<OpenSegment> allSegments = getOpenSegments();
        Set<OpenSegment> toRemoveSegments = allSegments.stream()
                .filter(s -> s.getNodeLength() < 5 && s.isConnector())
                .collect(Collectors.toSet());
        if (toRemoveSegments.size() > 0) {
            for (OpenSegment s : toRemoveSegments) {
                replaceNodes(s.getNodes());
            }
            replaceShortConnectorSegments();
        }
    }

    void connectNearbyEnds() {
        Set<OpenSegment> allSegments = getOpenSegments();
        Set<PixelNode> endNodes = getVertices(1);
        Rectangle2D boundingRect = getBoundingRectangle();
        endNodes.forEach(n1 ->
                endNodes.stream()
                        .filter(n2 -> !n1.equals(n2) && n1.distanceTo(n2) < Math.min(5, boundingRect.getWidth() * boundingRect.getHeight() * 0.001))
                        .forEach(n2 -> PixelNode.connect(n1, n2))
        );
    }

    private Set<OpenSegment> traceSegments(PixelNode endNode1) {
        Set<OpenSegment> segments = new HashSet<>();
        for (PixelNode path : endNode1.connectedNodes) {
            PixelNode previous = endNode1;
            PixelNode currentNode = path;
            int length = 1;
            double distance = previous.distanceTo(currentNode);
            boolean exitLoop = false;
            while (currentNode.connectedNodes.size() == 2) {
                for (PixelNode n : currentNode.connectedNodes) {
                    if (n != previous) {
                        if (n == endNode1) {
                            previous = currentNode;
                            currentNode = n;
                            exitLoop = true;
                            break;
                        }
                        previous = currentNode;
                        currentNode = n;
                        break;
                    }
                }
                if (exitLoop) {
                    break;
                }
                length++;
                distance += previous.distanceTo(currentNode);
            }
            segments.add(new OpenSegment(endNode1, currentNode, length, distance, path, previous));
        }
        return segments;
    }

    public Set<PixelNode> getVertices(int connections) {
        return getVertices(connections, connections);
    }

    public Set<PixelNode> getVertices(int connectionsMin, int connectionsMax) {
        return this.nodes.stream().filter(n -> n.connectedNodes.size() >= connectionsMin && n.connectedNodes.size() <= connectionsMax).collect(Collectors.toSet());
    }

    private static Set<OpenSegment> getConnectedSegments(PixelNode node, Collection<OpenSegment> allSegments) {
        return allSegments.stream()
                .filter(s -> node == s.getEndNode1() || node == s.getEndNode2())
                .collect(Collectors.toSet());
    }

    public Set<Set<OpenSegment>> getLoop(int maxSegmentsInLoop) {
        return getLoop(maxSegmentsInLoop, getOpenSegments());
    }

    public Set<Set<OpenSegment>> getLoop(int maxSegmentsInLoop, Set<OpenSegment> allSegments) {
        Set<Set<OpenSegment>> loops = new HashSet<>();
        Set<PixelNode> visitedNodes = new HashSet<>();
        for (OpenSegment s : allSegments) {
            PixelNode root = s.getEndNode1();
            if (!visitedNodes.contains(root)) {
                Map<PixelNode, OpenSegment> visited = new HashMap<>();
                findLoopsDepthFirstSearch(0, maxSegmentsInLoop, root, null, visited, allSegments, loops);
                visitedNodes.addAll(visited.keySet());
            }
        }
        if (loops.size() <= 1) {
            return loops;
        } else {
            Set<Set<OpenSegment>> innerLoops = new HashSet<>();
            loops.stream()
                    .filter(l -> l.size() < allSegments.size())
                    .forEach(l -> {
                        Set<Set<OpenSegment>> nestedLoops = getLoop(maxSegmentsInLoop, l);
                        assert nestedLoops.size() > 0;
                        if (nestedLoops.size() == 1) {
                            innerLoops.add(l);
                        } else {
                            innerLoops.addAll(nestedLoops);
                        }
                    });
            return innerLoops;
        }
    }

    private static void findLoopsDepthFirstSearch(int currentSegments, int maxSegmentsInLoop, PixelNode currentNode, OpenSegment previousPath, Map<PixelNode, OpenSegment> visitedNodes, Set<OpenSegment> allSegments, Set<Set<OpenSegment>> loops) {
        if (visitedNodes.containsKey(currentNode)) {
            // Find most recent common ancestor node and trace path to get loop
            List<PixelNode> pathNodes = new ArrayList<>();
            List<OpenSegment> path = new ArrayList<>();
            PixelNode traceNode = currentNode;
            while (traceNode != null) {
                pathNodes.add(traceNode);
                OpenSegment segment = visitedNodes.get(traceNode);
                if (segment == null) {
                    traceNode = null;
                } else {
                    path.add(segment);
                    traceNode = segment.getEndNode1() == traceNode ? segment.getEndNode2() : segment.getEndNode1();
                }
            }

            if (currentNode == previousPath.getEndNode1()) {
                currentNode = previousPath.getEndNode2();
            } else {
                currentNode = previousPath.getEndNode1();
            }
            List<OpenSegment> path2 = new ArrayList<>();
            while (!pathNodes.contains(currentNode)) {
                OpenSegment segment = visitedNodes.get(currentNode);
                if (segment == null) {
                    // No common ancestor node
                    return;
                } else {
                    path2.add(segment);
                    currentNode = segment.getEndNode1() == currentNode ? segment.getEndNode2() : segment.getEndNode1();
                }
            }
            Set<OpenSegment> loop = new HashSet<>();
            loop.addAll(path2);
            for (int i = 0; i < pathNodes.size() && pathNodes.get(i) != currentNode; i++) {
                loop.add(path.get(i));
            }
            loop.add(previousPath);
            loops.add(loop);
        } else if (currentSegments < maxSegmentsInLoop) {
            visitedNodes.put(currentNode, previousPath);
            Set<OpenSegment> connections = getConnectedSegments(currentNode, allSegments);
            for (OpenSegment s : connections) {
                if (s != previousPath) {
                    PixelNode connectedNode = s.getEndNode1();
                    if (connectedNode == currentNode) {
                        connectedNode = s.getEndNode2();
                    }
                    findLoopsDepthFirstSearch(currentSegments + 1, maxSegmentsInLoop, connectedNode, s, visitedNodes, allSegments, loops);
                }
            }
        }
    }

    public Set<OpenSegment> getOpenSegments() {
        Set<OpenSegment> segments = new HashSet<>();
        for (PixelNode n : this.nodes) {
            if (n.connectedNodes.size() == 0) {
                segments.add(new OpenSegment(n));
            } else if (n.connectedNodes.size() != 2) {
                segments.addAll(traceSegments(n));
            }
        }
        if (segments.size() == 0) {
            Set<PixelNode> usedNodes = new HashSet<>();
            this.nodes.forEach(n -> {
                if (!usedNodes.contains(n)) {
                    Set<OpenSegment> newSegments = traceSegments(n);
                    segments.addAll(newSegments);
                    for (OpenSegment s : newSegments) {
                        usedNodes.addAll(s.getNodes());
                    }
                }
            });
        }
        return segments;
    }

    public void remove(PixelNode node) {
        if (this.nodes.remove(node)) {
            for (PixelNode n : new ArrayList<>(node.connectedNodes)) {
                PixelNode.disconnect(node, n);
            }
        }
    }

    public void removeSegment(OpenSegment segment) {
        Set<PixelNode> nodes = segment.getEdgeNodes();
        if (nodes != null) {
            nodes.forEach(this::remove);
        }
    }

    public List<PixelNode> getNodes() {
        return this.nodes;
    }

    @Override
    public String toString() {
        return "PixelNodeGraph{" +
                "nodes=" + nodes +
                '}';
    }

    public static class PixelNode implements Comparable<PixelNode> {
        private final double locationX;
        private final double locationY;
        private final Set<PixelNode> connectedNodes = new HashSet<>();

        PixelNode(double x, double y) {
            this.locationX = x;
            this.locationY = y;
        }

        public double getLocationX() {
            return locationX;
        }

        public double getLocationY() {
            return locationY;
        }

        public int getConnectedNodesCount() {
            return connectedNodes.size();
        }

        /**
         * Gets the connected node that is opposite of a specified node based on the following conditions:<br>
         *     <ol>
         *         <li>If the number of connected nodes is 0 or greater than 2 then {@code null} is returned.</li>
         *         <li>If the number of connected nodes is 1 then the connected node is returned
         *         only if {@code neighbor1} is {@code null}, otherwise {@code null} is returned.</li>
         *         <li>If the number of connected nodes is 2 then the connected node that is not {@code equals} to {@code neighbor1} is returned
         *         only if {@code neighbor1} is one of the connected nodes, otherwise {@code null} is returned.</li>
         *     </ol>
         * @param neighbor1 one of the connected nodes
         * @return the connected node that is opposite of {@code neighbor1}
         */
        public PixelNode getOppositeConnection(PixelNode neighbor1) {
            if (neighbor1 == null && this.connectedNodes.size() == 1) {
                for (PixelNode n : this.connectedNodes) {
                    return n;
                }
            } else if (this.connectedNodes.size() == 2) {
                boolean found = false;
                PixelNode otherNode = null;
                for (PixelNode n : this.connectedNodes) {
                    if (n.equals(neighbor1)) {
                        found = true;
                    } else {
                        otherNode = n;
                    }
                }
                if (found) {
                    return otherNode;
                }
            }
            return null;
        }

        public void drawToImage(BufferedImage image) {
            drawToImage(image, null);
        }

        public void drawToImage(BufferedImage image, Color color) {
            double x = getLocationX();
            double y = getLocationY();
            List<Integer> drawXs = new ArrayList<>();
            Color color2 = Color.BLACK;
            if (x % 1 >= 0.25 && x % 1 < 0.75) {
                drawXs.add((int) Math.floor(x));
                drawXs.add((int) Math.ceil(x));
                color2 = Color.BLUE;
            } else {
                drawXs.add((int) Math.round(x));
            }
            List<Integer> drawYs = new ArrayList<>();
            if (y % 1 >= 0.25 && y % 1 < 0.75) {
                drawYs.add((int) Math.floor(y));
                drawYs.add((int) Math.ceil(y));
                color2 = color2 == Color.BLUE ? Color.GREEN : Color.RED;
            } else {
                drawYs.add((int) Math.round(y));
            }
            for (int y2 : drawYs) {
                for (int x2 : drawXs) {
                    if (x2 >= 0 && x2 < image.getWidth() && y2 >= 0 && y2 < image.getHeight()) {
                        image.setRGB(x2, y2, (color != null ? color : color2).getRGB());
                    }
                }
            }
        }

        public double distanceTo(double x, double y) {
            return Math.sqrt(Math.pow(this.locationX - x, 2) + Math.pow(this.locationY - y, 2));
        }

        public double distanceTo(PixelNode node) {
            return distanceTo(node.getLocationX(), node.getLocationY());
        }

        public static void connect(PixelNode node1, PixelNode node2) {
            if (node1 != null && node2 != null && node1 != node2) {
                node1.connectedNodes.add(node2);
                node2.connectedNodes.add(node1);
            }
        }

        public static void disconnect(PixelNode node1, PixelNode node2) {
            node1.connectedNodes.remove(node2);
            node2.connectedNodes.remove(node1);
        }

        private static Collection<PixelNode> copyNodes(Collection<PixelNode> nodes) {
            Map<PixelNode, PixelNode> newNodes = new LinkedHashMap<>();
            for (PixelNode n : nodes) {
                newNodes.put(n, new PixelNode(n.getLocationX(), n.getLocationY()));
            }
            for (PixelNode n : nodes) {
                n.connectedNodes.stream()
                        .filter(nodes::contains)
                        .forEach(c -> PixelNode.connect(newNodes.get(n), newNodes.get(c)));
            }
            return newNodes.values();
        }

        private static Collection<PixelNode> copyAndConnectAdjacentNodes(List<PixelNode> nodes) {
            Map<PixelNode, PixelNode> newNodes = new LinkedHashMap<>();
            for (PixelNode n : nodes) {
                newNodes.put(n, new PixelNode(n.getLocationX(), n.getLocationY()));
            }
            for (PixelNode n : nodes) {
                n.connectedNodes.stream()
                        .filter(nodes::contains)
                        .forEach(c -> PixelNode.connect(newNodes.get(n), newNodes.get(c)));
            }
            for (int i = 0; i < nodes.size() - 1; i++) {
                PixelNode.connect(newNodes.get(nodes.get(i)), newNodes.get(nodes.get(i + 1)));
            }
            return newNodes.values();
        }

        @Override
        public String toString() {
            return "PixelNode{" +
                    "x=" + locationX +
                    ", y=" + locationY +
                    ", connectedNodes=" + connectedNodes.size() +
                    '}';
        }

        @Override
        public int compareTo(PixelNode o) {
            int y = Double.compare(this.locationY, o.locationY);
            return y == 0 ? Double.compare(this.locationX, o.locationX) : y;
        }
    }

    /**
     * A group of interconnected nodes with exactly 2 nodes that have 1 or more than 2 node connections and all other nodes having exactly 2 node connections or
     * a single node with 0 connections.
     */
    public static class OpenSegment {
        private PixelNode endNode1;
        private PixelNode endNode2;
        private PixelNode endNode1Neighbor;
        private PixelNode endNode2Neighbor;
        private final int length;
        private final double distance;

        public OpenSegment(PixelNode endNode) {
            this(endNode, endNode, 0, 0, null, null);
        }

        public OpenSegment(PixelNode endNode1, PixelNode endNode2, int length, double distance, PixelNode endNode1Neighbor, PixelNode endNode2Neighbor) {
            if (endNode1.compareTo(endNode2) < 0) {
                this.endNode1 = endNode1;
                this.endNode2 = endNode2;
            } else {
                this.endNode1 = endNode2;
                this.endNode2 = endNode1;
                PixelNode temp = endNode1Neighbor;
                endNode1Neighbor = endNode2Neighbor;
                endNode2Neighbor = temp;
            }
            this.length = length;
            this.distance = distance;
            this.endNode1Neighbor = endNode1Neighbor;
            if (endNode1Neighbor == null && endNode1.connectedNodes.size() == 1) {
                for (PixelNode n : endNode1.connectedNodes) {
                    this.endNode1Neighbor = n;
                }
            }
            this.endNode2Neighbor = endNode2Neighbor;
            if (endNode2Neighbor == null && endNode2.connectedNodes.size() == 1) {
                for (PixelNode n : endNode2.connectedNodes) {
                    this.endNode2Neighbor = n;
                }
            }
            if (isLoop()) {
                if (endNode1Neighbor == null || (endNode2Neighbor != null && endNode1Neighbor.compareTo(endNode2Neighbor) < 0)) {
                    this.endNode1Neighbor = endNode2Neighbor;
                    this.endNode2Neighbor = endNode1Neighbor;
                }
            }
        }

        public PixelNode getEndNode1() {
            return endNode1;
        }

        public PixelNode getEndNode2() {
            return endNode2;
        }

        public int getNodeLength() {
            return length;
        }

        public double getDistance() {
            return distance;
        }

        public boolean isSingle() {
            return getEndNode1() == getEndNode2() && getNodeLength() == 0;
        }

        public boolean isLoop() {
            return getEndNode1() == getEndNode2() && getNodeLength() >= 3;
        }

        public boolean isIsolated() {
            return isSingle() || getEndNode1().connectedNodes.size() == 1 && getEndNode2().connectedNodes.size() == 1;
        }

        public boolean isEdge() {
            return isSingle() || getEndNode1().connectedNodes.size() == 1 || getEndNode2().connectedNodes.size() == 1;
        }

        public boolean isConnector() {
            return getEndNode1() != getEndNode2() && getEndNode1().connectedNodes.size() > 2 && getEndNode2().connectedNodes.size() > 2;
        }

        public LinkedHashSet<PixelNode> getInnerNodes() {
            return getInnerNodes(0);
        }

        public LinkedHashSet<PixelNode> getInnerNodes(int skip) {
            if (skip < 0) throw new IllegalArgumentException("skip(" + skip + ") cannot be less than 0");
            if (isSingle()) {
                return new LinkedHashSet<>();
            } else {
                LinkedHashSet<PixelNode> nodes = new LinkedHashSet<>();
                PixelNode previous = this.endNode1;
                PixelNode currentNode = this.endNode1Neighbor;
                PixelNode end = this.endNode2;
                if (currentNode == null) {
                    previous = this.endNode2;
                    currentNode = this.endNode2Neighbor;
                    end = this.endNode1;
                    if (currentNode == null) {
                        return null;
                    }
                }
                int count = skip;
                while (currentNode != end && currentNode.connectedNodes.size() == 2) {
                    for (PixelNode n : currentNode.connectedNodes) {
                        if (n != previous) {
                            if (count == skip) {
                                nodes.add(currentNode);
                                count = 0;
                            } else {
                                count++;
                            }
                            previous = currentNode;
                            currentNode = n;
                            break;
                        }
                    }
                }
                if (skip > 0) {
                    nodes.add(this.endNode1Neighbor);
                    nodes.add(this.endNode2Neighbor);
                }
                return nodes;
            }
        }

        public LinkedHashSet<PixelNode> getEdgeNodes() {
            return getEdgeNodes(null);
        }

        private boolean isStartAtNode(Set<PixelNode> nodes, PixelNode endNode) {
            Iterator<PixelNode> innerNodesIterator = nodes.iterator();
            return innerNodesIterator.hasNext() && innerNodesIterator.next() == endNode;
        }

        public LinkedHashSet<PixelNode> getEdgeNodes(Integer skip) {
            PixelNode endNode1 = getEndNode1();
            PixelNode endNode2 = getEndNode2();
            LinkedHashSet<PixelNode> innerNodes = skip == null ? getInnerNodes() : getInnerNodes(skip);
            if (isStartAtNode(innerNodes, this.endNode2Neighbor)) {
                PixelNode temp = endNode1;
                endNode1 = endNode2;
                endNode2 = temp;
            }
            LinkedHashSet<PixelNode> nodes = new LinkedHashSet<>();
            if (endNode1.connectedNodes.size() <= 1) {
                nodes.add(endNode1);
            }
            nodes.addAll(innerNodes);
            if (endNode2.connectedNodes.size() == 1) {
                nodes.add(endNode2);
            }
            return nodes;
        }

        public LinkedHashSet<PixelNode> getNodes() {
            return getNodes(null);
        }

        public LinkedHashSet<PixelNode> getNodes(Integer skip) {
            PixelNode endNode1 = getEndNode1();
            PixelNode endNode2 = getEndNode2();
            LinkedHashSet<PixelNode> innerNodes = skip == null ? getInnerNodes() : getInnerNodes(skip);
            if (isStartAtNode(innerNodes, this.endNode2Neighbor)) {
                PixelNode temp = endNode1;
                endNode1 = endNode2;
                endNode2 = temp;
            }
            LinkedHashSet<PixelNode> nodes = new LinkedHashSet<>();
            nodes.add(endNode1);
            nodes.addAll(innerNodes);
            nodes.add(endNode2);
            return nodes;
        }

        private static Iterator<PixelNode> traverse(final PixelNode endNode1, final PixelNode endNode1Neighbor, final PixelNode endNode2) {
            return new Iterator<PixelNode>() {
                PixelNode previousNode = null;
                PixelNode currentNode = endNode1;
                @Override
                public boolean hasNext() {
                    return this.currentNode != null;
                }
                @Override
                public PixelNode next() {
                    PixelNode oldPreviousNode = this.previousNode;
                    PixelNode oldCurrentNode = this.currentNode;
                    this.previousNode = oldCurrentNode;
                    if (oldPreviousNode == null) {
                        this.currentNode = endNode1Neighbor;
                        if (this.currentNode == null && oldCurrentNode.connectedNodes.size() == 1) {
                            for (PixelNode n : oldCurrentNode.connectedNodes) {
                                this.currentNode = n;
                                break;
                            }
                        }
                    } else {
                        this.currentNode = null;
                        if (oldCurrentNode != endNode2) {
                            Set<PixelNode> connections = oldCurrentNode.connectedNodes;
                            if (connections.size() == 2) {
                                for (PixelNode n : connections) {
                                    if (n != oldPreviousNode) {
                                        this.currentNode = n;
                                        break;
                                    }
                                }
                            }
                            if (this.currentNode == endNode1) {
                                this.currentNode = null;
                            }
                        }
                    }
                    return oldCurrentNode;
                }
            };
        }

        public Iterator<PixelNode> traverse() {
            return traverse(this.endNode1, this.endNode1Neighbor, this.endNode2);
        }

        private static int getLength(Iterable<PixelNode> nodes) {
            int length = -1;
            for (PixelNode n : nodes) {
                length++;
            }
            return length;
        }

        private static double getDistance(Iterable<PixelNode> nodes) {
            double distance = 0;
            PixelNode previous = null;
            for (PixelNode n : nodes) {
                if (n == null) {
                    break;
                } else if (previous != null) {
                    distance += previous.distanceTo(n);
                }
                previous = n;
            }
            return distance;
        }

        public PolynomialRegression[] getRegressionCurves(int degrees) {
            return getRegressionCurves(degrees, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        /**
         * Gets a polynomial regression of the positions of the nodes against the distance along the curve
         * @param degrees the degree of the polynomial regression
         * @param distanceMin the minimum distance interval of the domain
         * @param distanceMax the maximum distance interval of the domain
         * @return an array of size 2 with the x-coordinate regression at index 0 and y-coordinate regression at index 1
         */
        public PolynomialRegression[] getRegressionCurves(int degrees, double distanceMin, double distanceMax) {
            List<Double> distance = new ArrayList<>();
            List<Double> xCoordinates = new ArrayList<>();
            List<Double> yCoordinates = new ArrayList<>();
            double currentDistance = 0;
            PixelNodeGraph.PixelNode previousNode = null;
            for (PixelNodeGraph.PixelNode n : getNodes()) {
                if (previousNode != null) {
                    currentDistance += previousNode.distanceTo(n);
                }
                if (currentDistance > distanceMax) {
                    break;
                } else if (distanceMin <= currentDistance) {
                    distance.add(currentDistance);
                    xCoordinates.add(n.getLocationX());
                    yCoordinates.add(n.getLocationY());
                }
                previousNode = n;
            }
            return new PolynomialRegression[] {
                    new PolynomialRegression(distance, xCoordinates, degrees),
                    new PolynomialRegression(distance, yCoordinates, degrees)
            };
        }

        static RationalPolynomial getRegressionCurvesDerivative(Polynomial x, Polynomial y) {
            return new RationalPolynomial(y.differentiate(), x.differentiate());
        }

        static RationalPolynomial getRegressionCurves2ndDerivative(Polynomial x, Polynomial y) {
            return getRegressionCurvesDerivative(x, y).differentiate();
        }

        RationalPolynomial getRegressionCurves2ndDerivative(int degrees) {
            PolynomialRegression[] regressions = getRegressionCurves(degrees);
            return getRegressionCurves2ndDerivative(regressions[1].getPolynomial(), regressions[0].getPolynomial());
        }

        static DoubleFunction getCurvatureFunction(Polynomial x, Polynomial y) {
            return new DoubleFunction() {
                Polynomial xDerivative1 = x.differentiate();
                Polynomial xDerivative2 = xDerivative1.differentiate();
                Polynomial yDerivative1 = y.differentiate();
                Polynomial yDerivative2 = yDerivative1.differentiate();
                @Override
                public double evaluate(double x) {
                    double xD1 = xDerivative1.evaluate(x);
                    double xD2 = xDerivative2.evaluate(x);
                    double yD1 = yDerivative1.evaluate(x);
                    double yD2 = yDerivative2.evaluate(x);
                    return (xD1 * yD2 - xD2 * yD1) / Math.pow(xD1 * xD1 + yD1 * yD1, 1.5);
                }
            };
        }

        /**
         * Creates a copy of the segment that skips a specified interval of nodes. When skip equals 0, the resulting
         * segment is the same. When skip equals 1, every other node is skipped.<br><br>
         * The end nodes and neighboring nodes are always included and never skipped.<br><br>
         * The resulting segment contains newly created nodes that are interconnected but not connected to the nodes of this segment.
         * @param skip the non-negative number of nodes to skip
         * @return a copy of the segment that skips nodes
         */
        public OpenSegment createSkippedSegment(int skip) {
            LinkedHashSet<PixelNode> nodes = getNodes(skip);
            List<PixelNode> nodesAsList = new ArrayList<>();
            nodes.forEach(nodesAsList::add);
            Collection<PixelNode> newNodes = PixelNode.copyAndConnectAdjacentNodes(nodesAsList);
            PixelNode newEndNode1 = newNodes.stream().filter(n -> Math.abs(n.getLocationX() - this.endNode1.getLocationX()) <= 0 && Math.abs(n.getLocationY() - this.endNode1.getLocationY()) <= 0).findFirst().orElse(null);
            PixelNode newEndNode2 = newNodes.stream().filter(n -> Math.abs(n.getLocationX() - this.endNode2.getLocationX()) <= 0 && Math.abs(n.getLocationY() - this.endNode2.getLocationY()) <= 0).findFirst().orElse(null);
            PixelNode newEndNode1Neighbor = newNodes.stream().filter(n -> Math.abs(n.getLocationX() - this.endNode1Neighbor.getLocationX()) <= 0 && Math.abs(n.getLocationY() - this.endNode1Neighbor.getLocationY()) <= 0).findFirst().orElse(null);
            PixelNode newEndNode2Neighbor = newNodes.stream().filter(n -> Math.abs(n.getLocationX() - this.endNode2Neighbor.getLocationX()) <= 0 && Math.abs(n.getLocationY() - this.endNode2Neighbor.getLocationY()) <= 0).findFirst().orElse(null);
            if (newEndNode1 == null) {
                throw new IllegalStateException("Cannot find original end node 1");
            } else if (newEndNode2 == null) {
                throw new IllegalStateException("Cannot find original end node 2");
            } else {
                if (newEndNode1Neighbor == null) {
                    newEndNode1Neighbor = newEndNode2Neighbor;
                    newEndNode2Neighbor = null;
                    PixelNode temp = newEndNode1;
                    newEndNode1 = newEndNode2;
                    newEndNode2 = temp;
                }
                final PixelNode newEndNode1Final = newEndNode1;
                final PixelNode newEndNode1NeighborFinal = newEndNode1Neighbor;
                final PixelNode newEndNode2Final = newEndNode2;
                Iterable<PixelNode> iterable = () -> traverse(newEndNode1Final, newEndNode1NeighborFinal, newEndNode2Final);
                Iterator<?> i = iterable.iterator();
                return new OpenSegment(newEndNode1, newEndNode2, getLength(iterable), getDistance(iterable), newEndNode1Neighbor, newEndNode2Neighbor);
            }
        }

        /**
         * Gets the mean value of the component in a specified direction of the nodes
         * @param nodes the set of nodes in which the mean is calculated from
         * @param direction an angle in radians starting at 0 from the positive x-axis and moving towards the positive y-axis
         * @return the mean
         * @see #getDeviation(Collection, double)
         * @see #getSkewness(Collection, double)
         */
        public static double getMean(Collection<PixelNode> nodes, double direction) {
            double directionX = Math.cos(direction);
            double directionY = Math.sin(direction);
            double componentsSum = 0;
            for (PixelNode n : nodes) {
                componentsSum += directionX * n.getLocationX() + directionY * n.getLocationY();
            }
            return componentsSum / nodes.size();
        }

        public double getMean(double direction) {
            return getMean(getNodes(), direction);
        }

        /**
         * Gets the standard deviation of the component in a specified direction of the nodes
         * @param nodes the set of nodes in which the mean is calculated from
         * @param direction an angle in radians starting at 0 from the positive x-axis and moving towards the positive y-axis
         * @return the standard deviation
         * @see #getMean(Collection, double)
         * @see #getSkewness(Collection, double)
         */
        public static double getDeviation(Collection<PixelNode> nodes, double direction) {
            double mean = getMean(nodes, direction);
            double directionX = Math.cos(direction);
            double directionY = Math.sin(direction);
            double variance = 0;
            for (PixelNode n : nodes) {
                double component = directionX * n.getLocationX() + directionY * n.getLocationY();
                variance += Math.pow(component - mean, 2);
            }
            return Math.sqrt(variance / (nodes.size() - 1));
        }

        public double getDeviation(double direction) {
            return getDeviation(getNodes(), direction);
        }

        /**
         * Gets the skewness of the component in a specified direction of the nodes
         * @param nodes the set of nodes in which the mean is calculated from
         * @param direction an angle in radians starting at 0 from the positive x-axis and moving towards the positive y-axis
         * @return the skewness
         * @see #getMean(Collection, double)
         * @see #getDeviation(Collection, double)
         */
        public static double getSkewness(Collection<PixelNode> nodes, double direction) {
            double mean = getMean(nodes, direction);
            double directionX = Math.cos(direction);
            double directionY = Math.sin(direction);
            double sum = 0;
            for (PixelNode n : nodes) {
                double component = directionX * n.getLocationX() + directionY * n.getLocationY();
                sum += Math.pow(component - mean, 3);
            }
            return sum / ((nodes.size() - 1) * Math.pow(getDeviation(nodes, direction), 3));
        }

        public double getSkewness(double direction) {
            return getSkewness(getNodes(), direction);
        }

        /**
         * Calculates an approximation to the curvature based on the direction changes between each node in the segment
         * @return a relative value representing the curvature
         */
        public double getCurvature() {
            double curvature = 0;
            PixelNode previous = null;
            Double previousAngle = null;
            for (PixelNode n : getNodes()) {
                if (previous != null) {
                    double angle = Math.atan((n.getLocationY() - previous.getLocationY()) / (n.getLocationX() - previous.getLocationX()));
                    if (previousAngle != null) {
                        double angleDifference = angle - previousAngle;
                        angleDifference = (angleDifference % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
                        angleDifference -= angleDifference >= Math.PI ? Math.PI * 2 : 0;
                        curvature += angleDifference;
                    }
                    previousAngle = angle;
                }
                previous = n;
            }
            return curvature;
        }

        /**
         * Calculates an approximation to the curvature based on the distance of the curve relative to the distance between the end points
         * @return a relative value representing the curvature
         */
        public double getCurvature2() {
            return getCurvature() * Math.log(1 + getDistance() / getEndNode1().distanceTo(getEndNode2()));
        }

        /**
         * Calculates the curvature assuming a circular model
         * @return a relative value representing the curvature
         */
        public double getCurvature3() {
            return 1.0 / getRadius(getDistance(), getEndNode1().distanceTo(getEndNode2()));
        }

        /**
         * Calculates the curvature assuming a circular model while also taking the distance of the segment into account
         * @return a relative value representing the curvature
         */
        public double getCurvature3Mod() {
            double distance = getDistance();
            double endNodeDistance = getEndNode1().distanceTo(getEndNode2());
            return getCurvature3Mod(endNodeDistance, distance);
        }

        public static double getCurvature3Mod(double directDistance, double curveDistance) {
            return curveDistance / getRadius(curveDistance, directDistance);
        }

        /**
         * Calculates the radius of circle given the arc length and distance between 2 points that lie on the circle
         * @param arcLength the arc length between the 2 points on the circle
         * @param chordLength the distance between the 2 points on the circle
         * @return the radius of the circle
         */
        public static double getRadius(double arcLength, double chordLength) {
            return getRadius(arcLength, chordLength, 0, 1000);
        }

        private static double getRadius(double arcLength, double chordLength, double maxError, int maxIterations) {
            if (maxIterations <= 0) throw new IllegalArgumentException("Requires at least 1 iteration");
            if (maxError < 0) throw new IllegalArgumentException("Max Error(" + maxError + ") cannot be negative");
            if (Double.isNaN(arcLength) || Double.isNaN(chordLength)) {
                return Double.NaN;
            } else {
                double sign = (arcLength > 0) == (chordLength > 0) ? 1 : -1;
                arcLength = Math.abs(arcLength);
                final double absChordLength = Math.abs(chordLength);
                double halfChord = absChordLength / 2;
                double minHalfCircumference = Math.PI * halfChord;
                if (Double.isInfinite(arcLength) || Double.isInfinite(absChordLength)) {
                    return sign * Double.POSITIVE_INFINITY;
                } else if (arcLength == 0 || chordLength == 0) {
                    return Double.NaN;
                } else if (Math.abs(arcLength - absChordLength) <= maxError) {
                    return Double.POSITIVE_INFINITY;
                } else if (Math.abs(arcLength - minHalfCircumference) <= maxError) {
                    return sign * halfChord;
                } else if (absChordLength >= arcLength) {
                    throw new IllegalArgumentException("Chord length(" + absChordLength + ") cannot be greater than arc length(" + arcLength + ")");
                } else if (arcLength < minHalfCircumference){
                    return sign * DoubleFunction.newtonsMethod(x -> getArcLength1(x, absChordLength), x -> getArcLength1DerivativeWRTRadius(x, absChordLength), arcLength, Math.nextUp(halfChord), maxError, maxIterations);
                } else {
                    return sign * DoubleFunction.newtonsMethod(x -> getArcLength2(x, absChordLength), x -> getArcLength2DerivativeWRTRadius(x, absChordLength), arcLength, Math.nextUp(halfChord), maxError, maxIterations);
                }
            }
        }

        private static double getArcLength1(double radius, double chordLength) {
            if (radius < chordLength / 2) throw new IllegalArgumentException("chord(" + chordLength + ") does not fit in radius(" + radius + ")");
            return 2 * radius * Math.asin(chordLength / (2 * radius));
        }

        private static double getArcLength2(double radius, double chordLength) {
            if (radius < chordLength / 2) throw new IllegalArgumentException("chord(" + chordLength + ") does not fit in radius(" + radius + ")");
            return 2 * radius * (Math.PI - Math.asin(chordLength / (2 * radius)));
        }

        private static double getArcLength1DerivativeWRTRadius(double radius, double chordLength) {
            if (radius < chordLength / 2) throw new IllegalArgumentException("chord(" + chordLength + ") does not fit in radius(" + radius + ")");
            return 2 * Math.asin(chordLength / (2 * radius)) - 2 * chordLength / Math.sqrt(4 * radius * radius - chordLength * chordLength);
        }

        private static double getArcLength2DerivativeWRTRadius(double radius, double chordLength) {
            if (radius < chordLength / 2) throw new IllegalArgumentException("chord(" + chordLength + ") does not fit in radius(" + radius + ")");
            return 2 * Math.PI - getArcLength1DerivativeWRTRadius(radius, chordLength);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OpenSegment that = (OpenSegment) o;

            if (endNode1 != null ? !endNode1.equals(that.endNode1) : that.endNode1 != null) {
                return false;
            }
            if (endNode2 != null ? !endNode2.equals(that.endNode2) : that.endNode2 != null) {
                return false;
            }
            if (endNode1Neighbor != null ? !endNode1Neighbor.equals(that.endNode1Neighbor)
                    : that.endNode1Neighbor != null) {
                return false;
            }
            return endNode2Neighbor != null ? endNode2Neighbor.equals(that.endNode2Neighbor)
                    : that.endNode2Neighbor == null;
        }

        @Override
        public int hashCode() {
            int result = endNode1 != null ? endNode1.hashCode() : 0;
            result = 31 * result + (endNode2 != null ? endNode2.hashCode() : 0);
            result = 31 * result + (endNode1Neighbor != null ? endNode1Neighbor.hashCode() : 0);
            result = 31 * result + (endNode2Neighbor != null ? endNode2Neighbor.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            if (isSingle()) {
                return "OpenSegment{" +
                        "distance=" + distance +
                        ", length=" + length +
                        ", endNode=" + endNode1 +
                        '}';
            } else {
                return "OpenSegment{" +
                        "distance=" + distance +
                        ", length=" + length +
                        ", endNode1=" + endNode1 +
                        ", endNode2=" + endNode2 +
                        '}';
            }
        }
    }
}
