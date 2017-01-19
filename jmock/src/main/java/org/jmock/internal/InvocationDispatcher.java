package org.jmock.internal;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.jmock.api.Expectation;
import org.jmock.api.ExpectationError;
import org.jmock.api.Invocation;

import java.util.*;

public class InvocationDispatcher implements ExpectationCollector, SelfDescribing {
	private List<Expectation> expectations = new ArrayList<Expectation>();
	private List<StateMachine> stateMachines = new ArrayList<StateMachine>();
	private Map<String, Double> responseTimes = new HashMap<String, Double>();
    
    public StateMachine newStateMachine(String name) {
        StateMachine stateMachine = new StateMachine(name);
        stateMachines.add(stateMachine);
        return stateMachine;
    }
    
	public void add(Expectation expectation) {
		expectations.add(expectation);
	}
	
    public void describeTo(Description description) {
        describe(description, expectations);
    }

    public void describeMismatch(Invocation invocation, Description description) {
        describe(description, describedWith(expectations, invocation));
    }

    private Iterable<SelfDescribing> describedWith(List<Expectation> expectations, final Invocation invocation) {
        final Iterator<Expectation> iterator = expectations.iterator();
        return new Iterable<SelfDescribing>() {
            public Iterator<SelfDescribing> iterator() {
                return new Iterator<SelfDescribing>() {
                    public boolean hasNext() { return iterator.hasNext(); }
                    public SelfDescribing next() {
                        return new SelfDescribing() {
                            public void describeTo(Description description) {
                                iterator.next().describeMismatch(invocation, description);
                            }
                        };
                    }
                    public void remove() { iterator.remove(); }
                };
            }
        };
    }

    private void describe(Description description, Iterable<? extends SelfDescribing> selfDescribingExpectations) {
        if (expectations.isEmpty()) {
            description.appendText("no expectations specified: did you...\n"+
                                   " - forget to start an expectation with a cardinality clause?\n" +
                                   " - call a mocked method to specify the parameter of an expectation?");
        }
        else {
            description.appendList("expectations:\n  ", "\n  ", "", selfDescribingExpectations);
            if (!stateMachines.isEmpty()) {
                description.appendList("\nstates:\n  ", "\n  ", "", stateMachines);
            }
        }
    }
    
    public void calculateResponseTimes() {
        for (Expectation expectation : expectations) {
            Double d = ((InvocationExpectation)expectation).getResponseTime();
            String k = expectation.toString();
            Double v = responseTimes.get(k);
            if (v != null) {
                responseTimes.put(k, v + d);
            } else {
                responseTimes.put(k, d);
            }
        }
    }


    public boolean isSatisfied() {
		for (Expectation expectation : expectations) {
		    if (! expectation.isSatisfied()) {
                return false;
            }
        }
        return true;
	}
	
	public Object dispatch(Invocation invocation) throws Throwable {
		for (Expectation expectation : expectations) {
		    if (expectation.matches(invocation)) {
		        return expectation.invoke(invocation);
            }
        }
        
        throw ExpectationError.unexpected("unexpected invocation", invocation);
	}

	public void reset() {
        expectations.clear();
    }

    public void overallResponseTimes(int repeats) {
        for (Map.Entry<String, Double> e : responseTimes.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue() / repeats);
        }
    }

    protected Map<String, Double> getResponseTimes() {
        return responseTimes;
    }
}
