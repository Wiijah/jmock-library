package ic.doc;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.internal.perfmodel.distribution.Exp;
import org.jmock.internal.perfmodel.network.Delay;
import org.jmock.internal.perfmodel.network.ISNetwork;
import org.junit.Rule;
import org.junit.Test;


public class ThreadedTest {
    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void thisIsATest() {
        // each A can create further threads
        final DBService dbService = context.mock(DBService.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final WebService webService = context.mock(WebService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));

        // outer loop, repeats, sequential
        context.repeat(100, () -> {
            // inner loop, runInThreads, number of As, threaded
            context.runInThreads(10, 2, () -> {
                context.checking(new Expectations() {{
                    oneOf(dbService).query();
                    oneOf(webService).request();
                }});

                new A(dbService, webService).run();
            });

            // specify some kind of perf expectation here...
        });
    }

    class A {
        private DBService dbService;
        private WebService webService;

        A(DBService dbService, WebService webService) {
            this.dbService = dbService;
            this.webService = webService;
        }

        void run() {
            Runnable dbrunnable = () -> {
                long dbRes = dbService.query();
            };
            Thread dbthread = new Thread(dbrunnable);

            Runnable wsrunnable = () -> {
                long wsRes = webService.request();
            };
            Thread wsthread = new Thread(wsrunnable);

            dbthread.start();
            wsthread.start();
            try {
                dbthread.join();
                wsthread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}