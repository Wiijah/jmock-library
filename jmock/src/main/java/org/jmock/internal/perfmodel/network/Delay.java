package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.distribution.Distribution;

public class Delay {
    private final Distribution distribution;

    public Delay(Distribution distribution) {
        this.distribution = distribution;
    }

    protected double sample() {
        return distribution.sample();
    }
}