package org.jmock.test.unit.internal;

import org.jmock.internal.ReturnDefaultValueAction;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.jmock.test.unit.internal.ReturnDefaultValueActionTests.invocationReturning;

/**
 * @author Steve Freeman 2013 http://www.jmock.org
 */
public class ReturnDefaultCollectionTests {
  private final ReturnDefaultValueAction action = new ReturnDefaultValueAction();

  @SuppressWarnings("unchecked")
  @Test public void
  returnsANewInstanceForEachCall() throws Throwable {
    final ArrayList firstInstance = returnedArrayList();
    firstInstance.add(new Object());

    assertThat(returnedArrayList(), is(empty()));
  }

  @Test public void
  returnsNewInstanceOfIterableClasses() throws Throwable {
    returnsInstanceForType(ArrayList.class, ArrayList.class);
    returnsInstanceForType(PriorityQueue.class, PriorityQueue.class);
  }

  @Test public void
  returnsNewInstanceOfMapClasses() throws Throwable {
    returnsInstanceForType(HashMap.class, HashMap.class);
    returnsInstanceForType(Properties.class, Properties.class);
  }

  @Test public void
  returnsNewInstanceConformingToIterableInterface() throws Throwable {
    returnsInstanceForType(List.class, LinkedList.class);
    returnsInstanceForType(Set.class, TreeSet.class);
    returnsInstanceForType(NavigableSet.class, TreeSet.class);
    returnsInstanceForType(SortedSet.class, TreeSet.class);
    returnsInstanceForType(Queue.class, LinkedList.class);
    returnsInstanceForType(Deque.class, LinkedList.class);
  }

  @Test public void
  returnsNewInstanceConformingToMapType() throws Throwable {
    returnsInstanceForType(Map.class, TreeMap.class);
    returnsInstanceForType(SortedMap.class, TreeMap.class);
    returnsInstanceForType(NavigableMap.class, TreeMap.class);
  }

  private void returnsInstanceForType(Class<?> declaredType, Class<?> expectedType) throws Throwable {
    assertThat(
        action.invoke(invocationReturning(declaredType)),
        instanceOf(expectedType));
  }


  private ArrayList returnedArrayList() throws Throwable {
    return (ArrayList) action.invoke(invocationReturning(ArrayList.class));
  }

}
