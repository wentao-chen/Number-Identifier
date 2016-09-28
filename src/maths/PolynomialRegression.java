package maths;

import java.util.Collection;


public class PolynomialRegression {

    private final Polynomial polynomial;
    private final double ssResidual;
    private final double ssTotal;

    public PolynomialRegression(Collection<Double> x, Collection<Double> y, int degree) {
        this(toPrimitiveArray(x), toPrimitiveArray(y), degree);
    }

    /**
     * Creates a polynomial regression of a specified degree. The values of x and y should be distinct within its own set.
     * @param x the set of distinct independent values
     * @param y the set of distinct dependent values
     * @param degree the degree of the polynomial
     */
    public PolynomialRegression(double[] x, double[] y, int degree) {
        if (x.length != y.length) throw new IllegalArgumentException("data set values size does not match");
        if (x.length == 0) throw new IllegalArgumentException("no data");
        if (degree < 0) throw new IllegalArgumentException("degree(" + degree + ") cannot be less than 0");
        double[][] entries = new double[x.length][degree + 1];
        for (int row = 0; row < entries.length; row++) {
            entries[row][0] = 1;
            for (int col = 1; col < entries[row].length; col++) {
                entries[row][col] = Math.pow(x[row], col);
            }
        }
        MatrixDouble xMatrix = MatrixDouble.createMatrix(entries);
        MatrixDouble xMatrixTranspose = xMatrix.transpose();
        MatrixDouble regressionPolynomial = xMatrixTranspose.multiplyRight(xMatrix).inverse().multiplyRight(xMatrixTranspose).multiplyRight(MatrixDouble.createVertical(y));
        double[] polynomial = new double[degree + 1];
        for (int i = 0 ; i < polynomial.length; i++) {
            polynomial[i] = regressionPolynomial.getEntry(i, 0);
        }
        this.polynomial = new Polynomial(polynomial);
        double yMean = 0;
        for (double y2 : y) {
            yMean += y2;
        }
        yMean /= y.length;
        double ssRes = 0;
        double ssTot = 0;
        for (int i = 0; i < x.length; i++) {
            ssRes += Math.pow(y[i] - this.polynomial.evaluate(x[i]), 2);
            ssTot += Math.pow(y[i] - yMean, 2);
        }
        this.ssResidual = ssRes;
        this.ssTotal = ssTot;
    }

    public Polynomial getPolynomial() {
        return polynomial;
    }

    public double getR2() {
        return 1.0 - ssResidual / ssTotal;
    }

    @Override
    public String toString() {
        return "PolynomialRegression{" + "R^2=" + getR2() + ", polynomial=" + polynomial + '}';
    }

    static double[] toPrimitiveArray(Collection<Double> c) {
        double[] array = new double[c.size()];
        int i = 0;
        for (double d : c) {
            array[i++] = d;
        }
        return array;
    }
}
