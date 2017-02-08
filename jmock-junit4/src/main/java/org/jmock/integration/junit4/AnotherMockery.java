package org.jmock.integration.junit4;

import org.jmock.PerformanceTest;
import org.jmock.auto.internal.Mockomatic;
import org.jmock.internal.AllDeclaredFields;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;

public class AnotherMockery extends JUnit4Mockery implements MethodRule {
    private final Mockomatic mockomatic = new Mockomatic(this);

    public final Object lock = new Object();
    public int testVal = 0;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private List<Thread> threads = new ArrayList<Thread>();
    private Map<String, Object> currentMocks = new HashMap<String, Object>();

    public Statement apply(final Statement base, FrameworkMethod method, final Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                prepare(target);
                base.evaluate();
                assertIsSatisfied();
                doExtraThings();
            }

            private void prepare(final Object target) {
                List<Field> allFields = AllDeclaredFields.in(target.getClass());
                assertOnlyOneJMockContextIn(allFields);
                fillInAutoMocks(target, allFields);
            }

            private void assertOnlyOneJMockContextIn(List<Field> allFields) {
                Field contextField = null;
                for (Field field : allFields) {
                    if (JUnitRuleMockery.class.isAssignableFrom(field.getType())) {
                        if (null != contextField) {
                            fail("Test class should only have one JUnitRuleMockery field, found "
                                + contextField.getName() + " and " + field.getName());
                        }
                        contextField = field;
                    }
                }
            }

            private void fillInAutoMocks(final Object target, List<Field> allFields) {
                mockomatic.fillIn(target, allFields);
            }
        };
    }

    public void run(final PerformanceTest test) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    startSignal.await();
                    test.testMethod();
                    assertIsSatisfied();
                    doExtraThings();
                    // 
                    synchronized (lock) {
                        testVal++;
                        lock.notifyAll();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        int repeat = test.getThreads();
        for (int i = 0; i < repeat; i++) {
            threads.add(new Thread(r));
        }

        for (Thread t : threads) {
            t.start();
        }
        startSignal.countDown();

        synchronized (lock) {
            while (testVal < repeat) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        testVal = 0;
        overallResponseTimes(repeat);
        System.out.println("----------");
        //performanceMockeryCleanup();
    }

    public void performanceMockeryCleanup() {
        threads.clear();
        currentMocks.clear();
        somethingElse();
    }

    public void runInThreads(int numThreads, final Runnable testScenario) {
        run(new PerformanceTest(numThreads) {
            @Override
            public void testMethod() {
                testScenario.run();
            }
        });
    }

}