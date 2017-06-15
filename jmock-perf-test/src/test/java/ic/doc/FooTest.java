package ic.doc;

import examples.SocialGraph;
import examples.User;
import examples.UserDetailsService;
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
        final SocialGraph socialGraph = context.mock(SocialGraph.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final UserDetailsService userDetailsService = context.mock(UserDetailsService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));

        context.runConcurrent(10, () -> {
            context.checking(new Expectations() {{
                oneOf(socialGraph).query(1001L);
                oneOf(userDetailsService).lookup(with(any(Long.class)));
            }});

            List<Long> friends = socialGraph.query(1001L);
            User user = userDetailsService.lookup(friends.get(0));
        });
    }
}