package org.jmock.internal.perfmodel.network;

import org.jmock.api.Invocation;
import org.jmock.internal.perfmodel.Sim;
import org.jmock.internal.perfmodel.distribution.Exp;

public class CPUNetwork extends Network {
    private ProcessorSharingNode psNode;
    private Sink sink;

    public CPUNetwork(Sim sim) {
        super(sim);
        this.psNode = new ProcessorSharingNode(this, sim, "PS Node", new Delay(new Exp(1 / 0.05)));
        this.sink = new Sink(this, sim);
        // TODO change this syntax?
        Link nodeToSink = new Link(this, sink);
        //psNode.link(nodeToSink);

        Node disk1 = new QueueingNode(this, sim, "Disk1", new Delay(new Exp(0.03)), 1, new FIFOQueue());
        Node disk2 = new QueueingNode(this, sim, "Disk2", new Delay(new Exp(0.027)), 1, new FIFOQueue());

        double[] routingProbs = {1.0 / 121.0, 70.0 / 121.0, 50.0 / 121.0};
        ProbabilisticBranch cpuOutputLink = new ProbabilisticBranch(this, routingProbs, new Node[]{sink, disk1, disk2});

        psNode.link(cpuOutputLink);
        disk1.link(new Link(this, psNode));
        disk2.link(new Link(this, psNode));
    }

    public void query(long threadId, Invocation invocation) {
        Customer customer = new Customer(this, sim, Thread.currentThread().getId());
        psNode.enter(customer);
    }
}