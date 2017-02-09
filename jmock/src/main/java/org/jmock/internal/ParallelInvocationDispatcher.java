package org.jmock.internal;

import org.hamcrest.Description;
import org.jmock.api.Expectation;
import org.jmock.api.Invocation;

import java.util.*;

public class ParallelInvocationDispatcher extends InvocationDispatcher {
    private static final ThreadLocal<InvocationDispatcher> dispatchers = ThreadLocal.withInitial(InvocationDispatcher::new);
    private final Map<Long, Double> responseTimes = Collections.synchronizedMap(new HashMap<Long, Double>());

    @Override
    public StateMachine newStateMachine(String name) {
        return dispatchers.get().newStateMachine(name);
    }

    @Override
    public void add(Expectation expectation) {
        dispatchers.get().add(expectation);
    }

    @Override
    public void describeTo(Description description) {
        dispatchers.get().describeTo(description);
    }

    @Override
    public void describeMismatch(Invocation invocation, Description description) {
        dispatchers.get().describeMismatch(invocation, description);
    }

    @Override
    public void calculateTotalResponseTime() {
        dispatchers.get().calculateTotalResponseTime();
        Long k = Thread.currentThread().getId();
        Double v = totalResponseTime();
        responseTimes.put(k, v);
    }

    @Override
    public double totalResponseTime() {
        return dispatchers.get().totalResponseTime();
    }

    @Override
    public void overallResponseTimes(int repeats) {
        for (Map.Entry<Long, Double> e : responseTimes.entrySet()) {
            System.out.println("Thread: " + e.getKey() + ", Total response time: " + e.getValue());
        }
    }

    @Override
    public List<Double> getAllRuntimes() {
        System.out.println("ParallelInvocationDispatcher#runtimes " + responseTimes.size());
        return new ArrayList<>(responseTimes.values());
    }

    public void resetResponseTimes() {
        responseTimes.clear();
    }

    @Override
    public boolean isSatisfied() {
        return dispatchers.get().isSatisfied();
    }

    @Override
    public Object dispatch(Invocation invocation) throws Throwable {
        return dispatchers.get().dispatch(invocation);
    }

    @Override
    public void reset() {
        dispatchers.get().reset();
    }
}