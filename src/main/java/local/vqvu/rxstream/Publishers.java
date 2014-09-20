package local.vqvu.rxstream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Supplier;

import local.vqvu.rxstream.emitter.ErrorEmitter;
import local.vqvu.rxstream.emitter.IteratorEmitter;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.SyncStreamEmitter;
import local.vqvu.rxstream.operator.ConcatOperator;

public class Publishers {
    private Publishers() {}

    public static <T> SyncPublisher<T> empty() {
        return just();
    }

    @SafeVarargs
    public static <T> SyncPublisher<T> just(T...vals) {
        return from(Arrays.asList(vals));
    }

    public static <T> SyncPublisher<T> from(final Iterator<? extends T> iterator) {
        return createSync(new Supplier<SyncStreamEmitter<T>>() {
            private boolean firstTime = true;

            @Override
            public SyncStreamEmitter<T> get() {
                if (firstTime) {
                    firstTime = false;
                    return new IteratorEmitter<T>(iterator);
                } else {
                    return new ErrorEmitter<T>(new RuntimeException("Single-use Publisher."));
                }
            }
        });
    }

    public static <T> SyncPublisher<T> from(final Iterable<? extends T> iterable) {
        return createSync(() -> {
            return new IteratorEmitter<>(iterable.iterator());
        });
    }

    public static <T> SyncPublisher<T> error(final Throwable err) {
        return createSync(() -> {
            return new ErrorEmitter<T>(err);
        });
    }

    public static <T> Publisher<T> create(Supplier<? extends StreamEmitter<? extends T>> generator) {
        return new Publisher<T>(generator);
    }

    public static <T> SyncPublisher<T> createSync(Supplier<? extends SyncStreamEmitter<? extends T>> generator) {
        return new SyncPublisher<T>(generator);
    }

    public static <T> Publisher<T> concat(Publisher<? extends Publisher<? extends T>> source) {
        return source.transform(new ConcatOperator<T>());
    }
}
