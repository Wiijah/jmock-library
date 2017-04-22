package org.jmock.internal.perfmodel.network;

import java.util.concurrent.PriorityBlockingQueue;

import static java.util.Comparator.comparingDouble;

public class OrderedQueue extends PriorityBlockingQueue<Customer> implements CappedQueue {
    private int cap;

    public OrderedQueue() {
        super(99999, comparingDouble(Customer::aTime));
        this.cap = Integer.MAX_VALUE;
    }

    public OrderedQueue(int cap) {
        super(cap, comparingDouble(Customer::aTime));
        this.cap = cap;
    }

    public boolean canAccept(Customer c) {
        return size() < cap;
    }

    public boolean add(Customer c) {
        if (canAccept(c)) {
            return super.add(c);
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean offer(Customer c) {
        return canAccept(c) && super.offer(c);
    }
}