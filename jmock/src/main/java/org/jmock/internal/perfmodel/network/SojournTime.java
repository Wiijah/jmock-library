package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.CustomerMeasure;
import org.jmock.internal.perfmodel.Sim;

import java.util.ArrayList;
import java.util.List;

public class SojournTime {
    private final Network network;
    private final Sim sim;
    private final String nodeName;
    private final String measureName;

    private final List<List<CustomerMeasure>> sojournTime = new ArrayList<>();
    private final CustomerMeasure globalSojournTime;

    public SojournTime(Network network, Sim sim, String nodeName, String measureName) {
        this.network = network;
        this.sim = sim;
        this.nodeName = nodeName;
        this.measureName = measureName;

        for (int i = 0; i < Network.MAX_CLASSES; i++) {
            List<CustomerMeasure> list = new ArrayList<>();
            for (int j = 0; j < Network.MAX_PRIORITIES; j++) {
                list.add(new CustomerMeasure(sim));
            }
            sojournTime.add(list);
        }
        this.globalSojournTime = new CustomerMeasure(sim);
    }

    public void registerCompletion(Customer customer) {
        double time = sim.now() - customer.arrivalTime();
        globalSojournTime.add(time);
        sojournTime.get(customer.classType()).get(customer.priority()).add(time);
    }

    public void registerNodeCompletion(Customer customer) {
        double time = sim.now() - customer.nodeArrivalTime();
        globalSojournTime.add(time);
        sojournTime.get(customer.classType()).get(customer.priority()).add(time);
    }

    public void reset() {
        globalSojournTime.reset();
        for (List<CustomerMeasure> list : sojournTime) {
            for (CustomerMeasure cm : list) {
                cm.reset();
            }
        }
    }
}