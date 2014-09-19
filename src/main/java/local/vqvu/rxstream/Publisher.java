package local.vqvu.rxstream;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.SyncStreamEmitter;
import local.vqvu.rxstream.operator.BufferOperator;
import local.vqvu.rxstream.operator.MapOperator;
import local.vqvu.rxstream.operator.SynchronizeOperator;
import local.vqvu.rxstream.util.BasicSubcription;

import org.reactivestreams.Subscriber;

public class Publisher<T> implements org.reactivestreams.Publisher<T> {
    private final Supplier<? extends StreamEmitter<? extends T>> generator;

    Publisher(Supplier<? extends StreamEmitter<? extends T>> generator) {
        this.generator = generator;
    }

    public <R> Publisher<R> map(Function<? super T, ? extends R> mapper) {
        return transform(new MapOperator<T, R>(mapper));
    }

    public <R> Publisher<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return Publishers.concat(map(mapper));
    }

    public Publisher<List<T>> buffer(int size) {
        return transform(new BufferOperator<T>(size));
    }

    public Publisher<T> concat(Publisher<? extends T> pub) {
        return Publishers.concat(Publishers.just(this, pub));
    }

    public <R> Publisher<R> transform(final Operator<? super T, ? extends R> operator) {
        return Publishers.create(() -> {
            return safeCast(operator.apply(createEmitter()));
        });
    }

    public SyncPublisher<T> toSynchronousPublisher() {
        return Publishers.createSync(() -> {
            StreamEmitter<T> emitter = createEmitter();
            if (emitter instanceof SyncStreamEmitter) {
                return (SyncStreamEmitter<T>) emitter;
            } else {
                return new SynchronizeOperator<T>().apply(emitter);
            }
        });
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        BasicSubcription<T> sub = new BasicSubcription<>(subscriber, createEmitter());
        subscriber.onSubscribe(sub);
    }

    public StreamEmitter<T> createEmitter() {
        synchronized (generator) {
            return safeCast(generator.get());
        }
    }

    /**
     * Safely casts the emitter from some subtype of {@code T} to {@code T}.
     *
     * @param emitter the emitter to cast.
     * @return an emitter with generic type {@code T}
     */
    @SuppressWarnings("unchecked")
    private static <T> StreamEmitter<T> safeCast(StreamEmitter<? extends T> emitter) {
        // StreamEmitter is covariant, so this is ok..
        return (StreamEmitter<T>) emitter;
    }

    public interface Operator<T, R> extends
        Function<StreamEmitter<? extends T>, StreamEmitter<R>> {
        // This should ideally be a typedef...if Java had such a thing.
    }
}
