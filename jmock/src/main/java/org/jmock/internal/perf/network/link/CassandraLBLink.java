package org.jmock.internal.perf.network.link;

import org.jmock.internal.perf.network.request.CassandraCustomer;
import org.jmock.internal.perf.network.Network;
import org.jmock.internal.perf.network.node.CassandraNode;

import java.util.List;

public class CassandraLBLink extends Link<CassandraCustomer> {
    private final List<CassandraNode> networkList;
    private int next = 0;

    public CassandraLBLink(Network network, List<CassandraNode> networkList) {
        super(network);
        this.networkList = networkList;
    }

    @Override
    public void move(CassandraCustomer customer) {
        CassandraNode nextNode = networkList.get(next);
        next = (next + 1) % networkList.size();
        System.out.println(customer.uuid() + " " + customer.requestType() + " sent to " + nextNode.name());
        send(customer, nextNode);
    }
}