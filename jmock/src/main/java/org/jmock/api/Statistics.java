package org.jmock.api;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.List;

public class Statistics {
    public static double percentile(int i, List<Double> runTimes) {
        return new Percentile().evaluate(ArrayUtils.toPrimitive(runTimes.toArray(new Double[0])), i);
    }
}