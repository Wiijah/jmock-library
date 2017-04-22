package org.jmock.internal.perfmodel;

import org.jmock.internal.perfmodel.network.SimDiaryEmptyException;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import static java.util.Comparator.comparingDouble;

public class Sim {
    private final PriorityQueue<CustomerEvent> diary = new PriorityQueue<>(comparingDouble(Event::invokeTime));
    // For the case of multi-threaded A, one thread = one A
    private final Map<Long, Double> threadLastExitTime = new HashMap<>();
    private double currentVTime = 0.0;
    private double finalExitEventTime = 0.0;
    // TODO 18-04: Store per-thread per-invocation response time?

    public double now() {
        return currentVTime;
    }

    public void schedule(CustomerEvent e) {
        diary.add(e);
    }

    public void deschedule(CustomerEvent e) {
        diary.remove(e);
    }

    public long runOnce() {
        // FIXME Debug message
        System.out.println("Main thread calling Sim#runOnce: start executing scheduled events, diary size = " + diary.size());
        while (!diary.isEmpty()) {
            CustomerEvent e = diary.poll();
            currentVTime = e.invokeTime();
            boolean stop = e.invoke();
            if (stop) {
                finalExitEventTime = e.invokeTime();
                double responseTime = currentVTime - e.customerArrivalTime();
                threadLastExitTime.merge(e.customerThreadId(), responseTime, (a, b) -> a + b);
                return e.customerThreadId();
            }
        }
        throw new SimDiaryEmptyException();
    }

    public double finalExitEventTime() {
        return finalExitEventTime;
    }

    public double finalExitEventTime(long threadId) {
        return threadLastExitTime.get(threadId);
    }
}