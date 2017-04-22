package org.jmock.integration.junit4;

import org.jmock.internal.ParallelInvocationDispatcher;
import org.jmock.internal.perfmodel.PerformanceModel;
import org.jmock.internal.perfmodel.Sim;
import org.jmock.internal.perfmodel.network.NetworkDispatcher;
import org.junit.rules.MethodRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMockery extends JUnitRuleMockery implements MethodRule {
    public static PerformanceMockery INSTANCE;

    private final Sim sim = new Sim();
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final Semaphore mockerySemaphore = new Semaphore(0);
    private final AtomicInteger aliveThreads = new AtomicInteger();
    private final NetworkDispatcher networkDispatcher = new NetworkDispatcher(sim, mockerySemaphore);

    private final Runnable mainThreadRunnable;
    private final Thread mainThread;

    public PerformanceMockery() {
        PerformanceMockery.INSTANCE = this;
        dispatcher.setNetworkDispatcher(networkDispatcher);

        this.mainThreadRunnable = () -> {
            try {
                while (aliveThreads.get() > 0) {
                    // FIXME Debug message
                    System.out.println("Main thread going to sleep, aliveThreads = " + aliveThreads.get());
                    mockerySemaphore.acquire();
                    // FIXME Debug message
                    System.out.println("Main thread is awake now, aliveThreads = " + aliveThreads.get());
                    if (aliveThreads.get() > 0) {
                        long threadToResume = networkDispatcher.tick();
                        // FIXME Debug message
                        System.out.println("Main thread decided to wake thread " + threadToResume);
                        networkDispatcher.wake(threadToResume);
                    }
                }
                // FIXME Debug message
                System.out.println("Main thread finished");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        this.mainThread = new Thread(mainThreadRunnable);
    }

    public Sim sim() {
        return sim;
    }

    public void endThreadCallback() {
        // FIXME Debug message
        System.out.println("Thread " + Thread.currentThread().getId() + " about to die, going to wake main thread");
        dispatcher.updateResponseTime(Thread.currentThread().getId());
        aliveThreads.decrementAndGet();
        mockerySemaphore.release();
    }

    public <T> T mock(Class<T> typeToMock, PerformanceModel model) {
        String defaultName = namingScheme.defaultNameFor(typeToMock);
        if (mockNames.contains(defaultName)) {
            throw new IllegalArgumentException("a mock with name " + defaultName + " already exists");
        }

        networkDispatcher.registerModel(defaultName, model);
        return mock(typeToMock, defaultName);
    }

    public void runInThreads(int numThreads, final Runnable testScenario) {
        setInvocationDispatcher(new ParallelInvocationDispatcher(networkDispatcher));
        Runnable r = () -> {
            try {
                startSignal.await();
                testScenario.run();
                assertIsSatisfied();
                endThreadCallback();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        aliveThreads.set(numThreads);
        networkDispatcher.setAliveThreads(aliveThreads);
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(r);
            long threadId = t.getId();
            Semaphore threadSemaphore = new Semaphore(0);
            networkDispatcher.registerThread(threadId, threadSemaphore);
            t.start();
        }
        startSignal.countDown();
        mainThreadRunnable.run();
    }

    public void testWillCreateThreads(int numThreads) {
        aliveThreads.set(numThreads);
        networkDispatcher.setAliveThreads(aliveThreads);
        mainThread.start();
        // FIXME 17-04: Missing a call to assertIsSatisfied()?
    }

    public void cleanUp() {
        try {
            mainThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        dispatcher.updateResponseTime();
    }
}