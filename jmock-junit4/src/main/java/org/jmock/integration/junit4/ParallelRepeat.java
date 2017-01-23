package org.jmock.integration.junit4;

import org.jmock.internal.AllDeclaredFields;
import org.jmock.internal.ParallelInvocationDispatcher;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class ParallelRepeat extends Statement {
    private final Method method;
    private final Statement next;
    private final int repeat;
    private final Semaphore main = new Semaphore(1);
    private PerformanceMockery mockery = null;
    private final CountDownLatch startSignal = new CountDownLatch(1);

    public ParallelRepeat(FrameworkMethod method, Object target, Statement next) {
        this.method = method.getMethod();
        this.next = next;
        this.repeat = getRepeats(method.getAnnotation(Repeat.class));

        Field contextField = getContext(target);
        if (contextField != null) {
            try {
                this.mockery = (PerformanceMockery) contextField.get(target);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        mockery.setThreadingPolicy(new Synchroniser());
        mockery.setNamingScheme(new UniqueNamingScheme());
        mockery.setInvocationDispatcher(new ParallelInvocationDispatcher());
    }

    @Override
    public void evaluate() throws Throwable {
        System.out.println("Repeats: " + repeat);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    startSignal.await();
                    next.evaluate();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        for (int i = 0; i < repeat; i++) {
            Thread t = new Thread(r);
            mockery.addThread(t);
        }
        mockery.start(startSignal);

        synchronized (mockery.lock) {
            while (mockery.test < repeat) {
                System.out.println("test < " + repeat + ", main calling wait()");
                mockery.lock.wait();
                System.out.println("after wait(), test = " + mockery.test);
            }
        }
        System.out.println("main waking");
        mockery.test = 0;
        mockery.overallResponseTimes(repeat);
        System.out.println("----------");
        mockery.performanceMockeryCleanup();
    }

    private int getRepeats(Repeat annotation) {
        if (annotation == null) {
            return 1;
        }
        return annotation.value();
    }

    private Field getContext(Object target) {
        List<Field> allFields = AllDeclaredFields.in(target.getClass());
        for (Field field : allFields) {
            if (PerformanceMockery.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }
}