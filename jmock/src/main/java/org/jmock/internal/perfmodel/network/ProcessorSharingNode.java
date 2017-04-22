package org.jmock.internal.perfmodel.network;

import org.jmock.internal.perfmodel.CustomerEvent;
import org.jmock.internal.perfmodel.Sim;

/**
 *
 */
public class ProcessorSharingNode extends QueueingNode {
    private double vtime = 0.0;
    private double timeServiceBegan = 0.0;
    private CustomerEvent nextCompletionEvent;

    public ProcessorSharingNode(Network network, Sim sim, String nodeName, Delay delay) {
        super(network, sim, nodeName, delay, 1, new OrderedQueue());
    }

    public ProcessorSharingNode(Network network, Sim sim, String nodeName, Delay delay, int maxRes) {
        super(network, sim, nodeName, delay, maxRes, new OrderedQueue());
    }

    @Override
    public synchronized void accept(Customer customer) {
        if (queue.canAccept(customer)) {
            double serviceTime = customer.serviceDemand();
            if (resources.isAvailable()) {
                resources.claim();
            } else {
                sim.deschedule(nextCompletionEvent);
                double inc = (sim.now() - timeServiceBegan) / queue.size();
                vtime += inc;
            }
            double t = vtime + serviceTime;
            //<!>System.out.println("Thread " + customer.threadId() + " put on queue with time = " + t);
            customer.setaTime(t);
            queue.offer(customer);
            //queue.offer(new OrderedQueueEntry(customer, vtime + serviceTime));
            serviceNextCustomer();
        } else {
            loseCustomer(customer);
        }
    }

    private void serviceNextCustomer() {
        Customer customer = queue.peek();
        //OrderedQueueEntry e = (OrderedQueueEntry) queue.head();

        double completionTime = (customer.aTime() - vtime) * queue.size();
        //double completionTime = (e.time - vtime) * queue.queueLength();

        timeServiceBegan = sim.now();
        //<!>System.out.println("Scheduling a new Completion event for thread " + customer.threadId() + " with end time = " + (sim.now() + completionTime));
        nextCompletionEvent = new Completion(sim.now() + completionTime, customer);
        sim.schedule(nextCompletionEvent);
    }

    @Override
    public void releaseResource() {
    }

    private class Completion extends CustomerEvent {
        public Completion(double time, Customer customer) {
            super(customer, time);
        }

        public boolean invoke() {
            vtime += (sim.now() - timeServiceBegan) / queue.size();
            //vtime += (sim.now() - timeServiceBegan) / queue.queueLength();
            Customer customer = queue.poll();
            //OrderedQueueEntry e = (OrderedQueueEntry) queue.dequeue();
            if (queue.size() == 0) {
                resources.release();
                vtime = 0.0;
            } else {
                serviceNextCustomer();
            }
            forward(customer);
            return false;
        }
    }
}