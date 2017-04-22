package org.jmock.internal.perfmodel.network;

public class Earth extends Link {
    public Earth(Network network) {
        super(network, network.nullNode);
    }
}