package ic.doc;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.internal.perfmodel.distribution.Exp;
import org.jmock.internal.perfmodel.network.Delay;
import org.jmock.internal.perfmodel.network.ISNetwork;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class FooTest {
    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void manyACallsTwoMockedObjects() {
        final DBService dbService = context.mock(DBService.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final WebService webService = context.mock(WebService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));

        context.runInThreads(10, () -> {
            context.checking(new Expectations() {{
                oneOf(dbService).query(1001L);
                oneOf(webService).lookup(with(any(Long.class)));
            }});

            List<Long> friends = dbService.query(1001L);
            User user = webService.lookup(friends.get(0));
        });
    }
}