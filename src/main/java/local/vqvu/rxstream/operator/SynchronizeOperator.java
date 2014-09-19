package local.vqvu.rxstream.operator;

import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.SyncStreamEmitter;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter;
import local.vqvu.rxstream.util.StreamItem;


public class SynchronizeOperator<T> implements Operator<T, T> {

    @Override
    public SyncStreamEmitter<T> apply(StreamEmitter<? extends T> t) {
        return new Emitter(t);
    }

    private class Emitter extends TransformingStreamEmitter<T, T> implements SyncStreamEmitter<T> {
        private Runnable action;
        private final Object lock;

        public Emitter(StreamEmitter<? extends T> source) {
            super(source);
            this.action = null;
            this.lock = this;
        }

        @Override
        public void emitOne(final EmitCallback<? super T> cb) {
            synchronized (lock) {
                action = null;
                getSource().emitOne(new EmitCallback<T>() {
                    @Override
                    public void accept(StreamItem<? extends T> item) {
                        accept(item, false);
                    }

                    @Override
                    public void accept(StreamItem<? extends T> item, boolean emitEnd) {
                        setAction(() -> {
                            cb.accept(item, emitEnd);
                        });
                    }

                    @Override
                    public void retry() {
                        setAction(cb::retry);
                    }
                });
                waitForAction();
                action.run();
            }
        }

        private void setAction(Runnable action) {
            synchronized (this.lock) {
                this.action = action;
                lock.notifyAll();
            }
        }

        private void waitForAction() {
            synchronized (lock) {
                while (action == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
