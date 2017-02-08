package ic.doc;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.jmock.Expectations;
import org.jmock.integration.junit4.AnotherMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static ic.doc.Statistics.percentile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class AnotherJUnitTest {
    @Rule
    public AnotherMockery context = new AnotherMockery();

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

class Statistics {
    public static double percentile(int i, List<Double> runTimes) {
        return new Percentile().evaluate(ArrayUtils.toPrimitive(runTimes.toArray(new Double[0])), i);
    }
}