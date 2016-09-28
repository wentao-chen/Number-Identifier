package maths;

import java.util.Arrays;


public class MatrixDouble {
    private final double[][] entries;
    private MatrixDouble(double[][] entries) {
        if (entries.length == 0) throw new IllegalArgumentException("Empty Matrix");
        Integer columns = null;
        for (double[] row : entries) {
            if (row.length == 0) throw new IllegalArgumentException("Empty Matrix");
            else if (columns == null) columns = row.length;
            else if (columns != row.length) throw new IllegalArgumentException("Matrix contains rows of different sizes");
        }
        this.entries = entries;
    }

    public static MatrixDouble createVertical(double[] entries) {
        double[][] newEntries = new double[entries.length][1];
        for (int i = 0; i < entries.length; i++) {
            newEntries[i][0] = entries[i];
        }
        return new MatrixDouble(newEntries);
    }

    public static MatrixDouble createHorizontal(double[] entries) {
        double[][] newEntries = new double[1][];
        newEntries[0] = Arrays.copyOf(entries, entries.length);
        return new MatrixDouble(newEntries);
    }

    private static double[][] copyArray2D(double[][] entries) {
        double[][] entriesCopy = new double[entries.length][];
        for (int i = 0; i < entriesCopy.length; i++) {
            entriesCopy[i] = Arrays.copyOf(entries[i], entries[i].length);
        }
        return entriesCopy;
    }

    public static MatrixDouble createMatrix(MatrixDouble m) {
        return createMatrix(m.entries);
    }

    public static MatrixDouble createMatrix(double[][] entries) {
        return new MatrixDouble(copyArray2D(entries));
    }

    public static MatrixDouble createZero(int n) {
        return new MatrixDouble(new double[n][n]);
    }

    public static MatrixDouble createIdentity(int n) {
        double[][] entries = new double[n][n];
        for (int i = 0; i < n; i++) {
            entries[i][i] = 1;
        }
        return new MatrixDouble(entries);
    }

    public MatrixDouble transpose() {
        double[][] entries = new double[getColumns()][];
        for (int y = 0; y < entries.length; y++) {
            entries[y] = new double[this.entries.length];
            for (int x = 0; x < entries[y].length; x++) {
                entries[y][x] = this.entries[x][y];
            }
        }
        return new MatrixDouble(entries);
    }

    public MatrixDouble appendHorizontal(MatrixDouble m) {
        if (getRows() != m.getRows()) throw new IllegalArgumentException("Invalid matrix dimensions for appending " + sizeString() + " <-> " + m.sizeString());
        double[][] entries = new double[this.entries.length][];
        for (int y = 0; y < entries.length; y++) {
            entries[y] = new double[this.entries[y].length + m.entries[y].length];
            System.arraycopy(this.entries[y], 0, entries[y], 0, this.entries[y].length);
            System.arraycopy(m.entries[y], 0, entries[y], this.entries[y].length, m.entries[y].length);
        }
        return new MatrixDouble(entries);
    }

    public MatrixDouble negate() {
        double[][] entries = new double[this.entries.length][];
        for (int y = 0; y < entries.length; y++) {
            entries[y] = new double[this.entries[y].length];
            for (int x = 0; x < entries[y].length; x++) {
                entries[y][x] = -this.entries[y][x];
            }
        }
        return new MatrixDouble(entries);
    }

    public MatrixDouble add(MatrixDouble m) {
        if (getRows() != m.getRows() || getColumns() != m.getColumns()) throw new IllegalArgumentException("Invalid matrix dimensions for addition " + sizeString() + " + " + m.sizeString());
        double[][] entries = new double[this.entries.length][];
        for (int y = 0; y < entries.length; y++) {
            entries[y] = new double[this.entries[y].length];
            for (int x = 0; x < entries[y].length; x++) {
                entries[y][x] = this.entries[y][x] + m.entries[y][x];
            }
        }
        return new MatrixDouble(entries);
    }

    public MatrixDouble multiplyLeft(MatrixDouble m) {
        return m.multiplyRight(this);
    }

    /**
     * Multiplies 2 matrices with matrix {@code m} on the right of {@code this} matrix (i.e. [this][m])
     * @param m the multiplicand matrix
     * @return the product matrix
     */
    public MatrixDouble multiplyRight(MatrixDouble m) {
        if (getColumns() != m.getRows()) throw new IllegalArgumentException("Invalid matrix dimensions for multiplication " + sizeString() + " * " + m.sizeString());
        double[][] entries = new double[this.entries.length][];
        for (int y = 0; y < entries.length; y++) {
            entries[y] = new double[m.entries[0].length];
            for (int x = 0; x < entries[y].length; x++) {
                double dotProduct = 0;
                for (int i = 0, size = this.entries[y].length; i < size; i++) {
                    dotProduct += this.entries[y][i] * m.entries[i][x];
                }
                entries[y][x] = dotProduct;
            }
        }
        return new MatrixDouble(entries);
    }

    private static boolean isRowZero(double[][] entries, int row) {
        return leadingCoefficient(entries, row) == null;
    }

    private static Double leadingCoefficient(double[][] entries, int row) {
        for (double e : entries[row]) {
            if (e != 0) {
                return e;
            }
        }
        return null;
    }

    private static void rowSwap(double[][] entries, int row1, int row2) {
        double[] temp = entries[row1];
        entries[row1] = entries[row2];
        entries[row2] = temp;
    }

    private static void rowMultiply(double[][] entries, int row, double factor) {
        if (factor == 0) throw new IllegalArgumentException("multiplying row by a factor of 0 is not an elementary operation");
        for (int i = 0; i < entries[row].length; i++) {
            entries[row][i] *= factor;
        }
    }

    private static void rowAdd(double[][] entries, int changeRow, int addRow, double factor) {
        for (int i = 0; i < entries[changeRow].length; i++) {
            entries[changeRow][i] += factor * entries[addRow][i];
        }
    }

    private static boolean reduceToREF(double[][] entries) {
        int m = entries.length;
        int n = entries[0].length;
        for (int i = 0; i < Math.min(m, n); i++) {
            int pivotMax = 0;
            double pivotMaxAbsValue = 0;
            for (int j = i; j < m; j++) {
                if (Math.abs(entries[j][i]) > pivotMaxAbsValue) {
                    pivotMax = j;
                    pivotMaxAbsValue = Math.abs(entries[j][i]);
                }
            }
            if (pivotMaxAbsValue == 0) return false; // Matrix is singular
            rowSwap(entries, i, pivotMax);
            for (int j = i + 1; j < m; j++) {
                double factor = entries[j][i] / entries[i][i];
                for (int k = i + 1; k < n; k++) {
                    entries[j][k] -= entries[i][k] * factor;
                }
                entries[j][i] = 0;
            }
        }
        return true;
    }

    private static boolean reduceToRREFFromREF(double[][] entries) {
        for (int y = entries.length - 1; y >= 0; y--) {
            int leadingCoefficientPosition = entries[y].length - 1;
            Double leadingCoefficient = null;
            for (int x = 0; x < entries[y].length; x++) {
                if (entries[y][x] != 0) {
                    leadingCoefficientPosition = x;
                    leadingCoefficient = entries[y][x];
                    break;
                }
            }
            if (leadingCoefficient == null) {
                return false;
            } else {
                rowMultiply(entries, y, 1.0 / leadingCoefficient);
                for (int y2 = 0; y2 < y; y2++) {
                    rowAdd(entries, y2, y, -entries[y2][leadingCoefficientPosition]);
                }
            }
        }
        return true;
    }

    private static boolean isRowEchelonForm(double[][] entries) {
        int rows = entries.length;
        boolean rowIsZero = true;
        for (int y = rows - 1; y >= 0; y--) {
            if (!isRowZero(entries, y)) {
                rowIsZero = false;
            } else if (!rowIsZero) {
                return false;
            }
        }
        for (int y = 0; y < rows; y++) {
            int leadingCoefficientColumn = entries[y].length - 1;
            for (int x = 0; x < entries[y].length; x++) {
                if (entries[y][x] != 0) {
                    leadingCoefficientColumn = x;
                    break;
                }
            }
            for (int y2 = y + 1; y2 < rows; y2++) {
                if (entries[y2][leadingCoefficientColumn] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isRowEchelonForm() {
        return isRowEchelonForm(this.entries);
    }

    public MatrixDouble rowEchelonForm() {
        double[][] entries = copyArray2D(this.entries);
        reduceToREF(entries);
        return new MatrixDouble(entries);
    }

    public MatrixDouble reducedRowEchelonForm() {
        double[][] entries = copyArray2D(this.entries);
        if (!isRowEchelonForm(entries)) {
            reduceToREF(entries);
        }
        reduceToRREFFromREF(entries);
        return new MatrixDouble(entries);
    }

    private MatrixDouble removeFirstColumns(int n) {
        double[][] entries = new double[this.entries.length][this.entries[0].length - n];
        for (int y = 0; y < entries.length; y++) {
            System.arraycopy(this.entries[y], n, entries[y], 0, entries[y].length);
        }
        return new MatrixDouble(entries);
    }

    public MatrixDouble inverse() {
        int rows = getRows();
        if (rows != getColumns()) throw new UnsupportedOperationException("Inverse operation performed on non-invertible matrix " + sizeString());
        return appendHorizontal(createIdentity(rows)).reducedRowEchelonForm().removeFirstColumns(rows);
    }

    private static double[][] deleteRowAndColumn(double[][] entries, int row, int column) {
        double[][] newEntries = new double[entries.length - 1][entries[0].length - 1];
        for (int y = 0; y < entries.length; y++) {
            if (y != row) {
                int newY = y > row ? y - 1 : y;
                for (int x = 0; x < entries[y].length; x++) {
                    if (x != column) {
                        int newX = x > column ? x - 1 : x;
                        newEntries[newY][newX] = entries[y][x];
                    }
                }
            }
        }
        return newEntries;
    }

    private static double determinant(double[][] entries) {
        if (entries.length == 2 && entries[0].length == 2) {
            return entries[0][0] * entries[1][1] - entries[1][0] * entries[0][1];
        } else {
            double determinant = 0;
            for (int y = 0; y < entries.length; y++) {
                determinant += (y % 2 == 0 ? 1 : -1) * entries[y][0] * determinant(deleteRowAndColumn(entries, y, 0));
            }
            return determinant;
        }
    }

    public double determinant() {
        if (!isSquare()) throw new UnsupportedOperationException("Cannot calculate determinant from non-square matrix " + sizeString());
        return determinant(this.entries);
    }

    public double getEntry(int row, int column) {
        return this.entries[row][column];
    }

    public int getRows() {
        return this.entries.length;
    }

    public int getColumns() {
        return this.entries.length > 0 ? this.entries[0].length : 0;
    }

    public boolean isSquare() {
        return getRows() == getColumns();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MatrixDouble that = (MatrixDouble) o;
        return Arrays.deepEquals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(entries);
    }

    private String sizeString() {
        return "[" + getRows() + "x" + getColumns() + "]";
    }

    @Override
    public String toString() {
        String newLine = System.lineSeparator();
        StringBuilder s = new StringBuilder("MatrixDouble{ ").append(sizeString()).append(newLine);
        for (double[] row : this.entries) {
            s.append(Arrays.toString(row)).append(newLine);
        }
        return s.append("}").toString();
    }
}
