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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMockery extends JUnitRuleMockery implements MethodRule {
    public static PerformanceMockery INSTANCE;

    private final CountDownLatch startSignal = new CountDownLatch(1);
    private CountDownLatch doneSignal;
    private final Semaphore mockerySemaphore = new Semaphore(0);
    // This is usually 0 when not using runConcurrent(int, int, Runnable)
    private final AtomicInteger aliveChildThreads = new AtomicInteger();
    private final AtomicInteger aliveParentThreads = new AtomicInteger();
    private final NetworkDispatcher networkDispatcher = new NetworkDispatcher(sim, mockerySemaphore);

    private final Runnable mainThreadRunnable;
    private final Thread mainThread;
    private boolean concurrentExpectThreadsInit;
    private boolean concurrentTest;
    private boolean threadedTest;

    static final Map<Long, List<Long>> parentThreads = Collections.synchronizedMap(new HashMap<>());
    static final Map<Long, Long> childToParentMap = Collections.synchronizedMap(new HashMap<>());
    private boolean debug = false;
    private ThreadLocal<AtomicInteger> actualCreatedThreads = ThreadLocal.withInitial(AtomicInteger::new);
    private ThreadLocal<Integer> totalExpectedThreads = new ThreadLocal<>();

    public PerformanceMockery() {
        PerformanceMockery.INSTANCE = this;
        InvocationDispatcher.setNetworkDispatcher(networkDispatcher);

        this.mainThreadRunnable = () -> {
            try {
                while (aliveParentThreads.get() > 0) {
                    if (debug) {
                        System.out.println("Main thread going to sleep, aliveParentThreads = " + aliveParentThreads.get());
                    }
                    mockerySemaphore.acquire();
                    if (debug) {
                        System.out.println("Main thread is awake now, aliveParentThreads = " + aliveParentThreads.get());
                    }
                    if (aliveParentThreads.get() > 0) {
                        Long threadToResume = networkDispatcher.tick();
                        if (threadToResume != null) {
                            if (debug) {
                                System.out.println("Main thread decided to wake thread " + threadToResume);
                            }
                            networkDispatcher.wake(threadToResume);
                        } else {
                            if (debug) {
                                System.out.println("Sim diary was empty, sleep again...");
                            }
                        }
                    }
                }
                if (debug) {
                    System.out.println("Main thread finished");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        this.mainThread = new Thread(mainThreadRunnable);
        this.concurrentExpectThreadsInit = false;
        this.concurrentTest = false;
        this.threadedTest = false;
    }

    public Sim sim() {
        return sim;
    }

    public void enableDebug() {
        this.debug = true;
        networkDispatcher.enableDebug();
    }

    public void endThreadCallback() {
        if (debug) {
            System.out.println("<!> endThreadCallback(): Thread " + Thread.currentThread().getId() + " about to die, going to wake main thread");
        }
        threadResponseTimes.add(sim.finalThreadResponseTime());
        aliveParentThreads.decrementAndGet();
        mockerySemaphore.release();
    }

    private void endInnerThreadCallback() {
        if (debug) {
            System.out.println("<!> Thread " + Thread.currentThread().getId() + " endInnerThreadCallback");
        }
        aliveChildThreads.decrementAndGet();
        doneSignal.countDown();
        mockerySemaphore.release();
    }

    private void endOuterThreadCallback() {
        if (debug) {
            System.out.println("<!> endOuterThreadCallback()");
        }
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
            concurrentTest = false;
            threadedTest = false;
            // For the case of repeat but not runConcurrent
            if (threadResponseTimes.size() == i) {
                threadResponseTimes.add(sim.finalThreadResponseTime());
                sim.resetCurrentThread();
            }
        }
    }

    public void runConcurrent(int numThreads, final Runnable testScenario) {
        if (threadedTest) {
            throw new RuntimeException("test syntax error: runConcurrent must be nested above expectThreads");
        }
        concurrentTest = true;
        PerfMockInstrumenter.setPreCallback((Thread newlyCreatedThread) -> {
            Thread currentParentThread = Thread.currentThread();
            if (currentParentThread.getName().equals("main")) {
                if (debug) {
                    System.out.println(
                            "Outer; parent threadId = " + currentParentThread.getId() + ", name = "
                                    + currentParentThread.getName() + " --> child threadId = "
                                    + newlyCreatedThread.getId() + ", name = "
                                    + newlyCreatedThread.getName());
                }
                assert (!parentThreads.containsKey(newlyCreatedThread.getId()));
                PerformanceMockery.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
                NetworkDispatcher.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
            } else {
                // This should NOT happen
                throw new RuntimeException("unexpected threads created");
            }
        });

        setInvocationDispatcher(new ParallelInvocationDispatcher());
        this.doneSignal = new CountDownLatch(numThreads);
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

    public void expectThreads(int expectedThreads, final Runnable testScenario) {
        threadedTest = true;
        if (concurrentTest) {
            synchronized (this) {
                if (!concurrentExpectThreadsInit) {
                    PerfMockInstrumenter.setPreCallback((Thread newlyCreatedThread) -> {
                        Thread currentParentThread = Thread.currentThread();
                        if (currentParentThread.getName().equals("main")) {
                            if (debug) {
                                System.out.println(
                                        "Outer; parent threadId = " + currentParentThread.getId() + ", name = "
                                                + currentParentThread.getName() + " --> child threadId = "
                                                + newlyCreatedThread.getId() + ", name = "
                                                + newlyCreatedThread.getName());
                            }
                            assert (!parentThreads.containsKey(newlyCreatedThread.getId()));
                            PerformanceMockery.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
                            NetworkDispatcher.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
                        } else {
                            if (debug) {
                                System.out.println(
                                        "Inner; parent threadId = " + currentParentThread.getId() + ", name = "
                                                + currentParentThread.getName() + " --> child threadId = "
                                                + newlyCreatedThread.getId() + ", name = "
                                                + newlyCreatedThread.getName());
                            }
                            int threads = actualCreatedThreads.get().incrementAndGet();
                            if (threads > totalExpectedThreads.get()) {
                                throw new RuntimeException("too many threads created: got " + threads + ", expected " + totalExpectedThreads.get());
                            }
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

                    totalExpectedThreads.set(expectedThreads);

                    int numThreads = aliveParentThreads.get();
                    doneSignal = new CountDownLatch(numThreads + (numThreads * expectedThreads));
                    networkDispatcher.setAliveChildThreads(aliveChildThreads);
                    concurrentExpectThreadsInit = true;
                }
            }
            testScenario.run();
        } else {
            PerfMockInstrumenter.setPreCallback((Thread newlyCreatedThread) -> {
                Thread currentParentThread = Thread.currentThread();
                if (currentParentThread.getName().equals("main")) {
                    if (debug) {
                        System.out.println(
                                "Outer; parent threadId = " + currentParentThread.getId() + ", name = "
                                        + currentParentThread.getName() + " --> child threadId = "
                                        + newlyCreatedThread.getId() + ", name = "
                                        + newlyCreatedThread.getName());
                    }
                    assert (!parentThreads.containsKey(newlyCreatedThread.getId()));
                    PerformanceMockery.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
                    NetworkDispatcher.parentThreads.put(newlyCreatedThread.getId(), new ArrayList<>());
                } else {
                    if (debug) {
                        System.out.println(
                                "Inner; parent threadId = " + currentParentThread.getId() + ", name = "
                                        + currentParentThread.getName() + " --> child threadId = "
                                        + newlyCreatedThread.getId() + ", name = "
                                        + newlyCreatedThread.getName());
                    }
                    int threads = actualCreatedThreads.get().incrementAndGet();
                    if (threads > totalExpectedThreads.get()) {
                        throw new RuntimeException("too many threads created: got " + threads + ", expected " + totalExpectedThreads.get());
                    }
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

            setInvocationDispatcher(new ParallelInvocationDispatcher());
            this.doneSignal = new CountDownLatch(1 + expectedThreads);
            Runnable r = () -> {
                try {
                    startSignal.await();
                    totalExpectedThreads.set(expectedThreads);
                    testScenario.run();
                    assertIsSatisfied();
                    endOuterThreadCallback();
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            };

            aliveChildThreads.set(expectedThreads);
            networkDispatcher.setAliveChildThreads(aliveChildThreads);
            aliveParentThreads.set(1);
            networkDispatcher.setAliveParentThreads(aliveParentThreads);
            for (int i = 0; i < 1; i++) {
                Thread t = new Thread(r, "PerfMockery-" + i);
                long threadId = t.getId();
                Semaphore threadSemaphore = new Semaphore(0);
                networkDispatcher.registerThread(threadId, threadSemaphore);
                t.start();
            }
            startSignal.countDown();
            mainThreadRunnable.run();
            try {
                boolean ret = doneSignal.await(2, TimeUnit.SECONDS);
                if (!ret) {
                    throw new Error("expected " + expectedThreads + " threads to be created");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        // For the case of no repeat and no runConcurrent
        if (threadResponseTimes.isEmpty()) {
            threadResponseTimes.add(sim.finalThreadResponseTime());
        }
        System.out.println(threadResponseTimes);
        writeHtml(method);
    }

    public double runtime() {
        return sim.finalThreadResponseTime();
    }
}