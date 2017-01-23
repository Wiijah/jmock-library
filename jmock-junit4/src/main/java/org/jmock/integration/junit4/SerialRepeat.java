package org.jmock.integration.junit4;

import org.jmock.internal.AllDeclaredFields;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.List;

public class SerialRepeat extends Statement {
    private final Statement next;
    private final int repeat;
    private PerformanceMockery mockery = null;

    public SerialRepeat(FrameworkMethod method, Object target, Statement next) {
        this.next = next;
        this.repeat = getRepeats(method.getAnnotation(Concurrency.class));

        Field contextField = getContext(target);
        if (contextField != null) {
            try {
                this.mockery = (PerformanceMockery) contextField.get(target);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void evaluate() throws Throwable {
        System.out.println("Repeats: " + repeat);
        for (int i = 0; i < repeat; ++i) {
            next.evaluate();
        }
        mockery.overallResponseTimes(repeat);
    }

    private int getRepeats(Concurrency annotation) {
        if (annotation == null) {
            return 1;
        }
        return annotation.threads();
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