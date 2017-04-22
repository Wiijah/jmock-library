package org.jmock.internal.perfmodel.distribution;

public class ParetoDistribution implements Distribution {
    private final double k;
    private final double alpha;

    public ParetoDistribution() {
        this(1, 1);
    }

    public ParetoDistribution(double k, double alpha) {
        this.k = k;
        this.alpha = alpha;
    }

    public double sample() {
        double n = Math.random();
        return k / Math.pow(n, 1 / alpha);
    }
}