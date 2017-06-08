package ic.doc;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.internal.perfmodel.distribution.Exp;
import org.jmock.internal.perfmodel.network.Delay;
import org.jmock.internal.perfmodel.network.ISNetwork;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertThat;


public class ThreadedTest {

    static final long userId = 10001;
    static final List<Long> friends = Arrays.asList(2222L, 3333L, 4444L, 5555L);

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
                    oneOf(dbService).query(userId);
                    oneOf(webService).lookup(friends.get(0));
                }});

                new Controller(dbService, webService).makeRequest();
            });

            // specify some kind of perf expectation here...
        });
    }


    class Controller {
        private DBService dbService;
        private WebService webService;

        Controller(DBService dbService, WebService webService) {
            this.dbService = dbService;
            this.webService = webService;
        }

        void makeRequest() {
            Runnable dbrunnable = () -> {
                List<Long> friends = dbService.query(10001L);
            };
            Thread dbthread = new Thread(dbrunnable);

            Runnable wsrunnable = () -> {
                User wsRes = webService.lookup(friends.get(0));
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
