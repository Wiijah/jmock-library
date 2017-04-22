package org.jmock.internal.perfmodel.network;

import org.jmock.api.Invocation;
import org.jmock.internal.perfmodel.PerformanceModel;
import org.jmock.internal.perfmodel.Sim;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkDispatcher {
    private final Sim sim;
    private final Semaphore mockerySemaphore;

    private final Map<String, PerformanceModel> models = new HashMap<>();
    private final Map<Long, Semaphore> threadSemaphores = new HashMap<>();
    private final AtomicInteger threadsInQuery = new AtomicInteger();

    private AtomicInteger aliveThreads;

    public NetworkDispatcher(Sim sim, Semaphore mockerySemaphore) {
        this.sim = sim;
        this.mockerySemaphore = mockerySemaphore;
    }

    public long tick() {
        return sim.runOnce();
    }

    public double finalExitEventTime() {
        return sim.finalExitEventTime();
    }

    public double finalExitEventTime(long threadId) {
        return sim.finalExitEventTime(threadId);
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

    public void setAliveThreads(AtomicInteger threads) {
        aliveThreads = threads;
    }

    // TODO 10-04: This is called by multiple threads, make sure it is safe
    public void query(Invocation invocation) {
        long threadId = Thread.currentThread().getId();
        PerformanceModel model = models.get(invocation.getInvokedObject().toString());

        model.query(threadId, invocation);
        // TODO 12-04: Store a per-thread exit time in Sim?
        // For the case of one A, the response time of A is the last exiting thread
        // For the case of multiple A, the response time of each A is the sum of all mocked calls

        Semaphore currentThreadSemaphore = threadSemaphores.computeIfAbsent(threadId, k -> new Semaphore(0));
        try {
            int currentThreadsInQuery = threadsInQuery.incrementAndGet();
            if (currentThreadsInQuery == aliveThreads.get()) {
                // FIXME Debug message
                System.out.println("Thread " + threadId + " going to wake main thread");
                mockerySemaphore.release();
                currentThreadSemaphore.acquire();
            } else {
                // FIXME Debug message
                System.out.println("Thread " + threadId + " going to sleep on query()");
                currentThreadSemaphore.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        threadsInQuery.decrementAndGet();
    }
}