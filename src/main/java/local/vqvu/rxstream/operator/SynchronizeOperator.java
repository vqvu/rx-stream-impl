package local.vqvu.rxstream.operator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.SyncStreamEmitter;
import local.vqvu.rxstream.emitter.TransformingStreamEmitter;
import local.vqvu.rxstream.util.StreamItem;


public class SynchronizeOperator<T> implements Operator<T, T> {

    @Override
    public SyncStreamEmitter<T> apply(StreamEmitter<? extends T> t) {
        return new Emitter<>(t);
    }

    private static class Emitter<T> extends TransformingStreamEmitter<T, T> implements SyncStreamEmitter<T> {
        private static final Runnable STOP_SENTINAL = () -> {};

        private final BlockingQueue<Runnable> actionQueue;
        private final Object lock;

        public Emitter(StreamEmitter<? extends T> source) {
            super(source);
            this.actionQueue = new LinkedBlockingDeque<>(3);
            this.lock = this;
        }

        @Override
        public void emitOne(EmitCallback<? super T> cb) {
            synchronized (lock) {
                getSource().emitOne(new EmitCallback<T>() {
                    @Override
                    public void accept(StreamItem<? extends T> item) {
                        setAction(() -> {
                            cb.accept(item);
                        });
                        if (!item.isValue()) {
                            setAction(STOP_SENTINAL);
                        }
                    }

                    @Override
                    public void acceptLastValue(StreamItem<? extends T> item) throws IllegalArgumentException {
                        setAction(() -> {
                            cb.acceptLastValue(item);
                        });
                        setAction(STOP_SENTINAL);
                    }

                    @Override
                    public void next() throws IllegalStateException {
                        setAction(cb::next);
                        setAction(STOP_SENTINAL);
                    }
                });

                while (true) {
                    Runnable action;
                    try {
                        action = actionQueue.take();
                        if (action == STOP_SENTINAL) {
                            break;
                        } else {
                            action.run();
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        private void setAction(Runnable action) {
            actionQueue.add(action);
        }
    }
}
