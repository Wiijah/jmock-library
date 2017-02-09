package ic.doc;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.junit.Rule;
import org.junit.Test;

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
        final Subscriber subscriber = context.mock(Subscriber.class);

        context.runInThreads(10, () -> {
            Publisher publisher = new Publisher();
            publisher.add(subscriber);

            final String message = "message";

            // expectations
            context.checking(new Expectations() {{
                oneOf(subscriber).receive(message);
                responseTime(uniform(100, 155));
            }});

            // execute
            publisher.publish(message);
        });

        assertThat(percentile(80, context.runtimes()), is(lessThan(150.0)));
    }
}