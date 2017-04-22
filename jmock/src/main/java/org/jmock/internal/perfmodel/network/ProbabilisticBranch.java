package org.jmock.internal.perfmodel.network;

public class ProbabilisticBranch extends Link {
    private DiscreteSampler dist;
    private Node[] nodes;

    public ProbabilisticBranch(Network network, double[] probs, Node[] nodes) {
        super(network);
        this.dist = new DiscreteSampler(probs);
        this.nodes = nodes;
    }

    public void move(Customer customer) {
        int next = dist.next();
        if (next == 0) {
            System.out.println("\nProbabilisticBranch: sending thread " + customer.threadId() + " to Sink!\n");
        } else {
            System.out.println("ProbabilisticBranch: sending thread " + customer.threadId() + " to " + nodes[next].name());
        }
        send(customer, nodes[next]);
    }
}