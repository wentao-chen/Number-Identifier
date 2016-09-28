package maths;

import java.util.*;


public class RationalPolynomial implements DifferentiableFunction {
    private final Polynomial numerator;
    private final Polynomial denominator;
    private final boolean nonNegativeDenominator;

    public RationalPolynomial(Polynomial numerator, Polynomial denominator) {
        this(numerator, denominator, false);
    }

    private RationalPolynomial(Polynomial numerator, Polynomial denominator, boolean nonNegativeDenominator) {
        this.numerator = numerator;
        this.denominator = denominator;
        this.nonNegativeDenominator = nonNegativeDenominator;
    }

    public Polynomial getNumerator() {
        return numerator;
    }

    public Polynomial getDenominator() {
        return denominator;
    }

    @Override
    public double evaluate(double x) {
        return numerator.evaluate(x) / denominator.evaluate(x);
    }

    public RationalPolynomial add(RationalPolynomial rationalPolynomial) {
        Polynomial numerator = getNumerator().multiply(rationalPolynomial.getDenominator()).add(rationalPolynomial.getNumerator().multiply(getDenominator()));
        Polynomial denominator = getDenominator().multiply(rationalPolynomial.getDenominator());
        return new RationalPolynomial(numerator, denominator);
    }

    public RationalPolynomial negate() {
        return new RationalPolynomial(getNumerator().negate(), getDenominator());
    }

    public RationalPolynomial subtract(RationalPolynomial rationalPolynomial) {
        return add(rationalPolynomial.negate());
    }

    public RationalPolynomial multiply(RationalPolynomial rationalPolynomial) {
        return new RationalPolynomial(getNumerator().multiply(rationalPolynomial.getNumerator()), getDenominator().multiply(rationalPolynomial.getDenominator()));
    }

    public RationalPolynomial reciprocal() {
        return new RationalPolynomial(getDenominator(), getNumerator());
    }

    public RationalPolynomial divide(RationalPolynomial rationalPolynomial) {
        return multiply(rationalPolynomial.reciprocal());
    }

    @Override
    public RationalPolynomial differentiate() {
        Polynomial numerator = this.denominator
                .multiply(this.numerator.differentiate()).subtract(this.numerator.multiply(this.denominator.differentiate()));
        Polynomial denominator = this.denominator.multiply(this.denominator);
        return new RationalPolynomial(numerator, denominator, true);
    }

    public List<Double> getSignChangesPoints(double x1, double x2) {
        return getSignChangesPoints(x1, x2, (x2 - x1) / 1000000000d);
    }

    public List<Double> getSignChangesPoints(double x1, double x2, double infinitesmalValue) {
        if (nonNegativeDenominator) {
            return numerator.getSignChangesLocations(x1, x2);
        } else {
            Set<Double> potentialSignChangeLocationsNumeratorSet = new HashSet<>();
            potentialSignChangeLocationsNumeratorSet.addAll(numerator.getSignChangesLocations(x1, x2));
            potentialSignChangeLocationsNumeratorSet.add(x2);
            List<Double> potentialSignChangeLocationsNumerator = new LinkedList<>(potentialSignChangeLocationsNumeratorSet);
            Collections.sort(potentialSignChangeLocationsNumerator);
            Set<Double> potentialSignChangeLocationsSet = new HashSet<>();
            double previousX = x1;
            for (double x : potentialSignChangeLocationsNumerator) {
                potentialSignChangeLocationsSet.addAll(denominator.getSignChangesLocations(previousX, x));
                potentialSignChangeLocationsSet.add(x);
                previousX = x;
            }
            List<Double> potentialSignChangeLocations = new LinkedList<>(potentialSignChangeLocationsSet);
            Collections.sort(potentialSignChangeLocations);

            List<Double> changes = new ArrayList<>();
            Boolean lastSign = null;
            for (double x : potentialSignChangeLocations) {
                double n = numerator.evaluate(x);
                double d = denominator.evaluate(x);
                while (Math.abs(n) < infinitesmalValue || Math.abs(d) < infinitesmalValue) {
                    x = Math.nextUp(infinitesmalValue);
                    n = numerator.evaluate(x);
                    d = denominator.evaluate(x);
                }
                boolean isPositive = Math.signum(n) * Math.signum(d) > 0;
                if (lastSign == null) {
                    lastSign = isPositive;
                } else if (lastSign != isPositive) {
                    lastSign = !lastSign;
                    changes.add(x);
                }
            }
            return changes;
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

        RationalPolynomial that = (RationalPolynomial) o;

        if (numerator != null ? !numerator.equals(that.numerator) : that.numerator != null) {
            return false;
        }
        return denominator != null ? denominator.equals(that.denominator) : that.denominator == null;
    }

    @Override
    public int hashCode() {
        int result = numerator != null ? numerator.hashCode() : 0;
        result = 31 * result + (denominator != null ? denominator.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RationalPolynomial{" + "numerator=" + numerator + ", denominator=" + denominator + '}';
    }
}
