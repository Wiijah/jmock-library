package org.jmock.internal;

import org.hamcrest.Description;
import org.jmock.api.Expectation;
import org.jmock.api.Invocation;
import org.jmock.internal.perfmodel.network.NetworkDispatcher;

import javax.management.RuntimeErrorException;
import java.util.*;

public class ParallelInvocationDispatcher extends InvocationDispatcher {
    private final NetworkDispatcher networkDispatcher;
    private final ThreadLocal<InvocationDispatcher> dispatchers;
    private final Map<Long, Double> threadResponseTimes = Collections.synchronizedMap(new HashMap<Long, Double>());

    public ParallelInvocationDispatcher(NetworkDispatcher networkDispatcher) {
        this.networkDispatcher = networkDispatcher;
        this.dispatchers = ThreadLocal.withInitial(() -> {
            InvocationDispatcher dispatcher = new InvocationDispatcher();
            dispatcher.setNetworkDispatcher(this.networkDispatcher);
            return dispatcher;
        });
    }

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
    public void updateResponseTime() {
        throw new RuntimeException("NO");
    }

    @Override
    public void updateResponseTime(long threadId) {
        dispatchers.get().updateResponseTime(threadId);
        System.out.println("<!> Thread " + threadId + " " + dispatchers.get().totalResponseTime());
    }

    @Override
    public double totalResponseTime() {
        return dispatchers.get().totalResponseTime();
    }

    @Override
    public List<Double> getAllRuntimes() {
        return new ArrayList<>(threadResponseTimes.values());
    }

    @Override
    public boolean isSatisfied() {
        return dispatchers.get().isSatisfied();
    }

    @Override
    public Object dispatch(Invocation invocation) throws Throwable {
        // multiple A calling one or more Bs
        Object ret = dispatchers.get().dispatch(invocation);
        Long k = Thread.currentThread().getId();
        Double v = totalResponseTime();
        threadResponseTimes.put(k, v);
        return ret;
    }
}