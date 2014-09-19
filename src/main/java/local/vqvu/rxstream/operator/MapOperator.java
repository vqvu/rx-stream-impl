package local.vqvu.rxstream.operator;

import java.util.function.Function;

import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter;
import local.vqvu.rxstream.util.StreamItem;
import local.vqvu.rxstream.util.StreamItem.Type;

public final class MapOperator<T, R> implements Operator<T, R> {
    private final Function<? super T, ? extends R> mapper;

    public MapOperator(Function<? super T, ? extends R> mapper) {
        this.mapper = mapper;
    }

    @Override
    public StreamEmitter<R> apply(StreamEmitter<? extends T> source) {
        return new MapOperator.Emitter<T,R>(source, mapper);
    }

    private static class Emitter<T, R> extends TransformingStreamEmitter<T, R> {
        private final Function<? super T, ? extends R> mapper;

        public Emitter(StreamEmitter<? extends T> source,
                       Function<? super T, ? extends R> mapper) {
            super(source);
            this.mapper = mapper;
        }

        @Override
        public void emitOne(EmitCallback<? super R> cb) {
            getSource().emitOne(new DelegatingEmitCallback<T,R>(cb) {
                @Override
                public void accept(StreamItem<? extends T> item, boolean isLast) {
                    if (item.getType() == Type.VALUE) {
                        emitItem(map(item.getValue()), isLast);
                    } else {
                        emitItem(item.<R>castIfNotValue(), isLast);
                    }
                }
            });
        }

        private StreamItem<R> map(T val) {
            R mappedValue = null;
            Throwable error = null;

            try {
                mappedValue = mapper.apply(val);
            } catch (Exception e) {
                error = e;
            }

            if (error != null) {
                return StreamItem.error(error);
            } else {
                return StreamItem.value(mappedValue);
            }
        }
    }
}