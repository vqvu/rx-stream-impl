package local.vqvu.rxstream.operator;

import local.vqvu.rxstream.Publisher;
import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter;
import local.vqvu.rxstream.exception.StreamEmitterException;
import local.vqvu.rxstream.util.StreamItem;

public class ConcatOperator<T> implements Operator<Publisher<? extends T>, T> {
    @Override
    public StreamEmitter<T> apply(StreamEmitter<? extends Publisher<? extends T>> source) {
        return new Emitter<T>(source);
    }

    private static class Emitter<T> extends TransformingStreamEmitter<Publisher<? extends T>, T> {
        private StreamEmitter<? extends T> childSource;
        private boolean endReached;

        private final Object lock;

        public Emitter(StreamEmitter<? extends Publisher<? extends T>> source) {
            super(source);
            this.childSource = null;
            this.endReached = false;
            this.lock = this;
        }

        @Override
        public void emitOne(EmitCallback<? super T> cb) {
            synchronized (this) {
                if (endReached && childSource == null) {
                    Throwable err = new StreamEmitterException("Emitter already reached the end.");
                    cb.accept(StreamItem.<T>error(err));
                    return;
                }

                if (childSource == null) {
                    pullChildSourceAndEmit(cb);
                } else {
                    emitNext(cb);
                }
            }
        }

        /**
         * Attempt to pull the next child source, and if successful, attempt to
         * emit the next item. This method should only be called when the lock
         * is held and {@code childSource} is {@code null}.
         *
         * @param cb a callback
         */
        private void pullChildSourceAndEmit(EmitCallback<? super T> cb) {
            getSource().emitOne(new DelegatingEmitCallback<Publisher<? extends T>, T>(cb) {
                @Override
                public void accept(StreamItem<? extends Publisher<? extends T>> item, boolean emitEnd) {
                    synchronized(lock) {
                        if (!item.isValue()) {
                            endReached = true;
                            getDelegate().accept(item.<T>castIfNotValue());
                        } else {
                            Publisher<? extends T> pub = item.getValue();
                            if (pub != null) {
                                childSource = pub.createEmitter();

                                if (emitEnd) {
                                    endReached = true;
                                }

                                emitNext(getDelegate());
                            } else {
                                if (emitEnd) {
                                    getDelegate().accept(StreamItem.<T>end());
                                } else {
                                    getDelegate().retry();
                                }
                            }
                        }
                    }
                }
            });
        }

        /**
         * Attempt to emit the next from {@code childSource} to the callback.
         * This method should only be called when the lock is held and
         * {@code childSource} is not {@code null}.
         *
         * @param cb a callback
         */
        private void emitNext(EmitCallback<? super T> cb) {
            childSource.emitOne(new DelegatingEmitCallback<T,T>(cb) {
                @Override
                public void accept(StreamItem<? extends T> item, boolean emitEnd) {
                    synchronized (lock) {
                        if (item.isEnd()) {
                            childSource = null;
                            getDelegate().retry();
                        } else {
                            if (emitEnd) {
                                childSource = null;
                            }
                            getDelegate().accept(item, emitEnd && endReached);
                        }
                    }
                }
            });
        }
    }
}
