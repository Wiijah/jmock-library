package ic.doc;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.integration.junit4.PerformanceTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

public class GettingStartedJUnit4Rule {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @RunWith(PerformanceTestRunner.class)
    public class NestedLocalClass {
        public NestedLocalClass() {
            System.out.println("NestedLocalClass()");
        }
        
        @Rule
        public JUnitRuleMockery context2 = new JUnitRuleMockery();

        @Test
        public void aTestMethod() {
            final Subscriber subscriber2 = context2.mock(Subscriber.class);

            Publisher publisher = new Publisher();
            publisher.add(subscriber2);

            final String message = "message";

            // expectations
            context2.checking(new Expectations() {{
                oneOf(subscriber2).receive(message);
            }});

            System.out.println("NestedLocalClass#aTestMethod");

            // execute
            publisher.publish(message);
        }
    }

    @Test
    public void oneSubscriberReceivesAMessage() {
        // set up
        final Subscriber subscriber = context.mock(Subscriber.class);

        //TestClass testClass = new TestClass(NestedLocalClass.class);

        System.out.println("Before");
        Result r = JUnitCore.runClasses(NestedLocalClass.class);
        System.out.println(r.wasSuccessful());
        System.out.println("After");

        Publisher publisher = new Publisher();
        publisher.add(subscriber);

        final String message = "message";

        // expectations
        context.checking(new Expectations() {{
            oneOf(subscriber).receive(message);
        }});

        // execute
        publisher.publish(message);
    }
}