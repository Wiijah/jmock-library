package org.jmock.internal.perfmodel.distribution;

public class Deterministic implements Distribution {
    private final double value;

    public Deterministic(double v) {
        this.value = v;
    }

    public double sample() {
        return value;
    }
}