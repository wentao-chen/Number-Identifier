package maths;


public interface DifferentiableFunction extends DoubleFunction {
    DoubleFunction differentiate();

    default double getDerivativeAverage(double x1, double x2) {
        return (evaluate(x2) - evaluate(x1)) / (x2 - x1);
    }
}
