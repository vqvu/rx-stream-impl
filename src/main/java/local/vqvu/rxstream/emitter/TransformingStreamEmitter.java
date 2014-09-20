package local.vqvu.rxstream.emitter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import local.vqvu.rxstream.util.StreamToken;


public class TransformingStreamEmitter<T, R> implements StreamEmitter<R> {
    private final StreamEmitter<? extends T> source;
    private final TransformCallback<T,R> consumeCb;

    public TransformingStreamEmitter(StreamEmitter<? extends T> source, TransformCallback<T,R> consumeCb) {
        this.source = source;
        this.consumeCb = consumeCb;
    }

    @Override
    public void emitOne(EmitCallback<? super R> cb) {
        source.emitOne(new EmitCallback<T>() {
            AtomicReference<StreamToken<? extends T>> token = new AtomicReference<>(null);

            @Override
            public void accept(StreamToken<? extends T> token) {
                if (token.isValue()) {
                    this.token.set(token);
                } else {
                    consumeCb.accept(token, cb);
                }
            }

            @Override
            public void next() throws IllegalStateException {
                StreamToken<? extends T> token = this.token.getAndSet(null);
                if (token == null) {
                    cb.next();
                } else {
                    consumeCb.accept(token, cb);
                }
            }
        });
    }

    public interface TransformCallback<T, R> extends BiConsumer<StreamToken<? extends T>, EmitCallback<? super R>>{
        /**
         * Transform the {@code token} and emit the result to
         * {@link EmitCallback}. All rules for emitting to a callback specified
         * in {@link StreamEmitter#emitOne(EmitCallback)} still applies.
         */
        @Override
        void accept(StreamToken<? extends T> token, EmitCallback<? super R> cb);
    }
}
