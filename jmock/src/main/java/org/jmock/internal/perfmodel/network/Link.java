package org.jmock.internal.perfmodel.network;

public class Link {
    private final Network network;
    private Node src;
    private Node dst;

    public Link(Network network) {
        this.network = network;
        this.dst = network.nullNode;
    }

    public Link(Network network, Node dst) {
        this.network = network;
        this.dst = dst;
    }

    public void setSource(Node src) {
        this.src = src;
    }

    protected void send(Customer customer, Node destination) {
        network.send(customer, destination);
    }

    protected void move(Customer customer) {
        send(customer, dst);
    }
}