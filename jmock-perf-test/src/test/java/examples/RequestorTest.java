package examples;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.jmock.integration.junit4.ResponseTimes.exponentialDist;

public class RequestorTest {
    static final long USER_ID = 1111L;
    static final List<Long> FRIEND_IDS = Arrays.asList(2222L, 3333L, 4444L, 5555L);

    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void looksUpDetailsForEachFriend() {
        final SocialGraph socialGraph = context.mock(SocialGraph.class, exponentialDist(0.01));
        final UserDetailsService userDetails = context.mock(UserDetailsService.class, exponentialDist(0.005));

        context.repeat(10, () -> {
            context.expectThreads(2, () -> {
                context.checking(new Expectations() {{
                    exactly(1).of(socialGraph).query(USER_ID);
                    will(returnValue(FRIEND_IDS));
                    exactly(4).of(userDetails).lookup(with(any(Long.class)));
                    will(returnValue(new User()));
                }});

                new ParallelRequestor(socialGraph, userDetails).lookUpFriends(USER_ID);
            });
        });

        //assertThat(context.runtimes(), hasPercentile(80, lessThan(600.0)));
    }
}