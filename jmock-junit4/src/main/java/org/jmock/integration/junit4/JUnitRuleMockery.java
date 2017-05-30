package org.jmock.integration.junit4;

import org.jmock.auto.internal.Mockomatic;
import org.jmock.internal.AllDeclaredFields;
import org.jmock.internal.perfmodel.Sim;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * A <code>JUnitRuleMockery</code> is a JUnit Rule that manages JMock expectations
 * and allowances, and asserts that expectations have been met after each test
 * has finished. To use it, add a field to the test class (note that you don't 
 * have to specify <code>@RunWith(JMock.class)</code> any more). For example,  
 * 
 * <pre>public class ATestWithSatisfiedExpectations {
 *  &#64;Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
 *  private final Runnable runnable = context.mock(Runnable.class);
 *     
 *  &#64;Test
 *  public void doesSatisfyExpectations() {
 *    context.checking(new Expectations() {{
 *      oneOf (runnable).run();
 *    }});
 *          
 *    runnable.run();
 *  }
 *}</pre>
 *
 * Note that the Rule field must be declared public and as a <code>JUnitRuleMockery</code>
 * (not a <code>Mockery</code>) for JUnit to recognise it, as it's checked statically.
 * 
 * @author smgf
 */

public class JUnitRuleMockery extends JUnit4Mockery implements MethodRule {
    private final Mockomatic mockomatic = new Mockomatic(this);

    final List<Double> threadResponseTimes = Collections.synchronizedList(new ArrayList<>());
    final Sim sim = new Sim();
    boolean isMultithreadTest = false;

    private void writeHtml() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        ClassLoader loader = getClass().getClassLoader();
        try {
            List<String> lines = Files.readAllLines(Paths.get(loader.getResource("d3.min.js").getFile()));
            Files.write(Paths.get(tmpDir, "d3.min.js"), lines);
            List<String> frontLines = Files.readAllLines(Paths.get(loader.getResource("front.html").getFile()));
            frontLines.add("var data = " + threadResponseTimes + ";");
            List<String> backLines = Files.readAllLines(Paths.get(loader.getResource("back.html").getFile()));
            Files.write(Paths.get(tmpDir, "test.html"), frontLines);
            Files.write(Paths.get(tmpDir, "test.html"), backLines, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Statement apply(final Statement base, FrameworkMethod method, final Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                prepare(target);
                base.evaluate();
                assertIsSatisfied();
                // check performance expectations here
                assertPerformanceIsSatisfied();
                if (threadResponseTimes.isEmpty()) {
                    threadResponseTimes.add(sim.finalThreadResponseTime());
                }
                System.out.println(threadResponseTimes);
                writeHtml();
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
}
