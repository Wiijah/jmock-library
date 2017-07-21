package examples;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.integration.junit4.PerformanceModels;
import org.jmock.integration.junit4.QueueingDisciplines;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.lessThan;
import static org.jmock.integration.junit4.ServiceTimes.exponential;
import static org.jmock.internal.perfmodel.stats.PerfStatistics.hasPercentile;
import static org.junit.Assert.assertThat;

public class RequestorTest7 {
    static final long USER_ID = 1111L;
    static final List<Long> FRIEND_IDS = Arrays.asList(2222L, 3333L, 4444L, 5555L);

    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void looksUpDetailsForEachFriend() {
        final SocialGraph socialGraph = context.mock(SocialGraph.class, exponential(0.005));
        final UserDetailsService userDetails = context.mock(UserDetailsService.class, PerformanceModels.singleServer(QueueingDisciplines.fifo(), exponential(0.005)));

        context.runConcurrent(3, () -> {
            context.checking(new Expectations() {{
                exactly(1).of(socialGraph).query(USER_ID); will(returnValue(FRIEND_IDS));
                exactly(4).of(userDetails).lookup(with(any(Long.class))); will(returnValue(new User()));
            }});

            new ProfileController(socialGraph, userDetails).lookUpFriends(USER_ID);
        });

        // assertThat(context.runtimes(), hasPercentile(80, lessThan(800.0)));
    }
}