package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.Sim;

public class LossNode extends Node {
    public LossNode(Network network, Sim sim) {
        this(network, sim, "Loss node");
    }

    public LossNode(Network network, Sim sim, String nodeName) {
        super(network, sim, nodeName);
    }

    @Override
    protected void accept(Customer customer) {
        network.registerLoss(customer);
    }
}