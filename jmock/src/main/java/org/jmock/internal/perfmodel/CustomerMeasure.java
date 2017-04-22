package org.jmock.internal.perfmodel;

public class CustomerMeasure extends Measure {
    public CustomerMeasure(Sim sim) {
        super(sim);
    }

    public CustomerMeasure(Sim sim, int n) {
        super(sim, n);
    }

    public void add(double x) {
        for (int i = 1; i <= moments; i++) {
            moment[i] += Math.pow(x, i);
        }
        n++;
    }

    public double mean() {
        return moment[1] / n;
    }

    public double variance() {
        double mean = mean();
        return (moment[2] - n * mean * mean) / (n - 1);
    }
}