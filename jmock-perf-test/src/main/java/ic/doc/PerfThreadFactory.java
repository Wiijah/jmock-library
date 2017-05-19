package ic.doc;

import org.jmock.integration.junit4.PerformanceMockery;

import java.util.concurrent.ThreadFactory;

public class PerfThreadFactory implements ThreadFactory {
    // performance mockery instance

    public Thread newThread(Runnable target) {
        Thread t = new Thread(() -> {
            target.run();
            PerformanceMockery.INSTANCE.endThreadCallback();
        });
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}