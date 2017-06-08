package org.jmock.integration.junit4;

import ic.doc.agent.PerfMockInstrumenter;
import org.jmock.internal.InvocationDispatcher;
import org.jmock.internal.perfmodel.PerformanceModel;
import org.jmock.internal.perfmodel.Sim;
import org.jmock.internal.perfmodel.network.NetworkDispatcher;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMockery extends JUnitRuleMockery implements MethodRule {
    public static PerformanceMockery INSTANCE;

    private final CountDownLatch startSignal = new CountDownLatch(1);
    private CountDownLatch doneSignal;
    private final Semaphore mockerySemaphore = new Semaphore(0);
    // This is usually 0 when not using runInThreads(int, int, Runnable)
    private final AtomicInteger aliveChildThreads = new AtomicInteger();
    private final AtomicInteger aliveParentThreads = new AtomicInteger();
    private final NetworkDispatcher networkDispatcher = new NetworkDispatcher(sim, mockerySemaphore);

    private final Runnable mainThreadRunnable;
    private final Thread mainThread;

    static final Map<Long, List<Long>> parentThreads = Collections.synchronizedMap(new HashMap<>());
    static final Map<Long, Long> childToParentMap = Collections.synchronizedMap(new HashMap<>());

    public PerformanceMockery() {
        PerformanceMockery.INSTANCE = this;
        InvocationDispatcher.setNetworkDispatcher(networkDispatcher);

        this.mainThreadRunnable = () -> {
            try {
                while (aliveParentThreads.get() > 0) {
                    // FIXME Debug message
                    System.out.println("Main thread going to sleep, aliveParentThreads = " + aliveParentThreads.get());
                    mockerySemaphore.acquire();
                    // FIXME Debug message
                    System.out.println("Main thread is awake now, aliveParentThreads = " + aliveParentThreads.get());
                    if (aliveParentThreads.get() > 0) {
                        Long threadToResume = networkDispatcher.tick();
                        if (threadToResume != null) {
                            // FIXME Debug message
                            System.out.println("Main thread decided to wake thread " + threadToResume);
                            networkDispatcher.wake(threadToResume);
                        } else {
                            System.out.println("Sim diary was empty, sleep again...");
                        }
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
        System.out.println("<!> endThreadCallback(): Thread " + Thread.currentThread().getId() + " about to die, going to wake main thread");
        threadResponseTimes.add(sim.finalThreadResponseTime());
        aliveParentThreads.decrementAndGet();
        mockerySemaphore.release();
    }

    private void endInnerThreadCallback() {
        System.out.println("<!> endInnerThreadCallback");
        aliveChildThreads.decrementAndGet();
        doneSignal.countDown();
        mockerySemaphore.release();
    }

    private void endOuterThreadCallback() {
        System.out.println("<!> endOuterThreadCallback()");
        threadResponseTimes.add(sim.finalThreadResponseTime());
        aliveParentThreads.decrementAndGet();
        doneSignal.countDown();
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
        for (int i = 0; i < times; i++) {
            test.run();
            mockerySemaphore.drainPermits();
            // For the case of repeat but not runInThreads
            if (threadResponseTimes.size() == i) {
                threadResponseTimes.add(sim.finalThreadResponseTime());
                sim.resetCurrentThread();
            }
            System.out.println("--------------------------------------------------------------------------------");
        }
    }

    public void runInThreads(int numThreads, final Runnable testScenario) {
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
            // Only for child threads
            if (!parentThreads.containsKey(currentThread.getId())) {
                PerformanceMockery.INSTANCE.endThreadCallback();
            }
        });

        setInvocationDispatcher(new ParallelInvocationDispatcher());
        this.doneSignal = new CountDownLatch(numThreads);
        Runnable r = () -> {
            try {
                startSignal.await();
                testScenario.run();
                assertIsSatisfied();
                endThreadCallback();
                doneSignal.countDown();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        aliveParentThreads.set(numThreads);
        networkDispatcher.setAliveParentThreads(aliveParentThreads);
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

    public void runInThreads(int numThreads, int eachCreates, final Runnable testScenario) {
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
            // Only for child threads
            if (!parentThreads.containsKey(currentThread.getId())) {
                PerformanceMockery.INSTANCE.endInnerThreadCallback();
            }
        });

        PerfMockInstrumenter.setBeforeExecuteCallback(() -> {
            System.out.println("Thread " + Thread.currentThread().getId() + " starting task");
        });

        PerfMockInstrumenter.setAfterExecuteCallback(() -> {
            System.out.println("Thread " + Thread.currentThread().getId() + " finishing task");
        });

        setInvocationDispatcher(new ParallelInvocationDispatcher());
        this.doneSignal = new CountDownLatch(numThreads + (numThreads * eachCreates));
        Runnable r = () -> {
            try {
                startSignal.await();
                testScenario.run();
                assertIsSatisfied();
                endOuterThreadCallback();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        aliveChildThreads.set(numThreads * eachCreates);
        networkDispatcher.setAliveThreads(aliveChildThreads);
        aliveParentThreads.set(numThreads);
        networkDispatcher.setAliveParentThreads(aliveParentThreads);
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

    private void writeHtml(FrameworkMethod method) {
        //String tmpDir = System.getProperty("java.io.tmpdir");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        Path dirPath = Paths.get("target", dtf.format(LocalDateTime.now()));
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Path filePath = Paths.get(dirPath.toString(),
                method.getDeclaringClass().getName() + "-" + method.getName() + ".html");
        ClassLoader loader = getClass().getClassLoader();
        try {
            List<String> lines = Files.readAllLines(Paths.get(loader.getResource("d3.min.js").getFile()));
            Files.write(Paths.get(dirPath.toString(), "d3.min.js"), lines);
            List<String> frontLines = Files.readAllLines(Paths.get(loader.getResource("front.html").getFile()));
            frontLines.add("var data = " + threadResponseTimes + ";");
            List<String> backLines = Files.readAllLines(Paths.get(loader.getResource("back.html").getFile()));
            Files.write(filePath, frontLines);
            Files.write(filePath, backLines, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doExtraStuff(FrameworkMethod method) {
        // For the case of no repeat and no runInThreads
        if (threadResponseTimes.isEmpty()) {
            threadResponseTimes.add(sim.finalThreadResponseTime());
        }
        System.out.println(threadResponseTimes);
        writeHtml(method);
    }
}