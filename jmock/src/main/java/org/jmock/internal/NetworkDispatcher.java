package org.jmock.internal;

import org.jmock.api.Invocation;
import org.jmock.internal.perf.PerformanceModel;
import org.jmock.internal.perf.Sim;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkDispatcher {
    public static final Map<Long, List<Long>> parentThreads = Collections.synchronizedMap(new HashMap<Long, List<Long>>());
    public static final Map<Long, Long> childToParentMap = Collections.synchronizedMap(new HashMap<Long, Long>());
    private final Sim sim;
    private final Semaphore mockerySemaphore;
    private final Map<String, PerformanceModel> models = Collections.synchronizedMap(new HashMap<String, PerformanceModel>());
    private final Map<Long, Semaphore> threadSemaphores = Collections.synchronizedMap(new HashMap<Long, Semaphore>());
    private final AtomicInteger threadsInQuery = new AtomicInteger();
    private final AtomicInteger parentsInQuery = new AtomicInteger();
    private AtomicInteger aliveChildThreads;
    private AtomicInteger aliveParentThreads;
    private boolean debug = false;

    public NetworkDispatcher(Sim sim, Semaphore mockerySemaphore) {
        this.sim = sim;
        this.mockerySemaphore = mockerySemaphore;
    }

    public Long tick() {
        return sim.runOnce();
    }

    public void registerModel(String defaultName, PerformanceModel model) {
        models.put(defaultName, model);
    }

    public void registerThread(long threadId, Semaphore threadSemaphore) {
        threadSemaphores.put(threadId, threadSemaphore);
    }

    public void wake(long threadId) {
        threadSemaphores.get(threadId).release();
    }

    public void setAliveChildThreads(AtomicInteger threads) {
        aliveChildThreads = threads;
    }

    public void setAliveParentThreads(AtomicInteger threads) {
        aliveParentThreads = threads;
    }

    public void query(Invocation invocation) {
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        PerformanceModel model = models.get(invocation.getInvokedObject().toString());
        if (model == null) {
            return;
        }
        // need to know about thread's parent...
        model.query(threadId, invocation);
        // For the case of one A, the response time of A is the last exiting thread
        // For the case of multiple A, the response time of each A is the sum of all mocked calls
        //if (threadId > 1) {
        if (threadName.startsWith("PerfMock")) {
            Semaphore currentThreadSemaphore = threadSemaphores.get(threadId);
            if (currentThreadSemaphore == null) {
                currentThreadSemaphore = threadSemaphores.put(threadId, new Semaphore(0));
            }

            if (parentThreads.containsKey(threadId)) {
                try {
                    int currentParentsInQuery = parentsInQuery.incrementAndGet();
                    if (currentParentsInQuery == aliveParentThreads.get()) {
                        if (debug) {
                            System.out.println("PARENT Thread " + threadId + " in query() going to wake main thread");
                        }
                        mockerySemaphore.release();
                        currentThreadSemaphore.acquire();
                    } else {
                        if (debug) {
                            System.out.println("PARENT Thread " + threadId + " in query() going to sleep");
                        }
                        currentThreadSemaphore.acquire();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                parentsInQuery.decrementAndGet();
            } else {
                try {
                    int currentThreadsInQuery = threadsInQuery.incrementAndGet();
                    if (debug) {
                        System.out.println("CHILD Thread " + threadId + " currentThreadsInQuery = " + currentThreadsInQuery + ", aliveChildThreads = " + aliveChildThreads.get());
                    }
                    if (currentThreadsInQuery == aliveChildThreads.get()) {
                        if (debug) {
                            System.out.println("CHILD Thread " + threadId + " in query() going to wake main thread");
                        }
                        mockerySemaphore.release();
                        currentThreadSemaphore.acquire();
                    } else {
                        if (debug) {
                            System.out.println("CHILD Thread " + threadId + " in query() going to sleep");
                        }
                        currentThreadSemaphore.acquire();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                threadsInQuery.decrementAndGet();
            }
        } else {
            sim.runOnce();
        }
    }

    public void enableDebug() {
        this.debug = true;
    }
}