package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.PerformanceModel;
import org.jmock.internal.perfmodel.Sim;

import java.util.ArrayList;

public abstract class Network implements PerformanceModel {
    static final int MAX_CLASSES = 16;
    static final int MAX_PRIORITIES = 16;

    protected Sim sim;
    Node nullNode;
    Node lossNode;
    private ArrayList<Node> nodes = new ArrayList<>();
    private int completions = 0;
    private int losses = 0;
    private SojournTime responseTime;
    private SojournTime lossTime;

    public Network(Sim sim) {
        this.sim = sim;
        this.responseTime = new SojournTime(this, sim, "Network", "response time");
        this.lossTime = new SojournTime(this, sim, "Network", "lost customer sojourn time");
        this.nullNode = new NullNode(this, sim);
        this.lossNode = new LossNode(this, sim);
    }

    public int add(Node node) {
        nodes.add(node);
        return nodes.size() - 1;
    }

    // FIXME New added method
    public void send(Customer customer, Node destination) {
        nodes.get(destination.id()).accept(customer);
    }

    public void reset() {
        completions = 0;
        losses = 0;
        responseTime.reset();
        lossTime.reset();
        for (Node node : nodes) {
            node.reset();
        }
    }

    // called by a Sink!
    public void registerCompletion(Customer customer) {
        completions++;
        responseTime.registerCompletion(customer);
    }

    // called by any node with a FULL queue!
    public void registerLoss(Customer customer) {
        losses++;
        lossTime.registerCompletion(customer);
    }
}