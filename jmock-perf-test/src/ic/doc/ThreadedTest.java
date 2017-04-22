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
    public void oneACreatesTwoThreads() {
        final DBService dbService = context.mock(DBService.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final WebService webService = context.mock(WebService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));
        context.testWillCreateThreads(2);

        Runnable dbrunnable = () -> {
            long dbRes = dbService.query();
        };
        Thread dbthread = new ic.doc.Thread(dbrunnable);

        Runnable wsrunnable = () -> {
            long wsRes = webService.request();
        };
        Thread wsthread = new ic.doc.Thread(wsrunnable);

        context.checking(new Expectations() {{
            oneOf(dbService).query();
            oneOf(webService).request();
        }});

        dbthread.start();
        wsthread.start();
        try {
            dbthread.join();
            wsthread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        context.cleanUp();
    }
}