package local.vqvu.rxstream.operator;

import java.util.ArrayList;
import java.util.List;

import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter;
import local.vqvu.rxstream.util.StreamItem;

public class BufferOperator<T> implements Operator<T,List<T>> {
    private final int bufferSize;

    public BufferOperator(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public StreamEmitter<List<T>> apply(StreamEmitter<? extends T> source) {
        return new Emitter(source);
    }

    private class Emitter extends TransformingStreamEmitter<T, List<T>> {
        private List<T> buffer;

        private final Object lock;

        public Emitter(StreamEmitter<? extends T> source) {
            super(source);
            this.buffer = null;
            this.lock = this;
        }

        @Override
        public void emitOne(EmitCallback<? super List<T>> cb) {
            getSource().emitOne(new DelegatingEmitCallback<T, List<T>>(cb) {

                @Override
                public void accept(StreamItem<? extends T> item, boolean emitEnd) {
                    synchronized (lock) {
                        if (item.isValue()) {
                            if (buffer == null) {
                                buffer = new ArrayList<>();
                            }

                            buffer.add(item.getValue());

                            if (buffer.size() == bufferSize) {
                                emitBuffer(false);
                            } else {
                                getDelegate().retry();
                            }
                        } else if (item.isError()) {
                            buffer = null;
                            getDelegate().accept(item.<List<T>>castIfNotValue());
                        } else {
                            emitBuffer(true);
                        }
                    }
                }

                private void emitBuffer(boolean emitEnd) {
                    StreamItem<List<T>> bufferItem = StreamItem.value(buffer);
                    buffer = null;
                    getDelegate().accept(bufferItem, emitEnd);
                }
            });
        }

    }
}
