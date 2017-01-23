package ic.doc;

import org.jmock.Expectations;
import org.jmock.integration.junit4.PerformanceMockery;
import org.jmock.integration.junit4.PerformanceTestRunner;
import org.jmock.integration.junit4.Concurrency;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PerformanceTestRunner.class)
public class GettingStartedJUnit4Rule {
  @Rule
  public PerformanceMockery context = PerformanceMockery.INSTANCE;
  //public JUnitRuleMockery context = new JUnitRuleMockery();

  @Test
  @Concurrency(threads=50)
  public void oneSubscriberReceivesAMessage() {
    // set up
    final Subscriber subscriber1 = context.perfMock(Subscriber.class);

    Publisher publisher = new Publisher();
    publisher.add(subscriber1);

    final String message = "message";
    final String message2 = "message2";

    // expectations
    context.checking(new Expectations() {{
      oneOf(subscriber1).receive(message);
      responseTime(uniform(10, 100));
      
      exactly(4).of(subscriber1).receive(message2);
      responseTime(uniform(1000, 10000));
    }});

    // execute
    publisher.publish(message);
    
    publisher.publish(message2);
    publisher.publish(message2);
    publisher.publish(message2);
    publisher.publish(message2);
  }

  @Test
  public void testMethod2() {
    // set up
    final Subscriber subscriber = context.perfMock(Subscriber.class);

    Publisher publisher = new Publisher();
    publisher.add(subscriber);

    final String message = "message";

    // expectations
    context.checking(new Expectations() {{
      oneOf(subscriber).receive(message);
      responseTime(uniform(20, 200));
    }});

    // execute
    publisher.publish(message);
  }
}
