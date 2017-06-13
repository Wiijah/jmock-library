package org.jmock.integration.junit4;

import org.jmock.internal.perfmodel.distribution.*;
import org.jmock.internal.perfmodel.network.Delay;
import org.jmock.internal.perfmodel.network.FixedDelayNetwork;
import org.jmock.internal.perfmodel.network.ISNetwork;

public class ResponseTimes {
    public static FixedDelayNetwork constantDelay(int milliseconds) {
        return new FixedDelayNetwork(PerformanceMockery.INSTANCE.sim(), milliseconds);
    }

    public static ISNetwork exponentialDist(double lambda) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Exp(lambda)));
    }

    public static ISNetwork normalDist(double mean, double stdev) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Normal(mean, stdev)));
    }

    public static ISNetwork paretoDist(double scale, double shape) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Pareto(scale, shape)));
    }

    public static ISNetwork poissonDist(double lambda) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Poisson(lambda)));
    }

    public static ISNetwork uniformDist(double min, double max) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Uniform(min, max)));
    }
}