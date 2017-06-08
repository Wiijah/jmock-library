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
        final SocialGraph socialGraph = context.mock(SocialGraph.class, new ISNetwork(context.sim(), new Delay(new Exp(2))));
        final UserDetailsService userDetailsService = context.mock(UserDetailsService.class, new ISNetwork(context.sim(), new Delay(new Exp(3))));

        // outer loop, repeats, sequential
        context.repeat(100, () -> {
            // inner loop, runInThreads, number of As, threaded
            context.runInThreads(10, 2, () -> {
                context.checking(new Expectations() {{
                    oneOf(socialGraph).query(userId);
                    oneOf(userDetailsService).lookup(friends.get(0));
                }});

                new Controller(socialGraph, userDetailsService).makeRequest();
            });

            // specify some kind of perf expectation here...
        });
    }


    class Controller {
        private SocialGraph socialGraph;
        private UserDetailsService userDetailsService;

        Controller(SocialGraph socialGraph, UserDetailsService userDetailsService) {
            this.socialGraph = socialGraph;
            this.userDetailsService = userDetailsService;
        }

        void makeRequest() {
            Runnable dbrunnable = () -> {
                List<Long> friends = socialGraph.query(10001L);
            };
            Thread dbthread = new Thread(dbrunnable);

            Runnable wsrunnable = () -> {
                User wsRes = userDetailsService.lookup(friends.get(0));
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
