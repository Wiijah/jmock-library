package org.jmock.integration.junit4;

import org.jmock.internal.perfmodel.network.FixedDelayNetwork;

public class Delays {
    public static FixedDelayNetwork constantDelay(int milliseconds) {
        return new FixedDelayNetwork(PerformanceMockery.INSTANCE.sim(), milliseconds);
    }
}