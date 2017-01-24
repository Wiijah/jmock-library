package org.jmock.integration.junit4;

import org.jmock.Mockery;
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

public class PerformanceMockery extends Mockery implements MethodRule {
    public static final PerformanceMockery INSTANCE = new PerformanceMockery();

    private final Mockomatic mockomatic = new Mockomatic(this);
    private List<Thread> threads = new ArrayList<Thread>();
    public final Object lock = new Object();
    public int test = 0;

    private Map<String, Object> currentMocks = new HashMap<String, Object>();

    public void addThread(Thread thread) {
        threads.add(thread);
    }

    public void start(CountDownLatch startSignal) {
        for (Thread t : threads) {
            t.start();
        }
        startSignal.countDown();
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
        System.out.println("Clearing up currentMocks");
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
                    test++;
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