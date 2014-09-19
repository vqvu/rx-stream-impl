package local.vqvu.rxstream.emitter;

import java.util.function.BiConsumer;

import local.vqvu.rxstream.util.StreamItem;

public interface ConsumableStreamEmitter<T> extends StreamEmitter<T> {
    @Override
    public default void emitOne(EmitCallback<? super T> cb) {

    }

    public default void consume(ConsumeCallback<T> cb) {

    }

    interface ConsumeCallback<T> extends BiConsumer<StreamItem<T>, EmitCallback<T>> {
        /**
         *
         */
        @Override
        void accept(StreamItem<T> item, EmitCallback<T> cb);
    }
}
