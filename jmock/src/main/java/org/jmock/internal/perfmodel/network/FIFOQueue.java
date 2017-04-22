package org.jmock.internal.perfmodel.network;

import java.util.concurrent.ConcurrentLinkedQueue;

public class FIFOQueue extends ConcurrentLinkedQueue<Customer> implements CappedQueue {
    private int cap;

    public FIFOQueue() {
        this.cap = Integer.MAX_VALUE;
    }

    public FIFOQueue(int cap) {
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