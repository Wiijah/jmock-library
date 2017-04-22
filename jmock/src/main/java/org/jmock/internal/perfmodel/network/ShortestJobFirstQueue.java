package org.jmock.internal.perfmodel.network;

import java.util.PriorityQueue;
import java.util.Queue;

import static java.util.Comparator.comparingDouble;

public class ShortestJobFirstQueue extends PriorityQueue<Customer> implements Queue<Customer> {
    public ShortestJobFirstQueue() {
        super(comparingDouble(Customer::serviceDemand));
    }
}