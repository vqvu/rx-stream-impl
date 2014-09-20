package local.vqvu.rxstream.operator;

import local.vqvu.rxstream.Publisher;
import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.exception.StreamEmitterException;
import local.vqvu.rxstream.util.StreamToken;

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
            synchronized (lock) {
                if (endReached && childSource == null) {
                    Throwable err = new StreamEmitterException("Emitter already reached the end.");
                    cb.accept(StreamToken.error(err));
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
         * emit the next token. This method should only be called when the lock
         * is held and {@code childSource} is {@code null}.
         *
         * @param cb a callback
         */
        private void pullChildSourceAndEmit(EmitCallback<? super T> cb) {
            parentSource.emitOne(new EmitCallback<Publisher<? extends T>>() {
                @Override
                public void accept(StreamToken<? extends Publisher<? extends T>> token) {
                    synchronized(lock) {
                        if (!token.isValue()) {
                            endReached = true;
                            if (token.isError() || childSource == null) {
                                cb.accept(token.safeCast());
                            } else {
                                emitNext(cb);
                            }
                        } else {
                            Publisher<? extends T> pub = token.unwrap();
                            if (pub != null) {
                                childSource = pub.createEmitter();
                            }
                        }
                    }
                }

                @Override
                public void next() {
                    if (childSource == null) {
                        cb.next();
                    } else {
                        emitNext(cb);
                    }
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
                    public void accept(StreamToken<? extends T> token) {
                        synchronized (lock) {
                            if (!token.isValue()) {
                                childSource = null;
                                if (endReached || token.isError()) {
                                    cb.accept(token);
                                } else {
                                    next();
                                }
                            } else {
                                cb.accept(token);
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
