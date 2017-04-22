package org.jmock.internal.perfmodel.network;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LIFOQueue extends AbstractQueue<Customer> implements CappedQueue {
    private final Deque<Customer> q;
    private int cap;

    public LIFOQueue() {
        this.q = new ConcurrentLinkedDeque<>();
        this.cap = Integer.MAX_VALUE;
    }

    public LIFOQueue(int cap) {
        this.q = new LinkedList<>();
        this.cap = cap;
    }

    public boolean canAccept(Customer c) {
        return q.size() < cap;
    }

    public boolean add(Customer c) {
        if (canAccept(c)) {
            q.addFirst(c);
            return true;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean offer(Customer c) {
        return canAccept(c) && q.offerFirst(c);
    }

    public Customer remove() {
        return q.removeFirst();
    }

    public Customer poll() {
        return q.pollFirst();
    }

    public Customer element() {
        return q.getFirst();
    }

    public Customer peek() {
        return q.peekFirst();
    }

    public void clear() {
        q.clear();
    }

    public boolean contains(Object o) {
        return q.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return q.containsAll(c);
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }

    public Iterator<Customer> iterator() {
        return q.iterator();
    }

    @Override
    public Stream<Customer> parallelStream() {
        return q.parallelStream();
    }

    public boolean remove(Object o) {
        return q.remove(o);
    }

    public boolean removeAll(Collection<?> c) {
        return q.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super Customer> filter) {
        return q.removeIf(filter);
    }

    public boolean retainAll(Collection<?> c) {
        return q.retainAll(c);
    }

    public int size() {
        return q.size();
    }

    @Override
    public Spliterator<Customer> spliterator() {
        return q.spliterator();
    }

    @Override
    public Stream<Customer> stream() {
        return q.stream();
    }

    public Object[] toArray() {
        return q.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return q.toArray(a);
    }

    public String toString() {
        return q.toString();
    }

    @Override
    public void forEach(Consumer<? super Customer> action) {
        q.forEach(action);
    }
}