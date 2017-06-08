package org.jmock.internal.perfmodel.stats;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Comparator;
import java.util.List;

public class PerfStatistics {
    public static Matcher<List<Double>> hasPercentile(int i, Matcher<Double> percentileCheck) {
        return new TypeSafeMatcher<List<Double>>() {
            @Override
            protected boolean matchesSafely(List<Double> doubles) {
                System.out.println(percentile(i, doubles));
                return percentileCheck.matches(percentile(i, doubles));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("percentile " + i + " to be " + percentileCheck);
            }

            @Override
            protected void describeMismatchSafely(List<Double> doubles, Description description) {
                description.appendValue(percentile(i, doubles));
            }
        };
    }

    public static Matcher<List<Double>> hasMean(Matcher<Double> meanCheck) {
        return new TypeSafeMatcher<List<Double>>() {
            @Override
            protected boolean matchesSafely(List<Double> doubles) {
                return meanCheck.matches(mean(doubles));
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(meanCheck);
            }

            @Override
            protected void describeMismatchSafely(List<Double> doubles, Description description) {
                description.appendValue(mean(doubles));
            }
        };
    }

    public static Matcher<List<Double>> hasMedian(Matcher<Double> medianCheck) {
        return new TypeSafeMatcher<List<Double>>() {
            @Override
            protected boolean matchesSafely(List<Double> doubles) {
                return medianCheck.matches(median(doubles));
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(medianCheck);
            }

            @Override
            protected void describeMismatchSafely(List<Double> doubles, Description description) {
                description.appendValue(median(doubles));
            }
        };
    }

    private static double percentile(int i, List<Double> runTimes) {
        return new Percentile().evaluate(ArrayUtils.toPrimitive(runTimes.toArray(new Double[0])), i);
    }

    private static double mean(List<Double> runTimes) {
        Double sum = 0.0;
        for (Double d : runTimes) {
            sum += d;
        }
        return sum / runTimes.size();
    }

    private static double median(List<Double> runTimes) {
        runTimes.sort(Comparator.naturalOrder());
        if (runTimes.size() % 2 == 0) {
            int mid = runTimes.size() / 2;
            return (runTimes.get(mid) + runTimes.get(mid - 1)) / 2;
        } else {
            return runTimes.get(runTimes.size() / 2);
        }
    }
}