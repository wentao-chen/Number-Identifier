package main;


public abstract class BasicNumberCharacterPattern extends BasicCharacterPattern {
    private final int number;
    public BasicNumberCharacterPattern(int number) {
        if (number < 0 || number > 9) throw new IllegalArgumentException("Number(" + number + ") must be a single character");
        this.number = number;
    }
    public final int getValue() {
        return this.number;
    }
    @Override
    public String toString() {
        return String.valueOf(getValue());
    }
}
