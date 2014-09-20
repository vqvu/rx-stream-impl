package local.vqvu.rxstream.operator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

import local.vqvu.rxstream.Publisher.Operator;
import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.SyncStreamEmitter;
import local.vqvu.rxstream.util.StreamItem;


public class SynchronizeOperator<T> implements Operator<T, T> {

    @Override
    public SyncStreamEmitter<T> apply(StreamEmitter<? extends T> t) {
        return new Emitter<>(t);
    }

    private static class Emitter<T> implements SyncStreamEmitter<T> {
        private final StreamEmitter<? extends T> source;
        private final BlockingQueue<Supplier<Boolean>> actionQueue;

        public Emitter(StreamEmitter<? extends T> source) {
            this.source = source;
            this.actionQueue = new LinkedBlockingDeque<>(2);
        }

        @Override
        public void emitOne(EmitCallback<? super T> cb) {
            source.emitOne(new EmitCallback<T>() {
                @Override
                public void accept(StreamItem<? extends T> item) {
                    setAction(() -> {
                        cb.accept(item);
                        return !item.isValue();
                    });
                }

                @Override
                public void next() throws IllegalStateException {
                    setAction(() -> {
                        cb.next();
                        return true;
                    });
                }
            });

            while (true) {
                try {
                    Supplier<Boolean> shouldContinue = actionQueue.take();
                    if (shouldContinue.get()) {
                        break;
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        private void setAction(Supplier<Boolean> action) {
            actionQueue.add(action);
        }
    }
}
