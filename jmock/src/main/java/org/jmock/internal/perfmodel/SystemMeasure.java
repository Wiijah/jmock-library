package org.jmock.internal.perfmodel;

public class SystemMeasure extends Measure {
    private double current = 0.0;
    private double lastChange = 0.0;

    public SystemMeasure(Sim sim) {
        super(sim);
    }

    public SystemMeasure(Sim sim, int n) {
        super(sim, n);
    }

    @Override
    public void add(double x) {
        for (int i = 1; i <= moments; i++) {
            moment[i] += Math.pow(current, i) * (sim.now() - lastChange);
        }
        current = x;
        lastChange = sim.now();
        n++;
    }

    public double mean() {
        add(current);
        if (lastChange <= resetTime) {
            return current;
        } else {
            return moment[1] / (sim.now() - resetTime);
        }
    }

    public double variance() {
        double mean = mean();
        if (lastChange <= resetTime) {
            return current * current - mean * mean;
        } else {
            return moment[2] / (sim.now() - resetTime) - mean * mean;
        }
    }
}