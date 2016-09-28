package maths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class Polynomial implements DifferentiableFunction {
    public static final Polynomial ZERO = new Polynomial(0);

    private final double[] coefficients;

    public Polynomial(double... coefficients) {
        if (coefficients.length == 0) {
            this.coefficients = new double[]{0};
        } else {
            List<Double> nonZeroCoefficients = new ArrayList<>(coefficients.length);
            boolean foundFirstNonZero = false;
            for (int i = coefficients.length - 1; i >= 0; i--) {
                if (!Double.isFinite(coefficients[i])) throw new IllegalArgumentException("Coefficients must be finite: " + Arrays.toString(coefficients));
                if (coefficients[i] != 0 || foundFirstNonZero) {
                    foundFirstNonZero = true;
                    nonZeroCoefficients.add(0, coefficients[i]);
                }
            }
            if (nonZeroCoefficients.size() == 0) {
                this.coefficients = new double[]{0};
            } else {
                this.coefficients = PolynomialRegression.toPrimitiveArray(nonZeroCoefficients);
            }
        }
    }

    public double getCoefficient(int i) {
        return coefficients[i];
    }

    public double lead() {
        for (int i = coefficients.length - 1; i >= 0; i--) {
            if (coefficients[i] != 0) {
                return coefficients[i];
            }
        }
        return 0;
    }

    /**
     * Gets the degree of the polynomial. If the polynomial is the zero polynomial, returns -1.
     * @return the degree of the polynomial for non-zero polynomials; otherwise, -1
     */
    public int getDegree() {
        return isZero() ? -1 : coefficients.length - 1;
    }

    @Override
    public double evaluate(double x) {
        if (Double.isNaN(x)) {
            return x;
        } else if (Double.isInfinite(x)) {
            if (coefficients.length == 1) {
                return coefficients[0];
            } else {
                return (coefficients.length % 2 == 0 ? x : Double.POSITIVE_INFINITY) * Math.signum(
                        coefficients[coefficients.length - 1]);
            }
        } else {
            if (x < 1) {
                double sum = 0;
                for (int i = coefficients.length - 1; i >= 1; i--) {
                    sum += coefficients[i] * Math.pow(x, i);
                }
                return sum + coefficients[0];
            } else {
                double sum = coefficients[0];
                for (int i = 1; i < coefficients.length; i++) {
                    sum += coefficients[i] * Math.pow(x, i);
                }
                return sum;
            }
        }
    }

    public boolean isZero() {
        return coefficients.length == 1 && coefficients[0] == 0;
    }

    public boolean isZero(double maxError) {
        return coefficients.length == 1 && Math.abs(coefficients[0]) <= maxError;
    }

    public boolean isZero2(double maxError) {
        for (double c : coefficients) {
            if (Math.abs(c) > maxError) {
                return false;
            }
        }
        return true;
    }

    public double getAverage(double x1, double x2) {
        return getAntiDerivative().getDerivativeAverage(x1, x2);
    }

    @Override
    public Polynomial differentiate() {
        if (coefficients.length == 1) {
            return ZERO;
        } else {
            double[] derivative = new double[coefficients.length - 1];
            for (int i = 1; i < coefficients.length; i++) {
                derivative[i - 1] = coefficients[i] * i;
            }
            return new Polynomial(derivative);
        }
    }

    public Polynomial getAntiDerivative() {
        double[] antiDerivative = new double[coefficients.length + 1];
        for (int i = 1; i < antiDerivative.length; i++) {
            antiDerivative[i] = coefficients[i - 1] / i;
        }
        return new Polynomial(antiDerivative);
    }

    public Polynomial negate() {
        double[] newPolynomial = new double[coefficients.length];
        for (int i = 0; i < newPolynomial.length; i++) {
            newPolynomial[i] = -coefficients[i];
        }
        return new Polynomial(newPolynomial);
    }

    public Polynomial add(Polynomial polynomial) {
        double[] newPolynomial = new double[Math.max(coefficients.length, polynomial.coefficients.length)];
        for (int i = 0; i < newPolynomial.length; i++) {
            double x = 0;
            double y = 0;
            if (i < coefficients.length) {
                x = coefficients[i];
            }
            if (i < polynomial.coefficients.length) {
                y = polynomial.coefficients[i];
            }
            newPolynomial[i] = x + y;
        }
        return new Polynomial(newPolynomial);
    }

    public Polynomial subtract(Polynomial polynomial) {
        return add(polynomial.negate());
    }

    /**
     * Creates a copy of this polynomial of a certain degree by setting all coefficients greater than the desired degree to 0.<br>
     *     If the specified degree is greater or equal to the degree of this polynomial, this polynomial is returned.
     * @param degree the degree of the new polynomial
     * @return a polynomial of {@code degree} with coefficients equal to the coefficients of this polynomial
     */
    public Polynomial crop(int degree) {
        if (degree >= getDegree()) {
            return this;
        } else if (degree == -1) {
            return Polynomial.ZERO;
        } else {
            return new Polynomial(Arrays.copyOf(coefficients, degree + 1));
        }
    }

    public Polynomial remove(int coefficient) {
        if (coefficient == coefficients.length - 1) {
            if (coefficient == 0) {
                return Polynomial.ZERO;
            } else {
                return new Polynomial(Arrays.copyOf(coefficients, coefficients.length - 1));
            }
        } else {
            double[] newPolynomial = Arrays.copyOf(coefficients, coefficients.length);
            newPolynomial[coefficient] = 0;
            return new Polynomial(newPolynomial);
        }
    }

    public Polynomial multiply(Polynomial polynomial) {
        double[] newPolynomial = new double[coefficients.length + polynomial.coefficients.length - 1];
        for (int i = 0; i < coefficients.length; i++) {
            for (int j = 0; j < polynomial.coefficients.length; j++) {
                newPolynomial[i + j] += coefficients[i] * polynomial.coefficients[j];
            }
        }
        return new Polynomial(newPolynomial);
    }

    /**
     * Divides this polynomial by a divisor polynomial using long division.
     * @param divisor the divisor polynomial
     * @return an array of exactly 2 polynomials with the quotient at index 0 and the remainder at index 1
     */
    public Polynomial[] divide(Polynomial divisor) {
        if (divisor.equals(Polynomial.ZERO)) throw new IllegalArgumentException("Zero polynomial used as denominator");
        Polynomial q = Polynomial.ZERO;
        Polynomial r = this;
        while (!r.equals(Polynomial.ZERO) && r.getDegree() >= divisor.getDegree()) {
            double[] polynomialArray = new double[r.getDegree() - divisor.getDegree() + 1];
            polynomialArray[polynomialArray.length - 1] = r.lead() / divisor.lead();
            Polynomial p = new Polynomial(polynomialArray);
            q = q.add(p);
            int initialDegree = r.getDegree();
            r = r.subtract(p.multiply(divisor)).crop(initialDegree - 1);
        }
        return new Polynomial[] {q, r};
    }

    public Polynomial quotient(Polynomial divisor) {
        return divide(divisor)[0];
    }

    public Polynomial remainder(Polynomial divisor) {
        return divide(divisor)[1];
    }

    private int sturmChainSignChanges(Collection<Polynomial> sturmChain, double x) {
        Boolean currentSign = null;
        int signChanges = 0;
        for (Polynomial p : sturmChain) {
            double y = p.evaluate(x);
            if (y != 0) {
                if (currentSign == null) {
                    currentSign = y > 0;
                } else if (currentSign != (y > 0)) {
                    currentSign = !currentSign;
                    signChanges++;
                }
            }
        }
        return signChanges;
    }

    /**
     * Gets arbitrary values in the half-open interval ({@code x1}, {@code x2}] that meet the following criteria:
     * <ol>
     *     <li>If the value is the first in the list then:<br>
     *         There is exactly 1 root between {@code x1} (exclusive) and the current value (inclusive)
     *     </li>
     *     <li>If the value is not the first in the list then:<br>
     *         There is exactly 1 root between the previous value in the list (exclusive) and the current value (inclusive)
     *     </li>
     *     <li>If the value is last in the list and not equal to {@code x2} then:<br>
     *         There is no roots between the current value (exclusive) and {@code x2} (inclusive)
     *     </li>
     * </ol>
     * Note: An empty list implies no roots in the half-open interval.<br>
     * Note: The algorithm uses bisection method. Although, not a guarantee, the values in the list will occur at half-intervals.
     * @param x1 the lower bound of the half-open interval
     * @param x2 the upper bound of the half-open interval
     * @return arbitary values that separate roots of the function
     */
    public List<Double> getSignChangesLocations(double x1, double x2) {
        List<Double> signChangeLocations = new ArrayList<>();
        double center = (x1 + x2) / 2.0;
        int totalRoots = getZerosCount(x1, x2);
        int leftRoots = getZerosCount(x1, center);
        int rightRoots = totalRoots - leftRoots;
        if (totalRoots == 0) {
            return signChangeLocations;
        } else if (totalRoots == 1) {
            signChangeLocations.add(x2);
            return signChangeLocations;
        } else if (leftRoots == 1 && rightRoots == 1) {
            signChangeLocations.add(center);
            signChangeLocations.add(x2);
            return signChangeLocations;
        } else if (leftRoots == 0) {
            return getSignChangesLocations(center, x2);
        } else if (rightRoots == 0) {
            return getSignChangesLocations(x1, center);
        } else if (leftRoots == 1) {
            signChangeLocations.add(center);
            signChangeLocations.addAll(getSignChangesLocations(center, x2));
            return signChangeLocations;
        } else if (rightRoots == 1) {
            signChangeLocations.addAll(getSignChangesLocations(x1, center));
            signChangeLocations.add(x2);
            return signChangeLocations;
        } else {
            signChangeLocations.addAll(getSignChangesLocations(x1, center));
            signChangeLocations.addAll(getSignChangesLocations(center, x2));
            return signChangeLocations;
        }
    }

    /**
     * Determines the number of zeros over all real numbers.<br>
     *     Convenience method for {@link #getZerosCount(double, double)} with arguments -infinity and infinity
     * @return the total number of zeros in the real domain
     */
    public int getZerosCount() {
        return getZerosCount(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Determines the number of zeros in a specified half-open interval ({@code x1}, {@code x2}] using Strum's theorem.
     * @param x1 the lower bound of the half-open interval
     * @param x2 the upper bound of the half-open interval
     * @return the number of zeros in the interval
     */
    public int getZerosCount(double x1, double x2) {
        if (x2 <= x1) throw new IllegalArgumentException("Interval(" + x1 + ", " + x2 + ") must be strictly increasing with x2 > x1");
        List<Polynomial> sturmChain = new ArrayList<>();
        Polynomial previous = this;
        Polynomial current = differentiate();
        sturmChain.add(previous);
        sturmChain.add(current);
        while (true) {
            Polynomial next = previous.remainder(current).crop(current.getDegree() - 1);
            if (!next.isZero()) {
                previous = current;
                current = next.negate();
                sturmChain.add(current);
            } else {
                break;
            }
        }
        return sturmChainSignChanges(sturmChain, x1) - sturmChainSignChanges(sturmChain, x2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Polynomial that = (Polynomial) o;

        return Arrays.equals(coefficients, that.coefficients);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coefficients);
    }

    @Override
    public String toString() {
        String newLine = System.lineSeparator();
        StringBuilder s = new StringBuilder("Polynomial{");
        boolean hasPreviousTerm = false;
        for (int i = coefficients.length - 1; i >= 0; i--) {
            if (coefficients[i] != 0) {
                if (hasPreviousTerm) {
                    if (coefficients[i] < 0) {
                        s.append(" - ");
                    } else {
                        s.append(" + ");
                    }
                    double absCoefficient = Math.abs(coefficients[i]);
                    if (i == 0 || absCoefficient >= Math.nextUp(1.0) || absCoefficient <= Math.nextDown(1.0)) {
                        s.append(Math.abs(coefficients[i]));
                    }
                } else {
                    double absCoefficient = Math.abs(coefficients[i]);
                    if (i == 0 || absCoefficient >= Math.nextUp(1.0) || absCoefficient <= Math.nextDown(1.0)) {
                        s.append(coefficients[i]);
                    } else if (coefficients[i] < 0) {
                        s.append('-');
                    }
                }
                if (i > 0) {
                    s.append('x');
                }
                if (i > 1) {
                    s.append('^').append(i);
                }
                hasPreviousTerm = true;
            }
        }
        return s.append("}").toString();
    }
}
