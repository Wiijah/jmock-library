package ic.doc;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.internal.perfmodel.distribution.Exp;
import org.jmock.internal.perfmodel.network.Delay;
import org.jmock.internal.perfmodel.network.ISNetwork;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutorTest {
    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void oneACreatesFiveThreadsViaExecutor() {
        final DBService dbService = context.mock(DBService.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final WebService webService = context.mock(WebService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));

        context.repeat(10, () -> {
            context.runInThreads(2, 5, () -> {
                ExecutorService es = Executors.newFixedThreadPool(5);
                Runnable task = () -> {
                    long dbRes = dbService.query();
                    long wsRes = webService.request();
                };

                context.checking(new Expectations() {{
                    exactly(20).of(dbService).query();
                    exactly(20).of(webService).request();
                }});

                for (int i = 0; i < 20; i++) {
                    es.submit(task);
                }

                try {
                    es.shutdown();
                    es.awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.err.print("task interrupted");
                } finally {
                    if (!es.isTerminated()) {
                        System.err.println("cancel unfinished tasks");
                    }
                    es.shutdownNow();
                    System.out.println("shutdown finished");
                }
            });
        });
    }
}