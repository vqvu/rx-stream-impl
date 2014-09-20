package local.vqvu.rxstream.matcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import local.vqvu.rxstream.util.StreamToken;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class AsyncEmitterMatcher<E, T> extends TypeSafeMatcher<E> {
    private final List<StreamToken<T>> expected;
    private final List<StreamToken<T>> actual;

    private boolean done;
    private final Object lock;

    public AsyncEmitterMatcher(Class<?> expectedType, List<StreamToken<T>> expected) {
        super(expectedType);

        this.expected = expected;
        this.actual = new ArrayList<>();
        this.done = false;
        this.lock = this;

        if (expected.isEmpty() || expected.get(expected.size() - 1).isValue()) {
            expected.add(StreamToken.end());
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expected);
    }

    @Override
    protected boolean matchesSafely(E token) {
        reset();
        computeActuals(token);
        waitForDone();
        return matches();
    }

    protected abstract void computeActuals(E token);

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

        Iterator<StreamToken<T>> expectedIter = expected.iterator();
        Iterator<StreamToken<T>> actualIter = actual.iterator();

        while (expectedIter.hasNext() && actualIter.hasNext()) {
            StreamToken<T> expectedToken = expectedIter.next();
            StreamToken<T> actualToken = actualIter.next();

            if (expectedToken.isError() && actualToken.isError()) {
                return true;
            }

            if (!expectedToken.equals(actualToken)) {
                return false;
            }
        }

        return !expectedIter.hasNext() && !actualIter.hasNext();
    }

    @Override
    protected void describeMismatchSafely(E token, Description mismatchDescription) {
        if (done) {
            mismatchDescription.appendText("was ").appendValue(actual);
        }
    }

    protected void add(StreamToken<T> token) {
        synchronized (lock) {
            actual.add(token);

            if (!token.isValue()) {
                done = true;
                lock.notifyAll();
            }
        }
    }

    protected void addValue(T value) {
        add(StreamToken.value(value));
    }

    protected void addError(Throwable error) {
        add(StreamToken.error(error));
    }

    protected void addEnd() {
        add(StreamToken.end());
    }
}
