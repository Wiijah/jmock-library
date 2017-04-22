package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.Sim;

/**
 *
 */
public class Node {
    protected final Network network;
    protected final Sim sim;
    protected final String name;

    private final int id;
    private final SojournTime waitingTime;

    private int arrivals = 0;
    private Link link = null;

    public Node(Network network, Sim sim, String nodeName) {
        this.network = network;
        this.sim = sim;
        this.name = nodeName;

        this.id = network.add(this);
        this.waitingTime = new SojournTime(network, sim, nodeName, "Waiting time");
    }

    public String name() {
        return name;
    }

    public int id() {
        return id;
    }

    public void link(Link link) {
        link.setSource(this);
        this.link = link;
    }

    protected void enter(Customer customer) {
        arrivals++;
        customer.setLocation(this);
        customer.setNodeArrivalTime(sim.now());
        accept(customer);
    }

    protected void accept(Customer customer) {
        forward(customer);
    }

    protected void forward(Customer customer) {
        waitingTime.registerNodeCompletion(customer);
        link.move(customer);
    }

    public void reset() {
        waitingTime.reset();
    }
}