package org.jmock.internal.perfmodel.network;

import org.jmock.api.Invocation;
import org.jmock.internal.perfmodel.Sim;

public class ISNetwork extends Network {
    private final Node node;
    private final Sink sink;

    public ISNetwork(Sim sim, Delay delay) {
        super(sim);
        this.node = new InfiniteServerNode(this, sim, "ISNode", delay);
        this.sink = new Sink(this, sim);
        Link nodeToSink = new Link(this, sink);
        node.link(nodeToSink);
    }

    public void query(long threadId, Invocation invocation) {
        Customer customer = new Customer(this, sim, Thread.currentThread().getId(), invocation);
        node.enter(customer);
    }
}