package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.CustomerEvent;
import org.jmock.internal.perfmodel.Sim;

public class Sink extends Node {
    public Sink(Network network, Sim sim) {
        super(network, sim, "Sink");
    }

    public Sink(Network network, Sim sim, String nodeName) {
        super(network, sim, nodeName);
    }

    @Override
    public void accept(Customer customer) {
        sim.schedule(new ExitEvent(customer, sim.now()));
    }

    private class ExitEvent extends CustomerEvent {
        private ExitEvent(Customer customer, double time) {
            super(customer, time);
        }

        public boolean invoke() {
            network.registerCompletion(customer);
            // FIXME Debug message
            System.out.println("Thread " + customer.threadId() + " is leaving the model");
            return true;
        }
    }
}