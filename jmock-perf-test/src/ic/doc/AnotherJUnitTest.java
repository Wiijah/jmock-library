package ic.doc;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.internal.perfmodel.network.CPUNetwork;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.jmock.api.Statistics.percentile;
import static org.junit.Assert.assertThat;

public class AnotherJUnitTest {
    @Rule
    public PerformanceMockery context = new PerformanceMockery();

    @Test
    public void eachSubscriberReceivesSameMessage() {
        // set up
        //final Subscriber subscriber = context.mock(Subscriber.class, new MM1Network());
        final Subscriber subscriber = context.mock(Subscriber.class, new CPUNetwork(context.sim()));

        context.runInThreads(10, () -> {
            Publisher publisher = new Publisher();
            publisher.add(subscriber);

            final String message = "message";

            // expectations
            context.checking(new Expectations() {{
                oneOf(subscriber).receive(message);
                //responseTime(uniform(100, 155));
            }});

            // execute
            publisher.publish(message);
        });

        assertThat(context.runtimes().size(), is(10));
        //assertThat(percentile(80, context.runtimes()), is(lessThan(120.0)));
        assertThat(context.runtimes(), hasPercentile(80, lessThan(10.0)));
    }

    private Matcher<List<Double>> hasPercentile(int i, Matcher<Double> percentileCheck) {
        return new TypeSafeMatcher<List<Double>>() {
            @Override
            protected boolean matchesSafely(List<Double> doubles) {
                return percentileCheck.matches(percentile(i, doubles));
            }

            @Override
            public void describeTo(Description description) {
                HorizontalHistogram hh = new HorizontalHistogram(context.runtimes());
                hh.print();
                //description.appendText()
            }
        };
    }
}