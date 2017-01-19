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

    private final Map<String, Double> responseTimes = Collections.synchronizedMap(new HashMap<String, Double>());

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
    public void calculateResponseTimes() {
        myInvocationDispatcher().calculateResponseTimes();
        // FIXME - Is this synchronized block necessary?
        // Answer: probably not lel
        synchronized (responseTimes) {
            Map<String, Double> foo = myInvocationDispatcher().getResponseTimes();
            for (Map.Entry<String, Double> e : foo.entrySet()) {
                String k = e.getKey().replaceAll("-\\d\\d", "");
                Double v = e.getValue();
                Double d = responseTimes.get(k);
                if (d != null) {
                    responseTimes.put(k, d + v);
                } else {
                    responseTimes.put(k, v);
                }
            }
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

    @Override
    public void overallResponseTimes(int repeats) {
        for (Map.Entry<String, Double> e : responseTimes.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue() / repeats);
        }
    }
}