package org.jmock.internal;

import org.hamcrest.Description;
import org.jmock.api.Expectation;
import org.jmock.api.Invocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ParallelInvocationDispatcher extends InvocationDispatcher {
    private static final ThreadLocal<InvocationDispatcher> dispatchers = new ThreadLocal<InvocationDispatcher>() {
        @Override
        protected InvocationDispatcher initialValue() {
            return new InvocationDispatcher();
        }
    };

    private final Map<Long, Double> responseTimes = Collections.synchronizedMap(new HashMap<Long, Double>());

    private InvocationDispatcher myInvocationDispatcher() {
        return dispatchers.get();
    }

    @Override
    public StateMachine newStateMachine(String name) {
        return myInvocationDispatcher().newStateMachine(name);
    }

    @Override
    public void add(Expectation expectation) {
        myInvocationDispatcher().add(expectation);
    }

    @Override
    public void describeTo(Description description) {
        myInvocationDispatcher().describeTo(description);
    }

    @Override
    public void describeMismatch(Invocation invocation, Description description) {
        myInvocationDispatcher().describeMismatch(invocation, description);
    }

    @Override
    public void calculateTotalResponseTime() {
        myInvocationDispatcher().calculateTotalResponseTime();
        Long k = Thread.currentThread().getId();
        Double v = getTotalResponseTime();
        responseTimes.put(k, v);
    }

    @Override
    public double getTotalResponseTime() {
        return myInvocationDispatcher().getTotalResponseTime();
    }

    @Override
    public void overallResponseTimes(int repeats) {
        for (Map.Entry<Long, Double> e : responseTimes.entrySet()) {
            System.out.println("Thread: " + e.getKey() + ", Total response time: " + e.getValue());
        }
    }

    @Override
    public boolean isSatisfied() {
        return myInvocationDispatcher().isSatisfied();
    }

    @Override
    public Object dispatch(Invocation invocation) throws Throwable {
        return myInvocationDispatcher().dispatch(invocation);
    }

    @Override
    public void reset() {
        myInvocationDispatcher().reset();
    }
}