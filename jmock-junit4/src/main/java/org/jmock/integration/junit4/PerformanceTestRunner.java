package org.jmock.integration.junit4;

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

import java.util.List;

public class PerformanceTestRunner extends BlockJUnit4ClassRunner {
    private final VirtualTimeEngine vte = new VirtualTimeEngine();

    public PerformanceTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    private int getRepeats(Repeat annotation) {
        if (annotation == null) {
            return 1;
        }
        return annotation.value();
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        int repeats = getRepeats(method.getAnnotation(Repeat.class));
        if (isIgnored(method)) {
            notifier.fireTestIgnored(description);
        } else {
            Statement statement = methodBlock(method);
            EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
            eachNotifier.fireTestStarted();
            for (int i = 0; i < repeats; ++i) {
                try {
                    statement.evaluate();
                } catch (AssumptionViolatedException e) {
                    // TODO - Need to change this!
                    eachNotifier.addFailedAssumption(e);
                } catch (Throwable e) {
                    // TODO - Need to change this!
                    eachNotifier.addFailure(e);
                }
            }
            eachNotifier.fireTestFinished();
        }
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

        Statement statement = methodInvoker(method, test);
        statement = possiblyExpectingExceptions(method, test, statement);
        statement = withPotentialTimeout(method, test, statement);
        statement = withBefores(method, test, statement);
        statement = withAfters(method, test, statement);
        statement = withRules(method, test, statement);
        statement = withRepeat(method, test, statement);
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
        return rules(target);
    }

    private Statement withTestRules(FrameworkMethod method, List<TestRule> testRules,
                                    Statement statement) {
        return testRules.isEmpty() ? statement :
            new RunRules(statement, testRules, describeChild(method));
    }
}