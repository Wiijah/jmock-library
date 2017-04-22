package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.Sim;

public class Customer {
    private static int customerId = 0;

    private final Network network;
    private final Sim sim;
    private final long threadId;
    private final int id;
    private Node location;
    private int type;
    private int priority;

    private double arrivalTime;
    private double serviceDemand;
    private double nodeArrivalTime;

    private double aTime;

    public Customer(Network network, Sim sim, long threadId) {
        this(network, sim, threadId, 0);
    }

    public Customer(Network network, Sim sim, long threadId, int type) {
        this(network, sim, threadId, type, 0);
    }

    public Customer(Network network, Sim sim, long threadId, int type, int priority) {
        this.network = network;
        this.sim = sim;
        this.threadId = threadId;
        this.id = customerId++;
        this.location = network.nullNode;
        this.type = type;
        this.priority = priority;
        this.arrivalTime = sim.now();
    }

    @Override
    public String toString() {
        return "Customer " + id + " (class " + type + ", priority " + priority + ")";
    }

    public long threadId() {
        return threadId;
    }

    public int id() {
        return id;
    }

    public Node location() {
        return location;
    }

    public void setLocation(Node location) {
        this.location = location;
    }

    public int classType() {
        return type;
    }

    public void setClassType(int type) {
        this.type = type;
    }

    public int priority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public double arrivalTime() {
        return arrivalTime;
    }

    public double serviceDemand() {
        return serviceDemand;
    }

    public void setServiceDemand(double demand) {
        this.serviceDemand = demand;
    }

    public double nodeArrivalTime() {
        return nodeArrivalTime;
    }

    public void setNodeArrivalTime(double time) {
        nodeArrivalTime = time;
    }

    // TODO new
    public void setaTime(double time) {
        aTime = time;
    }

    public double aTime() {
        return aTime;
    }
}