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

public class NoThreadsSpecifiedRequestsExecutorTest {

    static final long USER_ID = 1111L;
    static final List<Long> FRIEND_IDS = Arrays.asList(2222L, 3333L, 4444L, 5555L);

    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void looksUpDetailsForEachFriend() {

        final SocialGraph socialGraph = context.mock(SocialGraph.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final UserDetailsService userDetails = context.mock(UserDetailsService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));

        context.repeat(1000, () -> {

                context.checking(new Expectations() {{
                    exactly(1).of(socialGraph).query(USER_ID); will(returnValue(FRIEND_IDS));
                    exactly(4).of(userDetails).lookup(with(any(Long.class))); will(returnValue(new User()));
                }});

                new Requestor(socialGraph, userDetails).lookUpFriends();
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

            List<Long> friendIds = socialGraph.query(USER_ID);
            List<User> friends = new ArrayList<>();

            for (Long friendId : friendIds) {
                friends.add(userDetailsService.lookup(friendId));
            }
        }
    }

}