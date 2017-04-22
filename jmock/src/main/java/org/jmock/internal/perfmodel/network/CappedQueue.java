package org.jmock.internal.perfmodel.network;

import java.util.Queue;

public interface CappedQueue extends Queue<Customer> {
    boolean canAccept(Customer c);
}