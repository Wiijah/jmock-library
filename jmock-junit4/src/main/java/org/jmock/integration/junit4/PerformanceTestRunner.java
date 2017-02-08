package org.jmock.integration.junit4;

import org.jmock.internal.AllDeclaredFields;
import org.jmock.internal.ParallelInvocationDispatcher;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.List;

public class PerformanceTestRunner extends BlockJUnit4ClassRunner {
    private final VirtualTimeEngine vte = new VirtualTimeEngine();

    public PerformanceTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Statement methodBlock(FrameworkMethod method) {
        Object test;
        try {
            test = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            return new Fail(e);
        }

        if (method.getAnnotation(Performance.class) != null) {
            Field mockField = null;
            AnotherMockery mockery = null;
            List<Field> allFields = AllDeclaredFields.in(test.getClass());
            for (Field field : allFields) {
                if (AnotherMockery.class.isAssignableFrom(field.getType())) {
                    mockField = field;
                }
            }
            if (mockField != null) {
                try {
                    mockery = (AnotherMockery) mockField.get(test);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (mockery != null) {
                    mockery.setThreadingPolicy(new Synchroniser());
                    mockery.setNamingScheme(new UniqueNamingScheme());
                    mockery.setInvocationDispatcher(new ParallelInvocationDispatcher());
                }
            }
        }

        Statement statement = methodInvoker(method, test);
        statement = possiblyExpectingExceptions(method, test, statement);
        statement = withPotentialTimeout(method, test, statement);
        statement = withBefores(method, test, statement);
        statement = withAfters(method, test, statement);
        statement = withRules(method, test, statement);
        return statement;
    }

    protected Statement withRepeat(FrameworkMethod method, Object target, Statement statement) {
        return new RunConcurrency(method, target, statement);
    }
    /* Not sure why but these are private in BlockJUnit4ClassRunner.
     * Copy and pasted verbatim. */
    private Statement withRules(FrameworkMethod method, Object target,
                                Statement statement) {
        List<TestRule> testRules = getTestRules(target);
        Statement result = statement;
        result = withMethodRules(method, testRules, target, result);
        result = withTestRules(method, testRules, result);

        return result;
    }

    private Statement withMethodRules(FrameworkMethod method, List<TestRule> testRules,
                                      Object target, Statement result) {
        for (org.junit.rules.MethodRule each : getMethodRules(target)) {
            if (!testRules.contains(each)) {
                result = each.apply(result, method, target);
            }
        }
        return result;
    }

    private List<org.junit.rules.MethodRule> getMethodRules(Object target) {
        List<org.junit.rules.MethodRule> r = rules(target);
        return rules(target);
    }

    private Statement withTestRules(FrameworkMethod method, List<TestRule> testRules,
                                    Statement statement) {
        return testRules.isEmpty() ? statement :
            new RunRules(statement, testRules, describeChild(method));
    }
}