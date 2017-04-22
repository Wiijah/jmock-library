package org.jmock.internal.perfmodel;

public class Resource {
    private final SystemMeasure resourceCount;
    private int maxResources;
    private int availableResources;

    public Resource(Sim sim) {
        this.resourceCount = new SystemMeasure(sim);
        this.maxResources = 1;
        this.availableResources = 1;
    }

    public Resource(Sim sim, int resources) {
        this.resourceCount = new SystemMeasure(sim);
        this.maxResources = resources;
        this.availableResources = resources;
    }

    public void claim() {
        if (availableResources <= 0) {
            throw new ResourceException("Attempting to claim unavailable resource.");
        }
        availableResources--;
        resourceCount.add(maxResources - availableResources);
    }

    public void release() {
        if (availableResources >= maxResources) {
            throw new ResourceException("Attempting to release non-existent resource.");
        }
        availableResources++;
        resourceCount.add(maxResources - availableResources);
    }

    public boolean isAvailable() {
        return availableResources > 0;
    }

    public double utilisation() {
        return resourceCount.mean() / maxResources;
    }

    public void reset() {
        resourceCount.reset();
    }
}