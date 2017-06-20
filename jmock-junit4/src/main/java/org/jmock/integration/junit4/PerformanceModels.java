package org.jmock.integration.junit4;

import org.jmock.internal.perfmodel.distribution.Distribution;
import org.jmock.internal.perfmodel.network.CappedQueue;
import org.jmock.internal.perfmodel.network.Network;
import org.jmock.internal.perfmodel.network.SingleServiceNetwork;

public class PerformanceModels {
    public static Network singleServer(Distribution serviceTime, CappedQueue queueingDiscipline) {
        return new SingleServiceNetwork(PerformanceMockery.INSTANCE.sim(), serviceTime, queueingDiscipline);
    }
}