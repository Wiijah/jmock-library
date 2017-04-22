package org.jmock.internal.perfmodel;

public abstract class Measure {
    private static final int MAX_MEASURES = 10;

    protected final Sim sim;
    protected final int moments;
    protected int n;
    protected double moment[] = new double[100];
    protected double resetTime = 0.0;

    public Measure(Sim sim) {
        this.sim = sim;
        this.moments = 2;
    }

    public Measure(Sim sim, int moments) {
        this.sim = sim;
        if (moments > MAX_MEASURES) {
            this.moments = MAX_MEASURES;
        } else if (moments < 2) {
            this.moments = 2;
        } else {
            this.moments = moments;
        }
    }

    public abstract void add(double x);

    public abstract double mean();

    public abstract double variance();

    public int count() {
        return n;
    }

    public void reset() {
        resetTime = sim.now();
        n = 0;
        for (int i = 1; i <= moments; i++) {
            moment[i] = 0.0;
        }
    }
}