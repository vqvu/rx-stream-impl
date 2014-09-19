package local.vqvu.rxstream.operator;

import local.vqvu.rxstream.Publisher;
import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.exception.StreamEmitterException;
import local.vqvu.rxstream.util.StreamItem;

public class ConcatOperator<T> implements Operator<Publisher<? extends T>, T> {
    @Override
    public StreamEmitter<T> apply(StreamEmitter<? extends Publisher<? extends T>> source) {
        return new Emitter<T>(source);
    }

    private static class Emitter<T> implements StreamEmitter<T> {
        private final StreamEmitter<? extends Publisher<? extends T>> parentSource;
        private StreamEmitter<? extends T> childSource;
        private boolean endReached;

        private final Object lock;

        public Emitter(StreamEmitter<? extends Publisher<? extends T>> source) {
            this.parentSource = source;
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
            parentSource.emitOne(new EmitCallback<Publisher<? extends T>>() {
                @Override
                public void accept(StreamItem<? extends Publisher<? extends T>> item, boolean isLast) {
                    synchronized(lock) {
                        if (!item.isValue()) {
                            endReached = true;
                            cb.accept(item.castIfNotValue());
                        } else {
                            endReached = isLast;
                            Publisher<? extends T> pub = item.getValue();
                            if (pub != null) {
                                childSource = pub.createEmitter();
                            }

                            // This means #next won't be called and there is
                            // nothing left to emit.
                            if (isLast && childSource == null) {
                                cb.acceptEnd();
                            }
                        }
                    }
                }

                @Override
                public void next() {
                    emitNext(cb);
                }
            });
        }

        /**
         * Attempt to emit the next from {@code childSource} to the callback.
         * This method should only be called when {@code childSource} is not
         * {@code null}.
         *
         * @param cb a callback
         */
        private void emitNext(EmitCallback<? super T> cb) {
            synchronized (lock) {
                childSource.emitOne(new EmitCallback<T>() {
                    @Override
                    public void accept(StreamItem<? extends T> item, boolean emitEnd) {
                        // TODO: Bug with ERROR item handling.
                        synchronized (lock) {
                            if (item.isEnd()) {
                                childSource = null;
                                next();
                            } else {
                                if (!emitEnd) {
                                    cb.accept(item);
                                } else {
                                    childSource = null;
                                    if (endReached) {
                                        // Nothing left, so we emit a last.
                                        cb.acceptLast(item);
                                    } else {
                                        // Otherwise, signal we are ready for next.
                                        next();
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void next() throws IllegalStateException {
                        cb.next();
                    }
                });
            }
        }
    }
}
