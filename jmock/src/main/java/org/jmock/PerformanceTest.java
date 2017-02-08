package org.jmock;

public class PerformanceTest  {
    private int threads;
    private int repeats;

    public PerformanceTest(int numThreads) {
        threads = numThreads;
    }

    public void testMethod() {};

    public int getThreads() {
        return threads;
    }

    public int getRepeats() {
        return repeats;
    }

    public void runTest() {
    }

    protected void setRepeats(int repeats) {
        this.repeats = repeats;
    }
}