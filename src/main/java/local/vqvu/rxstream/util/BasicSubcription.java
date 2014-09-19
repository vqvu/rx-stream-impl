package local.vqvu.rxstream.util;

import java.util.concurrent.atomic.AtomicLong;

import local.vqvu.rxstream.emitter.StreamEmitter;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class BasicSubcription<T> implements Subscription {
    private final Subscriber<? super T> subscription;
    private final Trampoline<T> trampoline;

    private final AtomicLong numRequests;

    public BasicSubcription(Subscriber<? super T> sub,
                            StreamEmitter<? extends T> emitter) {
        this.subscription = sub;
        this.trampoline = new Trampoline<>(emitter, this::emit);

        this.numRequests = new AtomicLong(0);
    }

    @Override
    public void request(long num) throws IllegalArgumentException, IllegalStateException {
        if (num <= 0) {
            String format = "Argument to request() on an active Subscription "
                + "must be positive. Actual: %d";
            String msg = String.format(format, num);
            Throwable err = new IllegalArgumentException(msg);
            emit(StreamItem.<T>error(err));
            return;
        }

        if (Long.MAX_VALUE - numRequests.get() < num) {
            String format = "Cannot request more than %d pending values. "
                + "Current pending: %d, additional request: %d";
            String msg = String.format(format, Long.MAX_VALUE, numRequests, num);
            Throwable err = new IllegalStateException(msg);
            emit(StreamItem.<T>error(err));
            return;
        }

        long numPending = numRequests.addAndGet(num);
        if (numPending > 0) {
            trampoline.resume();
        }
    }

    @Override
    public void cancel() {
        trampoline.stop();
    }

    private void emit(StreamItem<? extends T> item) {
        long numPending = numRequests.decrementAndGet();

        // Immediately pause the trampoline, since the next call to item#emit
        // may cause a resume by calling #request().
        if (numPending <= 0) {
            trampoline.pause();
        }

        item.emit(subscription);
    }
}
