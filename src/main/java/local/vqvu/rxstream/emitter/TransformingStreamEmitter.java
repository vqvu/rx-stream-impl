package local.vqvu.rxstream.emitter;

import java.util.concurrent.atomic.AtomicReference;

import local.vqvu.rxstream.util.StreamItem;


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
            AtomicReference<StreamItem<? extends T>> item = new AtomicReference<>(null);

            @Override
            public void accept(StreamItem<? extends T> item) {
                if (item.isValue()) {
                    this.item.set(item);
                } else {
                    consumeCb.accept(item, cb);
                }
            }

            @Override
            public void next() throws IllegalStateException {
                StreamItem<? extends T> item = this.item.getAndSet(null);
                if (item == null) {
                    cb.next();
                } else {
                    consumeCb.accept(item, cb);
                }
            }
        });
    }

    public interface TransformCallback<T, R> {
        /**
         *
         */
        void accept(StreamItem<? extends T> item, EmitCallback<? super R> cb);
    }
}
