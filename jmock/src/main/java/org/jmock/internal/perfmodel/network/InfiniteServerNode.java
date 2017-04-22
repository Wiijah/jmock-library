package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.CustomerEvent;
import org.jmock.internal.perfmodel.Sim;

public class InfiniteServerNode extends Node {
    private final Delay serviceTime;

    public InfiniteServerNode(Network network, Sim sim, String nodeName, Delay delay) {
        super(network, sim, nodeName);
        this.serviceTime = delay;
    }

    @Override
    public synchronized void enter(Customer customer) {
        customer.setServiceDemand(serviceTime.sample());
        super.enter(customer);
    }

    @Override
    public void accept(Customer customer) {
        scheduleEvent(customer);
    }

    protected void scheduleEvent(Customer customer) {
        sim.schedule(new EndServiceEvent(customer, sim.now() + customer.serviceDemand()));
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