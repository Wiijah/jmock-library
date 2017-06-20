package org.jmock.internal.perfmodel;

import org.jmock.internal.perfmodel.network.NetworkDispatcher;
import org.jmock.internal.perfmodel.network.SimDiaryEmptyException;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import static java.util.Comparator.comparingDouble;

public class Sim {
    private final Queue<CustomerEvent> diary = new PriorityBlockingQueue<>(1000, comparingDouble(Event::invokeTime));
    private final Map<Long, Double> perThreadEntryTime = new HashMap<>();
    private final Map<Long, Double> perThreadExitTime = new HashMap<>();
    private double currentVTime = 0.0;

    public double now() {
        return currentVTime;
    }

    public void schedule(CustomerEvent e) {
        Long parentThreadId = NetworkDispatcher.childToParentMap.get(e.customerThreadId());
        if (parentThreadId == null) {
            parentThreadId = e.customerThreadId();
        }
        synchronized (this) {
            if (!perThreadEntryTime.containsKey(parentThreadId)) {
                perThreadEntryTime.put(parentThreadId, e.customerArrivalTime());
            }
        }
        diary.add(e);
    }

    public void deschedule(CustomerEvent e) {
        Long parentThreadId = NetworkDispatcher.childToParentMap.get(e.customerThreadId());
        if (parentThreadId == null) {
            parentThreadId = e.customerThreadId();
        }
        synchronized (this) {
            if (perThreadEntryTime.containsKey(parentThreadId)) {
                perThreadEntryTime.remove(parentThreadId);
            }
        }
        diary.remove(e);
    }

    public Long runOnce() {
        while (!diary.isEmpty()) {
            CustomerEvent e = diary.poll();
            currentVTime = e.invokeTime();
            boolean stop = e.invoke();
            if (stop) {
                Long customerParentThreadId = NetworkDispatcher.childToParentMap.get(e.customerThreadId());
                if (customerParentThreadId == null) {
                    customerParentThreadId = e.customerThreadId();
                }
                assert (perThreadEntryTime.containsKey(customerParentThreadId));
                perThreadExitTime.put(customerParentThreadId, currentVTime);
                return e.customerThreadId();
            }
        }
        //throw new SimDiaryEmptyException();
        return null;
    }

    // This is called from the outer parent thread always.
    // This can also be called from thread 1 "main", if runConcurrent is NOT used
    public double finalThreadResponseTime() {
        long threadId = Thread.currentThread().getId();
        if (perThreadEntryTime.get(threadId) == null) {
            assert (perThreadEntryTime.size() == 1);
            for (Map.Entry<Long, Double> e : perThreadEntryTime.entrySet()) {
                long thread = e.getKey();
                double entryTime = e.getValue();
                double exitTime = perThreadExitTime.get(thread);
                return exitTime - entryTime;
            }
        }
        return perThreadExitTime.get(threadId) - perThreadEntryTime.get(threadId);
    }

    public void resetCurrentThread() {
        long threadId = Thread.currentThread().getId();
        perThreadEntryTime.remove(threadId);
        perThreadExitTime.remove(threadId);
    }
}