package org.jmock.integration.junit4;

import ic.doc.agent.PerfMockInstrumenter;
import org.jmock.internal.InvocationDispatcher;
import org.jmock.internal.perfmodel.PerformanceModel;
import org.jmock.internal.perfmodel.Sim;
import org.jmock.internal.perfmodel.network.NetworkDispatcher;
import org.junit.rules.MethodRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMockery extends JUnitRuleMockery implements MethodRule {
    public static PerformanceMockery INSTANCE;

    private final Sim sim = new Sim();
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private CountDownLatch doneSignal;
    private final Semaphore mockerySemaphore = new Semaphore(0);
    private final AtomicInteger aliveThreads = new AtomicInteger();
    private final NetworkDispatcher networkDispatcher = new NetworkDispatcher(sim, mockerySemaphore);

    private final Runnable mainThreadRunnable;
    private final Thread mainThread;
    private final List<Double> threadResponseTimes = Collections.synchronizedList(new ArrayList<>());

    static final Map<Long, List<Long>> parentThreads = Collections.synchronizedMap(new HashMap<>());
    static final Map<Long, Long> childToParentMap = Collections.synchronizedMap(new HashMap<>());

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
        //dispatcher.updateResponseTime(Thread.currentThread().getId());
        aliveThreads.decrementAndGet();
        mockerySemaphore.release();
    }

    private void endOuterThreadCallback() {
        System.out.println("<!> endOuterThreadCallback");
        threadResponseTimes.add(sim.finalThreadResponseTime());
    }

    private void writeHtml() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        ClassLoader loader = getClass().getClassLoader();
        try {
            List<String> lines = Files.readAllLines(Paths.get(loader.getResource("d3.min.js").getFile()));
            Files.write(Paths.get(tmpDir, "d3.min.js"), lines);
            List<String> frontLines = Files.readAllLines(Paths.get(loader.getResource("front.html").getFile()));
            frontLines.add("var data = " + threadResponseTimes + ";");
            List<String> backLines = Files.readAllLines(Paths.get(loader.getResource("back.html").getFile()));
            Files.write(Paths.get(tmpDir, "test.html"), frontLines);
            Files.write(Paths.get(tmpDir, "test.html"), backLines, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        for (int i = 0; i < times; i++) {
            test.run();
            mockerySemaphore.drainPermits();
        }
        System.out.println(threadResponseTimes);
        writeHtml();
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
        this.doneSignal = new CountDownLatch(numThreads);
        Runnable r = () -> {
            try {
                startSignal.await();
                testScenario.run();
                assertIsSatisfied();
                endOuterThreadCallback();
                doneSignal.countDown();
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
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}