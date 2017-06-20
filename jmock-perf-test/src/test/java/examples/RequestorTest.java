package examples;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.internal.perfmodel.network.MM1Network;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.lessThan;
import static org.jmock.integration.junit4.ResponseTimes.exponentialDist;
import static org.jmock.internal.perfmodel.stats.PerfStatistics.hasMean;
import static org.jmock.internal.perfmodel.stats.PerfStatistics.hasPercentile;
import static org.junit.Assert.assertThat;

public class RequestorTest {
    static final long USER_ID = 1111L;
    static final List<Long> FRIEND_IDS = Arrays.asList(2222L, 3333L, 4444L, 5555L);

    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void looksUpDetailsForEachFriend() {
        final SocialGraph socialGraph = context.mock(SocialGraph.class, new MM1Network(PerformanceMockery.INSTANCE.sim()));
        final UserDetailsService userDetails = context.mock(UserDetailsService.class, new MM1Network(PerformanceMockery.INSTANCE.sim()));

        context.repeat(1000, () -> {
            context.checking(new Expectations() {{
                exactly(1).of(socialGraph).query(USER_ID);
                will(returnValue(FRIEND_IDS));
                exactly(4).of(userDetails).lookup(with(any(Long.class)));
                will(returnValue(new User()));
            }});

            new Requestor(socialGraph, userDetails).lookUpFriends(USER_ID);
        });

        assertThat(context.runtimes(), hasMean(lessThan(600.0)));
    }
}