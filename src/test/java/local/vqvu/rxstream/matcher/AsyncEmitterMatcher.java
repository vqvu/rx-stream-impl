package local.vqvu.rxstream.matcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import local.vqvu.rxstream.util.StreamItem;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class AsyncEmitterMatcher<E, T> extends TypeSafeMatcher<E> {
    private final List<StreamItem<T>> expected;
    private final List<StreamItem<T>> actual;

    private boolean done;
    private final Object lock;

    public AsyncEmitterMatcher(Class<?> expectedType, List<StreamItem<T>> expected) {
        super(expectedType);

        this.expected = expected;
        this.actual = new ArrayList<>();
        this.done = false;
        this.lock = this;

        if (expected.isEmpty() || expected.get(expected.size() - 1).isValue()) {
            expected.add(StreamItem.end());
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expected);
    }

    @Override
    protected boolean matchesSafely(E item) {
        reset();
        computeActuals(item);
        waitForDone();
        return matches();
    }

    protected abstract void computeActuals(E item);

    private void reset() {
        synchronized (lock) {
            done = false;
            actual.clear();
        }
    }

    private void waitForDone() {
        synchronized (lock) {
            try {
                while (!done) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private boolean matches() {
        if (expected.size() != actual.size())
            return false;

        Iterator<StreamItem<T>> expectedIter = expected.iterator();
        Iterator<StreamItem<T>> actualIter = actual.iterator();

        while (expectedIter.hasNext() && actualIter.hasNext()) {
            StreamItem<T> expectedItem = expectedIter.next();
            StreamItem<T> actualItem = actualIter.next();

            if (expectedItem.isError() && expectedItem.getError() == null) {
                if (actualItem.isError()) {
                    continue;
                } else {
                    return false;
                }
            }

            if (!expectedItem.equals(actualItem)) {
                return false;
            }
        }

        return !expectedIter.hasNext() && !actualIter.hasNext();
    }

    @Override
    protected void describeMismatchSafely(E item, Description mismatchDescription) {
        if (done) {
            mismatchDescription.appendText("was ").appendValue(actual);
        }
    }

    protected void add(StreamItem<T> item) {
        synchronized (lock) {
            actual.add(item);

            if (!item.isValue()) {
                done = true;
                lock.notifyAll();
            }
        }
    }

    protected void addValue(T value) {
        add(StreamItem.value(value));
    }

    protected void addError(Throwable error) {
        add(StreamItem.error(error));
    }

    protected void addEnd() {
        add(StreamItem.end());
    }
}
