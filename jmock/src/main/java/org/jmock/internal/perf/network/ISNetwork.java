package org.jmock.internal.perf.network;

import org.jmock.api.Invocation;
import org.jmock.internal.perf.Delay;
import org.jmock.internal.perf.Sim;
import org.jmock.internal.perf.network.link.Link;
import org.jmock.internal.perf.network.node.InfiniteServerNode;
import org.jmock.internal.perf.network.node.Node;
import org.jmock.internal.perf.network.node.Sink;
import org.jmock.internal.perf.network.request.Customer;

public class ISNetwork extends Network {
    private final Node<Customer> node;
    private final Sink<Customer> sink;

    public ISNetwork(Sim sim, Delay delay) {
        super(sim);
        this.node = new InfiniteServerNode<>(this, sim, "ISNode", delay);
        this.sink = new Sink<>(this, sim);
        Link<Customer> nodeToSink = new Link<>(this, sink);
        node.link(nodeToSink);
    }

    public void query(long threadId, Invocation invocation) {
        Customer customer = new Customer(this, sim, Thread.currentThread().getId(), invocation);
        node.enter(customer);
    }
}