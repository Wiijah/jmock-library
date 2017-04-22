package org.jmock.internal.perfmodel;

public abstract class Event {
    protected final double invokeTime;

    public Event(double time) {
        this.invokeTime = time;
    }

    public double invokeTime() {
        return invokeTime;
    }

    public abstract boolean invoke();
}