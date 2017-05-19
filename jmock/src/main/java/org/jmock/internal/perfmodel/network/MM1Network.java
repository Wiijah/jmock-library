package org.jmock.internal.perfmodel.network;

import org.jmock.api.Invocation;
import org.jmock.internal.perfmodel.Sim;
import org.jmock.internal.perfmodel.distribution.Exp;

public class MM1Network extends Network {
    private final QueueingNode node;
    private final Sink sink;

    public MM1Network(Sim sim) {
        super(sim);
        this.node = new QueueingNode(this, sim, "FCFS", new Delay(new Exp(0.5)));
        this.sink = new Sink(this, sim);
        Link nodeToSink = new Link(this, sink);
        node.link(nodeToSink);
    }

    public void query(long threadId, Invocation invocation) {
        Customer customer = new Customer(this, sim, Thread.currentThread().getId(), invocation);
        node.enter(customer);
    }
}