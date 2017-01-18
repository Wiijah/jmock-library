package org.jmock.lib.distribution;

import org.jmock.api.Distribution;

public class UniformDistribution implements Distribution {
    private final double lower;
    private final double upper;

    public UniformDistribution() {
        this(0, 1);
    }

    public UniformDistribution(double lower, double upper) {
        // FIXME: Error if lower >= upper
        this.lower = lower;
        this.upper = upper;
    }

    public double sample() {
        double u = Math.random();
        return u * upper + (1 - u) * lower;
    }
}