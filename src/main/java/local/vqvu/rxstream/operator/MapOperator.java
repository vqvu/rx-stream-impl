package local.vqvu.rxstream.operator;

import java.util.function.Function;

import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.StreamEmitter.EmitCallback;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter.TransformCallback;
import local.vqvu.rxstream.util.StreamToken;

public final class MapOperator<T, R> implements Operator<T, R> {
    private final Function<? super T, ? extends R> mapper;

    public MapOperator(Function<? super T, ? extends R> mapper) {
        this.mapper = mapper;
    }

    @Override
    public StreamEmitter<R> apply(StreamEmitter<? extends T> source) {
        return new TransformingStreamEmitter<T,R>(source, new Callback());
    }

    private class Callback implements TransformCallback<T, R> {

        @Override
        public void accept(StreamToken<? extends T> token, EmitCallback<? super R> cb) {
            cb.accept(map(token));
        }

        private StreamToken<R> map(StreamToken<? extends T> token) {
            if (!token.isValue()) {
                return token.safeCast();
            }

            T val = token.unwrap();
            R mappedValue = null;
            Throwable error = null;

            try {
                mappedValue = mapper.apply(val);
            } catch (Exception e) {
                error = e;
            }

            if (error != null) {
                return StreamToken.error(error);
            } else {
                return StreamToken.value(mappedValue);
            }
        }
    }
}