package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.Sim;

public class NullNode extends Node {
    public NullNode(Network network, Sim sim) {
        super(network, sim, "Null node");
    }
}