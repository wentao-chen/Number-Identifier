package main;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class MainNumberIdentifier {

    public static void main(String[] args) {
        MainNumberIdentifier.launchGUI();
    }

    private static void launchGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException expected) {
            // Default look and feel will be used
        }

        JFrame frame = new JFrame("Digit Identifier");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        AtomicInteger zoom = new AtomicInteger(0);
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        JLabel locationXLabel = new JLabel("X:");
        JLabel locationYLabel = new JLabel("Y:");
        JLabel zoomLabel = new JLabel("Zoom: x1.0");
        JLabel responseLabel = new JLabel("Is this your number?");
        responseLabel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 3));
        JButton responseButtonYes = new JButton("Yes");
        JButton responseButtonNo = new JButton("No");
        JPanel responseButtonPanel = new JPanel(new GridLayout(1, 2));
        responseButtonYes.addActionListener(l -> {
            responseLabel.setText("Thanks for the response. Glad I got it right.");
            responseButtonPanel.setVisible(false);
        });
        responseButtonPanel.add(responseButtonYes);
        responseButtonPanel.add(responseButtonNo);
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.add(responseLabel, BorderLayout.CENTER);
        responsePanel.add(responseButtonPanel, BorderLayout.EAST);
        responsePanel.setVisible(false);
        JPanel messagePanel = new JPanel(new BorderLayout());
        String originalMessage = "Write a one digit number (0 - 9) above and click analyze.";
        JLabel messageLabel = new JLabel(originalMessage);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 3));
        JButton messageButton = new JButton();
        messageButton.setVisible(false);
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        messagePanel.add(messageButton, BorderLayout.EAST);

        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(l -> {
            try {
                messageLabel.setText("Analyzing...");
                AnalysisResult result = MainNumberIdentifier.processImage(image, false);
                CharacterPattern resultBestPattern = result.getHighestCharacterCertainty().getPattern();
                if (result.getHighestCharacterCertainty().getCertainty() >= 0.5) {
                    responseLabel.setText("Is this your number? " + resultBestPattern);
                    for (ActionListener al : responseButtonNo.getActionListeners()) {
                        responseButtonNo.removeActionListener(al);
                    }
                    responseButtonNo.addActionListener(actionEvent -> {
                        JDialog responseDialog = new JDialog(frame, "Response", true);
                        responseDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                        JLabel label = new JLabel("What was the correct number?");
                        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                        JPanel selectionPanel = new JPanel(new GridLayout(3, 3));
                        CharacterDigitsPattern.CHARACTERS_DIGITS_SET.stream()
                                .filter(digitPattern -> digitPattern != resultBestPattern)
                                .forEach(digitPattern -> {
                                    JButton button = new JButton(digitPattern.toString());
                                    button.addActionListener(actionEvent2 -> responseDialog.dispose());
                                    selectionPanel.add(button);
                                });
                        AtomicBoolean cancelled = new AtomicBoolean(false);
                        JButton cancelButton = new JButton("It was " + resultBestPattern + ". Sorry you were right all along.");
                        cancelButton.addActionListener(actionEvent2 -> {
                            cancelled.set(true);
                            responseDialog.dispose();
                            for (ActionListener al : responseButtonYes.getActionListeners()) {
                                al.actionPerformed(actionEvent2);
                            }
                        });
                        responseDialog.getContentPane().setLayout(new BorderLayout());
                        responseDialog.getContentPane().add(label, BorderLayout.NORTH);
                        responseDialog.getContentPane().add(selectionPanel, BorderLayout.CENTER);
                        responseDialog.getContentPane().add(cancelButton, BorderLayout.SOUTH);
                        responseDialog.setSize(300, 300);
                        responseDialog.setVisible(true);

                        if (!cancelled.get()) {
                            responseLabel.setText("Thanks for the response. I'll try better next time.");
                            responseButtonPanel.setVisible(false);
                        }
                    });
                    responseButtonPanel.setVisible(true);
                    responsePanel.setVisible(true);
                    analyzeButton.setEnabled(false);
                }
                messageLabel.setText(result.getStandardMessage());
                for (ActionListener al : messageButton.getActionListeners()) {
                    messageButton.removeActionListener(al);
                }
                messageButton.addActionListener(e -> {
                    JDialog dialog = new JDialog(frame, "More Stats", true);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                    String[] columnNames = {"Digit", "Score"};
                    java.util.List<Object[]> data = new ArrayList<>();
                    AnalysisResult.CharacterCertainty[] characterCertainties = result.getCharacterCertainties();
                    class CertaintyWrapper implements Comparable<CertaintyWrapper> {
                        private CharacterPattern pattern;
                        private double certainty;
                        private CertaintyWrapper(AnalysisResult.CharacterCertainty c) {
                            this.pattern = c.getPattern();
                            this.certainty = c.getCertainty();
                        }
                        @Override
                        public int compareTo(CertaintyWrapper o) {
                            int cCompare = AnalysisResult.CharacterCertainty.compareToWithNaN(this.certainty, o.certainty);
                            if (cCompare != 0) {
                                return cCompare;
                            } else {
                                return o.pattern.toString().compareTo(this.pattern.toString());
                            }
                        }
                        @Override
                        public String toString() {
                            return String.format("%f", (Double.isNaN(this.certainty) ? 0 : this.certainty) * 100.0);
                        }
                    }
                    for (AnalysisResult.CharacterCertainty c : characterCertainties) {
                        data.add(new Object[]{c, new CertaintyWrapper(c)});
                    }
                    DefaultTableModel model = new DefaultTableModel(data.toArray(new Object[data.size()][]), columnNames) {
                        private static final long serialVersionUID = -3370399831142810408L;
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };
                    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                    JTable table = new JTable(model);
                    table.setFillsViewportHeight(true);
                    table.setAutoCreateRowSorter(true);
                    table.setRowSorter(sorter);
                    sorter.setComparator(1, (o1, o2) -> {
                        CertaintyWrapper c1 = o1 instanceof CertaintyWrapper ? (CertaintyWrapper) o1 : null;
                        CertaintyWrapper c2 = o2 instanceof CertaintyWrapper ? (CertaintyWrapper) o2 : null;
                        if (c1 == null && c2 == null) {
                            return o1.toString().compareTo(o2.toString());
                        } else if (c1 == null) {
                            return -1;
                        } else if (c2 == null) {
                            return 1;
                        } else {
                            return c1.compareTo(c2);
                        }
                    });
                    java.util.List<RowSorter.SortKey> sortKeys = new ArrayList<>();
                    sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
                    sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                    sorter.setSortKeys(sortKeys);
                    table.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            super.mouseClicked(e);
                            if (e.getClickCount() >= 2) {
                                int row = table.rowAtPoint(e.getPoint());
                                if (row >= 0 && row < characterCertainties.length) {
                                    Object characterObject = model.getValueAt(row, 0);
                                    if (characterObject instanceof AnalysisResult.CharacterCertainty) {
                                        AnalysisResult.CharacterCertainty character = (AnalysisResult.CharacterCertainty) characterObject;
                                        JDialog subDialog = new JDialog(dialog, "More Factors for " + character.getPattern(), true);
                                        subDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                                        String[] columnNames = {"Factor", "Score", "Value"};
                                        java.util.List<Object[]> data = new ArrayList<>();
                                        for (BasicCharacterPattern.CertaintyFactor f : character.getFactors()) {
                                            data.add(new Object[]{f.getName(), String.format("%f", f.getScore() * 100.0), f.getValue()});
                                        }
                                        DefaultTableModel model = new DefaultTableModel(data.toArray(new Object[data.size()][]), columnNames) {
                                            private static final long serialVersionUID = -3370399831142810408L;
                                            @Override
                                            public boolean isCellEditable(int row, int column) {
                                                return false;
                                            }
                                        };
                                        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                                        JTable table = new JTable(model);
                                        table.setFillsViewportHeight(true);
                                        table.setAutoCreateRowSorter(true);
                                        table.setRowSorter(sorter);
                                        sorter.setComparator(1, (o1, o2) -> {
                                            Double d1;
                                            Double d2;
                                            try {
                                                d1 = Double.parseDouble(o1.toString());
                                            } catch (NumberFormatException ex) {
                                                d1 = null;
                                            }
                                            try {
                                                d2 = Double.parseDouble(o2.toString());
                                            } catch (NumberFormatException ex) {
                                                d2 = null;
                                            }
                                            if (d1 == null && d2 == null) {
                                                return o1.toString().compareTo(o2.toString());
                                            } else if (d1 == null) {
                                                return -1;
                                            } else if (d2 == null) {
                                                return 1;
                                            } else {
                                                return AnalysisResult.CharacterCertainty.compareToWithNaN(d1, d2);
                                            }
                                        });
                                        java.util.List<RowSorter.SortKey> sortKeys = new ArrayList<>();
                                        sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
                                        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                                        sorter.setSortKeys(sortKeys);

                                        subDialog.getContentPane().add(new JScrollPane(table));
                                        subDialog.setSize(400, 250);
                                        subDialog.setVisible(true);
                                    }
                                }
                            }
                        }
                    });

                    JPanel statsTabPaneRow1 = new JPanel();
                    statsTabPaneRow1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Loops"));
                    statsTabPaneRow1.add(new JLabel(String.valueOf(result.graphDataSet.getLoops().size())));
                    JPanel statsTabPaneRow2 = new JPanel();
                    statsTabPaneRow2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Segments"));
                    statsTabPaneRow2.add(new JLabel(String.valueOf(result.graphDataSet.getSegments().size())));

                    JPanel statsTabPane = new JPanel();
                    statsTabPane.setLayout(new BoxLayout(statsTabPane, BoxLayout.PAGE_AXIS));
                    statsTabPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    statsTabPane.add(statsTabPaneRow1);
                    statsTabPane.add(statsTabPaneRow2);
                    statsTabPane.add(Box.createVerticalGlue());

                    Map<Image, String> resultImages = result.getImages();
                    java.util.List<Image> images = new ArrayList<>(resultImages.keySet());
                    Collections.sort(images, (i1, i2) -> resultImages.get(i1).compareTo(resultImages.get(i2)));
                    java.util.List<String> imageNames = images.stream().map(resultImages::get).collect(Collectors.toList());
                    AtomicInteger imageIndex = new AtomicInteger(0);
                    class ImageDisplayPanel extends JPanel {
                        private static final long serialVersionUID = 7222549103630542291L;
                        private double zoom = 1;
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            int index = imageIndex.get();
                            if (index >= 0 && index < images.size()) {
                                Image image = images.get(index);
                                double imageWidth = image.getWidth(null);
                                double imageHeight = image.getHeight(null);
                                double ratio = Math.max(Math.max(imageWidth / getWidth(), imageHeight / getHeight()), 1.0);
                                int width = (int) (imageWidth / ratio);
                                int height = (int) (imageHeight / ratio);
                                Graphics2D g2d = (Graphics2D) g;
                                AffineTransform originalTransform = g2d.getTransform();

                                double zoom = this.zoom;
                                g2d.scale(zoom, zoom);
                                g2d.translate((getWidth() / zoom - width) * 0.5, (getHeight() / zoom - height) * 0.5);

                                g2d.drawImage(image, 0, 0, width, height, null);
                                g2d.setTransform(originalTransform);
                            }
                        }
                    }
                    ImageDisplayPanel processedImagePane = new ImageDisplayPanel();
                    processedImagePane.addMouseWheelListener(mouseWheelEvent -> {
                        if (mouseWheelEvent.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                            if (mouseWheelEvent.getWheelRotation() > 0) {
                                if (processedImagePane.zoom > 0.1) {
                                    processedImagePane.zoom /= 1.1;
                                }
                            } else {
                                if (processedImagePane.zoom < 10.0) {
                                    processedImagePane.zoom *= 1.1;
                                }
                            }
                            processedImagePane.repaint();
                        }
                    });
                    JComboBox<String> processedImageComboBox = new JComboBox<>(imageNames.toArray(new String[imageNames.size()]));
                    processedImageComboBox.addActionListener(actionEvent -> {
                        int index = processedImageComboBox.getSelectedIndex();
                        imageIndex.set(index);
                        processedImagePane.repaint();
                    });
                    JPanel processedImagesTabPane = new JPanel(new BorderLayout());
                    processedImagesTabPane.add(processedImagePane, BorderLayout.CENTER);
                    processedImagesTabPane.add(processedImageComboBox, BorderLayout.SOUTH);

                    JTabbedPane tabbedPane = new JTabbedPane();
                    tabbedPane.addTab("Scores", new JScrollPane(table));
                    tabbedPane.addTab("Stats", new JScrollPane(statsTabPane));
                    tabbedPane.addTab("Processed Images", processedImagesTabPane);
                    dialog.getContentPane().add(tabbedPane);
                    dialog.setSize(300, 270);
                    dialog.setVisible(true);
                });
                messageButton.setText("More");
                messageButton.setVisible(true);
                frame.revalidate();
            } catch (IOException e) {
                messageLabel.setText(e.getMessage());
                messageButton.setVisible(false);
            }
        });
        analyzeButton.setEnabled(false);
        JPanel drawPanel = new JPanel() {
            private static final long serialVersionUID = 5740296928355067064L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, getPreferredSize().width, getPreferredSize().height, null);
            }
        };
        JScrollPane drawPanelScrollPane = new JScrollPane();
        drawPanel.setMinimumSize(new Dimension(image.getWidth(), image.getHeight()));
        drawPanel.setPreferredSize(drawPanel.getMinimumSize());
        class GeneralMouseListener implements MouseListener, MouseMotionListener {
            private Graphics graphics;
            private Point2D lastMouseDown = new Point2D.Double(Double.NaN, Double.NaN);
            private Point2D lastMouse2Down = new Point2D.Double(Double.NaN, Double.NaN);
            private GeneralMouseListener() {
                this.graphics = image.createGraphics();
                this.graphics.setColor(Color.WHITE);
                this.graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                this.graphics.setColor(Color.BLACK);
            }
            private void clear() {
                this.graphics.setColor(Color.WHITE);
                this.graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                this.graphics.setColor(Color.BLACK);
                drawPanel.repaint();
                messageLabel.setText("Content cleared.");
                messageButton.setVisible(false);
                analyzeButton.setEnabled(false);
                responsePanel.setVisible(false);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                    this.graphics.setColor(e.getButton() == MouseEvent.BUTTON1 ? Color.BLACK : Color.WHITE);
                    this.lastMouseDown.setLocation(e.getPoint());
                    double actualZoom = Math.pow(2, zoom.get());
                    this.graphics.fillRect((int) (e.getX() / actualZoom), (int) (e.getY() / actualZoom), 1, 1);
                    drawPanel.repaint();
                    analyzeButton.setEnabled(true);
                    responsePanel.setVisible(false);
                    messageLabel.setText(originalMessage);
                    messageButton.setVisible(false);
                } else if (e.getButton() == MouseEvent.BUTTON2) {
                    this.lastMouse2Down.setLocation(e.getPoint());
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                    this.lastMouseDown.setLocation(Double.NaN, Double.NaN);
                } else if (e.getButton() == MouseEvent.BUTTON2) {
                    this.lastMouse2Down.setLocation(Double.NaN, Double.NaN);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                double actualZoom = Math.pow(2, zoom.get());
                locationXLabel.setText("X: " + (int) (e.getX() / actualZoom));
                locationYLabel.setText("Y: " + (int) (e.getY() / actualZoom));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                locationXLabel.setText("X:");
                locationYLabel.setText("Y:");
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                double actualZoom = Math.pow(2, zoom.get());
                locationXLabel.setText("X: " + (int) (e.getX() / actualZoom));
                locationYLabel.setText("Y: " + (int) (e.getY() / actualZoom));
                if (!Double.isNaN(this.lastMouseDown.getX())) {
                    this.graphics.drawLine((int) (this.lastMouseDown.getX() / actualZoom), (int) (this.lastMouseDown.getY() / actualZoom), (int) (e.getX() / actualZoom), (int) (e.getY() / actualZoom));
                    drawPanel.repaint();
                    this.lastMouseDown.setLocation(e.getPoint());
                } else if (!Double.isNaN(this.lastMouse2Down.getX())) {
                    JViewport viewport = drawPanelScrollPane.getViewport();
                    Point viewPosition = viewport.getViewPosition();
                    int preX = e.getX() - viewPosition.x;
                    int preY = e.getY() - viewPosition.y;
                    viewport.setViewPosition(new Point(
                            Math.max(Math.min((int) lastMouse2Down.getX() - preX, drawPanel.getWidth() - viewport.getWidth()), 0),
                            Math.max(Math.min((int) lastMouse2Down.getY() - preY, drawPanel.getHeight() - viewport.getHeight()), 0)
                    ));
                    Point viewPosition2 = viewport.getViewPosition();
                    this.lastMouse2Down.setLocation(new Point(viewPosition2.x + preX, viewPosition2.y + preY));
                }
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                double actualZoom = Math.pow(2, zoom.get());
                locationXLabel.setText("X: " + (int) (e.getX() / actualZoom));
                locationYLabel.setText("Y: " + (int) (e.getY() / actualZoom));
            }
        }
        GeneralMouseListener generalMouseListener = new GeneralMouseListener();
        drawPanel.addMouseListener(generalMouseListener);
        drawPanel.addMouseMotionListener(generalMouseListener);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.PAGE_AXIS));
        buttonsPanel.add(analyzeButton);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(l -> generalMouseListener.clear());
        buttonsPanel.add(clearButton);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(l -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                File saveFile = new File(path.endsWith(".png") ? path : path + ".png");
                try {
                    ImageIO.write(image, "png", saveFile);
                    messageLabel.setText("Save to \"" + saveFile.getAbsolutePath() + "\" success.");
                    messageButton.setVisible(false);
                } catch (IOException e) {
                    messageLabel.setText("Save to \"" + saveFile.getAbsolutePath() + "\" failed.");
                    messageButton.setVisible(false);
                }
            }
        });
        buttonsPanel.add(saveButton);
        buttonsPanel.add(Box.createVerticalGlue());
        buttonsPanel.add(locationXLabel);
        buttonsPanel.add(locationYLabel);
        buttonsPanel.add(zoomLabel);
        buttonsPanel.add(new JLabel("Width: " + image.getWidth()));
        buttonsPanel.add(new JLabel("Height: " + image.getHeight()));

        frame.getContentPane().setLayout(new BorderLayout());
        JPanel centerContainerPanel = new JPanel(new GridBagLayout());
        centerContainerPanel.addMouseWheelListener(e -> {
            if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                double previousZoom = Math.pow(2, zoom.get());

                int maxZoom = 10;
                int minZoom = (int) -(Math.log(image.getWidth()) / Math.log(2));
                if (e.getWheelRotation() < 0 && zoom.get() < maxZoom) {
                    zoom.incrementAndGet();
                } else if (e.getWheelRotation() > 0 && zoom.get() > minZoom) {
                    zoom.decrementAndGet();
                }
                double actualZoom = Math.pow(2, zoom.get());
                zoomLabel.setText("Zoom: x" + actualZoom);
                drawPanel.setMinimumSize(new Dimension((int) (image.getWidth() * actualZoom), (int) (image.getHeight() * actualZoom)));
                drawPanel.setPreferredSize(drawPanel.getMinimumSize());
                drawPanel.repaint();
                drawPanel.revalidate();

                JViewport viewport = drawPanelScrollPane.getViewport();
                Point convertedPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), viewport);
                double topX = e.getX() / previousZoom * actualZoom - convertedPoint.getX();
                double topY = e.getY() / previousZoom * actualZoom - convertedPoint.getY();
                viewport.scrollRectToVisible(SwingUtilities.convertRectangle(drawPanel, new Rectangle((int) topX - 1, (int) topY - 1, viewport.getWidth(), viewport.getHeight()), drawPanelScrollPane));
            }
        });
        centerContainerPanel.add(drawPanel);
        drawPanelScrollPane.setViewportView(centerContainerPanel);
        drawPanelScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        drawPanelScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        frame.getContentPane().add(responsePanel, BorderLayout.NORTH);
        frame.getContentPane().add(drawPanelScrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(new JScrollPane(buttonsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.EAST);
        frame.getContentPane().add(new JScrollPane(messagePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.SOUTH);
        frame.setSize(610, 580);
        frame.setVisible(true);
    }

    private static AnalysisResult processImage(BufferedImage inputImage, boolean outputFinalImage) throws IOException {
        Map<Image, String> processImages = new IdentityHashMap<>();
        processImages.put(inputImage, "01) Original");
        BufferedImage image2 = ImageAnalysis.blackOrWhite(ImageAnalysis.filter(inputImage, ImageAnalysis.GRAYSCALE_FILTER, 1), 0.7);
        processImages.put(image2, "02) GrayScale");
        Predicate<Integer> isFilled = rgb -> (rgb & 0x00FFFFFF) != 0xFFFFFF;
        BufferedImage image = ImageAnalysis.fillLoops(image2, Color.BLACK, isFilled, s -> s.size() < 0.0001 * image2.getWidth() * image2.getHeight());

        PixelNodeGraph graph = PixelNodeGraph.createGraphFromImage(image, isFilled);
        graph.replaceRectangles();
        graph.replaceShortConnectorSegments();
        processImages.put(graph.toImage(inputImage.getWidth(), inputImage.getHeight()), "03) Nodes Reduction");

        Set<PixelNodeGraph.OpenSegment> segments = graph.getOpenSegments();
        graph.connectNearbyEnds();
        processImages.put(graph.toImage(inputImage.getWidth(), inputImage.getHeight()), "04) Segment Connection");
        if (segments.size() > 2) {
            // Remove tiny segments that is likely 'noise'
            double totalSegmentLength = segments.stream().mapToDouble(PixelNodeGraph.OpenSegment::getDistance).sum();
            double largestSegmentLength = segments.stream().mapToDouble(PixelNodeGraph.OpenSegment::getDistance).max().orElse(0);
            segments.stream().filter(s -> s.isEdge() && s.getDistance() < totalSegmentLength * 0.025 && s.getDistance() < largestSegmentLength * 0.075).forEach(graph::removeSegment);
            segments = graph.getOpenSegments();
            Set<PixelNodeGraph.OpenSegment> removeSegmentsLoops = new HashSet<>();
            for (PixelNodeGraph.OpenSegment s1 : segments) {
                removeSegmentsLoops.addAll(segments.stream()
                        .filter(s2 -> s1 != s2 && s1.getDistance() >= s2.getDistance() && ((s1.getEndNode1() == s2.getEndNode1() && s1.getEndNode2() == s2.getEndNode2()) || (s1.getEndNode1() == s2.getEndNode2() && s1.getEndNode2() == s2.getEndNode1()))
                                && s2.getDistance() < totalSegmentLength * 0.025 && s2.getDistance() < largestSegmentLength * 0.075)
                        .collect(Collectors.toSet()));
            }
            removeSegmentsLoops.forEach(graph::removeSegment);
            segments = graph.getOpenSegments();
        }
        processImages.put(graph.toImage(inputImage.getWidth(), inputImage.getHeight()), "05) Segment Removal");

        AnalysisResult result = MainNumberIdentifier.analyzeGraph(graph, false, CharacterDigitsPattern.CHARACTERS_DIGITS_SET);

        BufferedImage outputImage = PixelNodeGraph.toImage(segments, image.getWidth(), image.getHeight());
        processImages.put(outputImage, "06) Segments");
        if (outputFinalImage) {
            ImageIO.write(outputImage, "png", new File("Out.png"));
        }

        for (Image i : processImages.keySet()) {
            result.addImage(i, processImages.get(i));
        }
        return result;
    }

    private static AnalysisResult analyzeGraph(PixelNodeGraph pixelNodeGraph, boolean write, Collection<? extends BasicCharacterPattern> characterPatterns) {
        AnalysisResult result = new AnalysisResult(pixelNodeGraph);
        characterPatterns.forEach(result::addCharacterCertainty);
        if (write) {
            for (AnalysisResult.CharacterCertainty c : result.getCharacterCertainties()) {
                System.out.println(c.toString() + ": " + c.getCertainty());
            }
            System.out.println(result.getStandardMessage());
        }
        return result;
    }

    static class AnalysisResult {
        private final PixelNodeGraph graph;
        private final BasicCharacterPattern.GraphDataSet graphDataSet;
        private final List<CharacterCertainty> characterCertainties = new ArrayList<>();
        private final Map<Image, String> images = new IdentityHashMap<>();

        private AnalysisResult(PixelNodeGraph graph) {
            this.graph = graph;
            graphDataSet = new BasicCharacterPattern.GraphDataSet(this.graph);
        }

        public PixelNodeGraph getGraph() {
            return graph;
        }

        public CharacterCertainty getHighestCharacterCertainty() {
            double maxCertainty = Double.NEGATIVE_INFINITY;
            CharacterCertainty maxCharacter = null;
            for (CharacterCertainty c : getCharacterCertainties(null)) {
                double certainty = c.getCertainty();
                if (certainty > maxCertainty) {
                    maxCharacter = c;
                    maxCertainty = certainty;
                }
            }
            return maxCharacter;
        }

        public CharacterCertainty[] getCharacterCertainties() {
            return getCharacterCertainties(CharacterCertainty::compareTo);
        }

        public CharacterCertainty[] getCharacterCertainties(Comparator<CharacterCertainty> comparator) {
            CharacterCertainty[] certainties = characterCertainties.toArray(new CharacterCertainty[characterCertainties.size()]);
            if (comparator != null) {
                Arrays.sort(certainties, comparator);
            }
            return certainties;
        }

        private Map<Image, String> getImages() {
            return images;
        }

        private void addCharacterCertainty(CharacterPattern p) {
            characterCertainties.add(new CharacterCertainty(p, p.getCertainty(graph)));
        }

        private void addCharacterCertainty(BasicCharacterPattern p) {
            characterCertainties.add(new CharacterCertainty(p, p.getCertaintyFactors(graphDataSet)));
        }

        public void addImage(Image image, String name) {
            images.put(image, name);
        }

        public String getStandardMessage() {
            CharacterCertainty[] certainties = getCharacterCertainties();
            CharacterCertainty highestConfidence = certainties.length >= 1 ? certainties[0] : null;
            StringBuilder message = new StringBuilder();
            if (highestConfidence != null) {
                if (highestConfidence.getCertainty() > 0.9) {
                    message.append("Obviously that's ");
                } else if (highestConfidence.getCertainty() > 0.7) {
                    message.append("That's ");
                } else if (highestConfidence.getCertainty() > 0.5) {
                    message.append("Probably ");
                } else if (highestConfidence.getCertainty() > 0.2) {
                    message.append("Guessing that's ");
                } else {
                    message.append("Not too sure. Maybe that's ");
                }
                message.append(highestConfidence.toString()).append(' ').append(String.format("(%f%%)", highestConfidence.getCertainty() * 100.0));
                CharacterCertainty secondHighestConfidence = certainties.length >= 2 ? certainties[1] : null;
                if (secondHighestConfidence != null) {
                    boolean hasSecondMessage = true;
                    if (secondHighestConfidence.getCertainty() / highestConfidence.getCertainty() > 0.95) {
                        message.append(" or ").append(secondHighestConfidence.toString());
                    } else if (secondHighestConfidence.getCertainty() / highestConfidence.getCertainty() > 0.85) {
                        message.append(" or maybe a ").append(secondHighestConfidence.toString());
                    } else if (secondHighestConfidence.getCertainty() / highestConfidence.getCertainty() > 0.6) {
                        message.append(" but also could be a ").append(secondHighestConfidence.toString());
                    } else if (secondHighestConfidence.getCertainty() / highestConfidence.getCertainty() > 0.35) {
                        message.append(" with a bit of ").append(secondHighestConfidence.toString()).append(" detected");
                    } else {
                        hasSecondMessage = false;
                    }
                    if (hasSecondMessage) {
                        message.append(' ').append(String.format("(%f%%)", secondHighestConfidence.getCertainty() * 100.0));
                    }
                }
            }
            return message.toString();
        }

        static class CharacterCertainty implements Comparable<CharacterCertainty> {
            private final CharacterPattern pattern;
            private final double certainty;
            private final BasicCharacterPattern.CertaintyFactor[] factors;
            CharacterCertainty(CharacterPattern pattern, double certainty) {
                this(pattern, new BasicCharacterPattern.CertaintyFactor("Certainty", certainty, certainty));
            }
            CharacterCertainty(CharacterPattern pattern, BasicCharacterPattern.CertaintyFactor... factors) {
                if (factors == null || factors.length == 0) throw new IllegalArgumentException("No factors specified");
                this.pattern = pattern;
                this.factors = Arrays.copyOf(factors, factors.length);
                this.certainty = BasicCharacterPattern.getCertainty(this.factors);
            }
            public CharacterPattern getPattern() {
                return pattern;
            }
            public double getCertainty() {
                return certainty;
            }
            BasicCharacterPattern.CertaintyFactor[] getFactors() {
                return factors;
            }
            @Override
            public int compareTo(CharacterCertainty o) {
                return compareToWithNaN(o.certainty, certainty);
            }
            @Override
            public String toString() {
                return pattern.toString();
            }

            public static int compareToWithNaN(double d1, double d2) {
                boolean n1 = Double.isNaN(d1);
                boolean n2 = Double.isNaN(d2);
                if (n1 && n2) {
                    return 0;
                } else if (n1) {
                    return -1;
                } else if (n2) {
                    return 1;
                } else {
                    return Double.compare(d1, d2);
                }
            }
        }
    }
}
