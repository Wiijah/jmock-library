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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ParallelRequestsExecutorTest {

    static final long userId = 10001;
    static final List<Long> friends = Arrays.asList(2222L, 3333L, 4444L, 5555L);

    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void oneACreatesFiveThreadsViaExecutor() {

        final SocialGraph socialGraph = context.mock(SocialGraph.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final UserDetailsService userDetails = context.mock(UserDetailsService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));

        context.repeat(10, () -> {
            context.runInThreads(1, 2, () -> {

                context.checking(new Expectations() {{
                    exactly(1).of(socialGraph).query(userId); will(returnValue(friends));
                    exactly(4).of(userDetails).lookup(with(any(Long.class))); will(returnValue(new User()));
                }});

                new Requestor(socialGraph, userDetails).lookUpFriends();
            });
        });
    }

    class Requestor {

        public Requestor(SocialGraph socialGraph, UserDetailsService userDetailsService) {
            this.socialGraph = socialGraph;
            this.userDetailsService = userDetailsService;
        }

        private SocialGraph socialGraph;
        private UserDetailsService userDetailsService;

        public void lookUpFriends() {

            List<Long> friendIds = socialGraph.query(userId);

            ExecutorService es = Executors.newFixedThreadPool(2);

            List<Future<User>> userDetailsRequests = new ArrayList<>();

            System.out.println("friendIds size = " + friendIds.size());
            for (Long friend : friendIds) {
                userDetailsRequests.add(es.submit(() -> userDetailsService.lookup(friend)));
            }
            es.shutdown();

            List<User> friends = new ArrayList<>();

            for (Future<User> userDetailsRequest : userDetailsRequests) {
                try {
                    friends.add(userDetailsRequest.get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}