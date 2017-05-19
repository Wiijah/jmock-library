package org.jmock.internal.perfmodel;

import org.jmock.api.Invocation;
import org.jmock.internal.perfmodel.network.Customer;

public abstract class CustomerEvent extends Event {
    protected final Customer customer;

    public CustomerEvent(Customer customer, double time) {
        super(time);
        this.customer = customer;
    }

    public long customerThreadId() {
        return customer.threadId();
    }

    public Invocation customerInvocation() {
        return customer.invocation();
    }

    public double customerArrivalTime() {
        return customer.arrivalTime();
    }
}