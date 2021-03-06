package local.vqvu.rxstream.operator;

import java.util.ArrayList;
import java.util.List;

import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.StreamEmitter.EmitCallback;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter.TransformCallback;
import local.vqvu.rxstream.util.StreamToken;

public class BufferOperator<T> implements Operator<T,List<T>> {
    private final int bufferSize;

    public BufferOperator(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public StreamEmitter<List<T>> apply(StreamEmitter<? extends T> source) {
        return new TransformingStreamEmitter<T,List<T>>(source, new Callback());
    }

    private class Callback implements TransformCallback<T, List<T>> {
        private List<T> buffer;

        private final Object lock;

        public Callback() {
            this.buffer = null;
            this.lock = this;
        }

        @Override
        public void accept(StreamToken<? extends T> token,
                EmitCallback<? super List<T>> cb) {
            synchronized (lock) {
                if (token.isValue()) {
                    if (buffer == null) {
                        buffer = new ArrayList<>();
                    }

                    buffer.add(token.unwrap());

                    if (buffer.size() == bufferSize) {
                        cb.acceptValue(buffer);
                        buffer = null;
                    }
                    cb.next();
                } else if (token.isError()) {
                    buffer = null;
                    cb.accept(token.safeCast());
                } else {
                    if (buffer != null) {
                        cb.acceptValue(buffer);
                        buffer = null;
                    }
                    cb.acceptEnd();
                }
            }
        }
    }
}
