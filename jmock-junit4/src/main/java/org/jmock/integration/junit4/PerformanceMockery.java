package org.jmock.integration.junit4;

import org.jmock.internal.ParallelInvocationDispatcher;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.rules.MethodRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PerformanceMockery extends JUnitRuleMockery implements MethodRule {
    // TODO Tidy up
    public final Object lock = new Object();
    public int testVal = 0;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private List<Thread> threads = new ArrayList<Thread>();
    private Map<String, Object> currentMocks = new HashMap<String, Object>();

    public PerformanceMockery() {
        setThreadingPolicy(new Synchroniser());
        setInvocationDispatcher(new ParallelInvocationDispatcher());
    }

    public void runInThreads(int numThreads, final Runnable testScenario) {
        Runnable r = () -> {
            try {
                startSignal.await();
                testScenario.run();
                assertIsSatisfied();
                doExtraThings();
                synchronized (lock) {
                    testVal++;
                    lock.notifyAll();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(r);
            threads.add(t);
            t.start();
        }
        startSignal.countDown();

        synchronized (lock) {
            while (testVal < numThreads) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        testVal = 0;
        overallResponseTimes(numThreads);
        System.out.println("----------");
        //performanceMockeryCleanup();
    }

    public void performanceMockeryCleanup() {
        threads.clear();
        currentMocks.clear();
        somethingElse();
    }
}