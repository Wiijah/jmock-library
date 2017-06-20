package org.jmock.integration.junit4;

import org.jmock.internal.perfmodel.distribution.Deterministic;
import org.jmock.internal.perfmodel.distribution.Distribution;
import org.jmock.internal.perfmodel.distribution.Exp;

public class ServiceTimes {
    public static Distribution exponential(double lambda) {
        return new Exp(lambda);
    }

    public static Distribution deterministic(double value) {
        return new Deterministic(value);
    }
}