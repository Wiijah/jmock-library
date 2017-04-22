package ic.doc;

import org.jmock.integration.junit4.PerformanceMockery;

public class Thread extends java.lang.Thread {
    public Thread(Runnable target) {
        super(() -> {
            target.run();
            // TODO 10-04: Thread needs to be able to call this...
            // use PerformanceMockery.INSTANCE?
            //assertIsSatisfied();
            PerformanceMockery.INSTANCE.endThreadCallback();
        });
    }
}