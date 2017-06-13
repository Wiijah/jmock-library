package org.jmock.integration.junit4;

import org.jmock.internal.perfmodel.distribution.*;
import org.jmock.internal.perfmodel.network.Delay;
import org.jmock.internal.perfmodel.network.FixedDelayNetwork;
import org.jmock.internal.perfmodel.network.ISNetwork;
import org.jmock.internal.perfmodel.network.Network;

public class ResponseTimes {
    public static Network constantDelay(int milliseconds) {
        return new FixedDelayNetwork(PerformanceMockery.INSTANCE.sim(), milliseconds);
    }

    public static Network exponentialDist(double lambda) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Exp(lambda)));
    }

    public static Network normalDist(double mean, double stdev) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Normal(mean, stdev)));
    }

    public static Network paretoDist(double scale, double shape) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Pareto(scale, shape)));
    }

    public static Network poissonDist(double lambda) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Poisson(lambda)));
    }

    public static Network uniformDist(double min, double max) {
        return new ISNetwork(PerformanceMockery.INSTANCE.sim(), new Delay(new Uniform(min, max)));
    }
}