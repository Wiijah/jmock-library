package org.jmock.integration.junit4;

import org.jmock.internal.perfmodel.distribution.Exp;
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
    
    public static ISNetwork normalDist(double mean, double sd) {
        // TODO: Make this pls Soon :tm:
        return null;
    }
}