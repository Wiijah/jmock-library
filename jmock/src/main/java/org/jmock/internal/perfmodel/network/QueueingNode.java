package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.CustomerEvent;
import org.jmock.internal.perfmodel.Resource;
import org.jmock.internal.perfmodel.Sim;

/**
 *
 */
public class QueueingNode extends Node {
    protected final Delay serviceTime;
    protected final int maxResources;
    protected final CappedQueue queue;
    protected final Resource resources;

    private int losses = 0;

    public QueueingNode(Network network, Sim sim, String nodeName, Delay delay) {
        this(network, sim, nodeName, delay, 1);
    }

    public QueueingNode(Network network, Sim sim, String nodeName, Delay delay, int maxRes) {
        this(network, sim, nodeName, delay, maxRes, new FIFOQueue());
    }

    public QueueingNode(Network network, Sim sim, String nodeName, Delay delay, int maxRes, CappedQueue queue) {
        super(network, sim, nodeName);
        this.serviceTime = delay;
        this.maxResources = maxRes;
        this.queue = queue;
        this.resources = new Resource(sim, maxRes);
    }

    @Override
    public synchronized void enter(Customer customer) {
        customer.setServiceDemand(serviceTime.sample());
        super.enter(customer);
    }

    @Override
    public void accept(Customer customer) {
        if (resources.isAvailable()) {
            resources.claim();
            scheduleEvent(customer);
        } else {
            if (!queue.offer(customer)) {
                loseCustomer(customer);
            }
        }
    }

    protected void loseCustomer(Customer customer) {
        losses++;
        network.lossNode.enter(customer);
    }

    @Override
    public void forward(Customer customer) {
        super.forward(customer);
        releaseResource();
    }

    protected void scheduleEvent(Customer customer) {
        sim.schedule(new EndServiceEvent(customer, sim.now() + customer.serviceDemand()));
    }

    protected void releaseResource() {
        if (!queue.isEmpty()) {
            Customer next = queue.poll();
            scheduleEvent(next);
        } else {
            resources.release();
        }
    }

    private class EndServiceEvent extends CustomerEvent {
        private EndServiceEvent(Customer customer, double time) {
            super(customer, time);
        }

        public boolean invoke() {
            forward(customer);
            return false;
        }
    }
}