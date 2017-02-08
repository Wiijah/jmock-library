package org.jmock.integration.junit4;

import org.jmock.Mockery;
import org.jmock.PerformanceTest;
import org.jmock.auto.internal.Mockomatic;
import org.jmock.internal.AllDeclaredFields;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;

public class PerformanceMockery extends Mockery implements MethodRule {
    public static final PerformanceMockery INSTANCE = new PerformanceMockery();

    private final Mockomatic mockomatic = new Mockomatic(this);
    private List<Thread> threads = new ArrayList<Thread>();
    public final Object lock = new Object();
    public int testVal = 0;

    private Map<String, Object> currentMocks = new HashMap<String, Object>();
    private final CountDownLatch startSignal = new CountDownLatch(1);

    public void run(final PerformanceTest test) {
        try {
            Method method = test.getClass().getMethod("testMethod");
            FrameworkMethod frameworkMethod = new FrameworkMethod(method);
            Statement statement = new InvokeMethod(frameworkMethod, test);
            statement = apply(statement, frameworkMethod, test);
            statement.evaluate();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    startSignal.await();
                    test.testMethod();
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
        performanceMockeryCleanup();
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T perfMock(Class<T> typeToMock) {
        String k = typeToMock.getName();
        Object v = currentMocks.get(k);
        if (v != null) {
            return (T) v;
        } else {
            T mock = mock(typeToMock);
            currentMocks.put(k, mock);
            return mock;
        }
    }

    public void performanceMockeryCleanup() {
        threads.clear();
        currentMocks.clear();
        somethingElse();
    }

    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                prepare(target);
                base.evaluate();
                assertIsSatisfied();
                doExtraThings();
                synchronized (lock) {
                    testVal++;
                    lock.notifyAll();
                }
            }

            private void prepare(final Object target) {
                List<Field> allFields = AllDeclaredFields.in(target.getClass());
                assertOnlyOneJMockContextIn(allFields);
                fillInAutoMocks(target, allFields);
            }

            private void assertOnlyOneJMockContextIn(List<Field> allFields) {
                Field contextField = null;
                for (Field field : allFields) {
                    if (PerformanceMockery.class.isAssignableFrom(field.getType())) {
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
}