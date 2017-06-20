package org.jmock.internal.perfmodel.network;

import org.jmock.api.Invocation;
import org.jmock.internal.perfmodel.Sim;
import org.jmock.internal.perfmodel.distribution.Distribution;

public class SingleServiceNetwork extends Network {
    private final QueueingNode node;
    private final Sink sink;

    public SingleServiceNetwork(Sim sim, Distribution serviceTime, CappedQueue queueingDiscipline) {
        super(sim);
        this.node = new QueueingNode(this, sim, "node", new Delay(serviceTime), 1, queueingDiscipline);
        this.sink = new Sink(this, sim);
        Link nodeToSink = new Link(this, sink);
        node.link(nodeToSink);
    }

    public void query(long threadId, Invocation invocation) {
        Customer customer = new Customer(this, sim, Thread.currentThread().getId(), invocation);
        node.enter(customer);
    }
}