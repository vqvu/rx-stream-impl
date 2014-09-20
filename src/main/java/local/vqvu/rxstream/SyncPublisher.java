package local.vqvu.rxstream;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import local.vqvu.rxstream.emitter.SyncStreamEmitter;
import local.vqvu.rxstream.util.StreamToken;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class SyncPublisher<T> extends Publisher<T> implements Iterable<T> {
    SyncPublisher(Supplier<? extends SyncStreamEmitter<? extends T>> generator) {
        super(generator);
    }

    @Override
    public SyncPublisher<T> toSynchronousPublisher() {
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        return new It();
    }

    private class It implements Iterator<T>, Subscriber<T> {
        private Subscription sub;

        private T nextValue;
        private boolean done;

        public It() {
            this.sub = null;

            this.nextValue = null;
            this.done = false;

            subscribe(this);
        }

        @Override
        public boolean hasNext() {
            ensureNextValue();
            return checkHasNext();
        }

        private void ensureNextValue() {
            if (nextValue == null && !done) {
                sub.request(1);
            }
        }

        private boolean checkHasNext() {
            return (nextValue != null);
        }

        @Override
        public T next() {
            if (hasNext()) {
                T ret = nextValue;
                nextValue = null;
                return ret;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void onSubscribe(Subscription sub) {
            this.sub = sub;
        }

        @Override
        public void onNext(T val) {
            assert(nextValue == null);
            nextValue = val;
        }

        @Override
        public void onError(Throwable t) {
            done = true;
            throw StreamToken.exception(t);
        }

        @Override
        public void onComplete() {
            done = true;
        }
    }
}
