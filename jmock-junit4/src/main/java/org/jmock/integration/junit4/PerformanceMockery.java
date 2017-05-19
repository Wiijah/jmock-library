package org.jmock.integration.junit4;

import ic.doc.agent.PerfMockInstrumenter;
import org.jmock.internal.InvocationDispatcher;
import org.jmock.internal.perfmodel.PerformanceModel;
import org.jmock.internal.perfmodel.Sim;
import org.jmock.internal.perfmodel.network.NetworkDispatcher;
import org.junit.rules.MethodRule;

import java.util.*;
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

    static final Map<Long, List<Long>> parentThreads = Collections.synchronizedMap(new HashMap<>());
    static final Map<Long, Long> childToParentMap = Collections.synchronizedMap(new HashMap<>());
    static AtomicInteger count = new AtomicInteger(0);

    static {
        PerfMockInstrumenter.setPreCallback((Thread newlyCreatedThread) -> {
            Thread currentParentThread = Thread.currentThread();
            if (currentParentThread.getName().equals("main")) {
                System.out.println(
                        "Outer; parent threadId = " + currentParentThread.getId() + ", name = "
                                + currentParentThread.getName() + " --> child threadId = "
                                + newlyCreatedThread.getId() + ", name = "
                                + newlyCreatedThread.getName());
                assert (!parentThreads.containsKey(newlyCreatedThread.getId()));
                PerformanceMockery.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
                NetworkDispatcher.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
            } else {
                System.out.println(
                        "Inner; parent threadId = " + currentParentThread.getId() + ", name = "
                                + currentParentThread.getName() + " --> child threadId = "
                                + newlyCreatedThread.getId() + ", name = "
                                + newlyCreatedThread.getName());
                PerformanceMockery.parentThreads.computeIfAbsent(currentParentThread.getId(), k -> new ArrayList<>())
                        .add(newlyCreatedThread.getId());
                NetworkDispatcher.parentThreads.computeIfAbsent(currentParentThread.getId(), k -> new ArrayList<>())
                        .add(newlyCreatedThread.getId());
                PerformanceMockery.childToParentMap.put(newlyCreatedThread.getId(), currentParentThread.getId());
                NetworkDispatcher.childToParentMap.put(newlyCreatedThread.getId(), currentParentThread.getId());
            }
        });

        PerfMockInstrumenter.setPostCallback((Thread currentThread) -> {
            if (!parentThreads.containsKey(currentThread.getId())) {
                PerformanceMockery.INSTANCE.endThreadCallback();
            }
        });

        PerfMockInstrumenter.setBeforeExecuteCallback(() -> {
            System.out.println("<*> Task starting on thread " + Thread.currentThread().getId());
        });

        PerfMockInstrumenter.setAfterExecuteCallback(() -> {
            // FIXME 18-05: Need per-task response time
            System.out.println("<!> Task ending on thread " + Thread.currentThread().getId());
            count.incrementAndGet();
        });
    }

    public PerformanceMockery() {
        PerformanceMockery.INSTANCE = this;
        InvocationDispatcher.setNetworkDispatcher(networkDispatcher);

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
                sim.testMethod();
                System.out.println(count.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        this.mainThread = new Thread(mainThreadRunnable);
    }

    public Sim sim() {
        return sim;
    }

    // TODO 01-05: Need two different types of end-callback, for A and X
    public void endThreadCallback() {
        // FIXME Debug message
        System.out.println("Thread " + Thread.currentThread().getId() + " about to die, going to wake main thread");
        //dispatcher.updateResponseTime(Thread.currentThread().getId());
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

    public void repeat(int times, final Runnable test) {
        test.run();
    }

    public void runInThreads(int numThreads, final Runnable testScenario) {
        setInvocationDispatcher(new ParallelInvocationDispatcher());
        Runnable r = () -> {
            try {
                startSignal.await();
                testScenario.run();
                assertIsSatisfied();
                //endThreadCallback();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        aliveThreads.set(numThreads);
        networkDispatcher.setAliveThreads(aliveThreads);
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(r, "PerfMockery-" + i);
            long threadId = t.getId();
            Semaphore threadSemaphore = new Semaphore(0);
            networkDispatcher.registerThread(threadId, threadSemaphore);
            t.start();
        }
        startSignal.countDown();
        mainThreadRunnable.run();
    }

    public void runInThreads(int numThreads, int eachCreates, final Runnable testScenario) {
        setInvocationDispatcher(new ParallelInvocationDispatcher());
        Runnable r = () -> {
            try {
                startSignal.await();
                testScenario.run();
                assertIsSatisfied();
                //endThreadCallback();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        aliveThreads.set(numThreads * eachCreates);
        networkDispatcher.setAliveThreads(aliveThreads);
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(r, "PerfMockery-" + i);
            long threadId = t.getId();
            Semaphore threadSemaphore = new Semaphore(0);
            networkDispatcher.registerThread(threadId, threadSemaphore);
            t.start();
        }
        startSignal.countDown();
        mainThreadRunnable.run();
    }

    public void testWillCreateThreads(int numThreads) {
        int currentAliveThreads = aliveThreads.get();
        if (currentAliveThreads == 0) {
            aliveThreads.set(numThreads);
            networkDispatcher.setAliveThreads(aliveThreads);
        } else {
            System.out.println("<!> 2nd branch in testWillCreateThreads lol");
            // already set by outer runInThreads
            aliveThreads.set(currentAliveThreads * numThreads);
        }
        mainThread.start();
        // FIXME 17-04: Missing a call to assertIsSatisfied()?
    }
}