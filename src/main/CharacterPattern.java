package main;

public interface CharacterPattern {
    /**
     * Calculates the probability from 0.0 to 1.0 of how likely the graph resembles this character pattern.<br>
     * @param graph the tested graph
     * @return a value from 0.0 to 1.0 of the likelihood of resembling the pattern
     */
    double getCertainty(PixelNodeGraph graph);
}
