package local.vqvu.rxstream.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import local.vqvu.rxstream.emitter.StreamEmitter;
import local.vqvu.rxstream.emitter.StreamEmitter.EmitCallback;

public class Trampoline<T> {
    private final StreamEmitter<? extends T> emitter;
    private final Consumer<StreamItem<? extends T>> consumer;

    private boolean done;
    private boolean paused;
    private boolean inEventLoop;
    private boolean waitingOnEmit;

    private final Object lock;

    public Trampoline(StreamEmitter<? extends T> emitter,
                    Consumer<StreamItem<? extends T>> consumer) {
        this.emitter = emitter;
        this.consumer = consumer;

        this.done = false;
        this.paused = true;
        this.inEventLoop = false;
        this.waitingOnEmit = false;

        this.lock = this;
    }

    /**
     * Resume emitting if the loop is currently paused.
     */
    public void resume() {
        synchronized (lock) {
            paused = false;
            runEmitLoop();
        }
    }

    private void runEmitLoop() {
        // Don't recurse into the event loop.
        if (inEventLoop) {
            return;
        }

        inEventLoop = true;
        while (!paused && !done && !waitingOnEmit) {
            waitingOnEmit = true;
            emitter.emitOne(new Callback());
        }
        inEventLoop = false;
    }

    /**
     * Pause emitting if the loop is currently emitting. The pause may not be
     * immediate. The loop may emit one more item after this call returns if it
     * was already in the process of emitting.
     */
    public void pause() {
        synchronized (lock) {
            paused = true;
        }
    }

    /**
     * Stop emitting forever. Subsequent calls to {@link #resume()} will do
     * nothing.
     */
    public void stop() {
        synchronized (lock) {
            pause();
            done = true;
            waitingOnEmit = false;
        }
    }

    /**
     * Signals that the {@link EmitCallback} has completed and this loop may
     * attempt to emit the next item.
     */
    private void next() {
        synchronized (lock) {
            waitingOnEmit = false;

            runEmitLoop();
        }
    }

    private void emit(StreamItem<? extends T> item) {
        synchronized (lock) {
            if (done) {
                return;
            }

            waitingOnEmit = false;
            consumer.accept(item);

            if (!item.isValue()) {
                stop();
            }
        }
    }

    private class Callback implements EmitCallback<T> {
        private static final int STATE_UNUSED = 0;
        private static final int STATE_SEEN_VALUE = 1;
        private static final int STATE_SEEN_END_OR_NEXT = 2;

        private final AtomicInteger state;

        public Callback() {
            this.state = new AtomicInteger(STATE_UNUSED);
        }

        private void checkNotUsed(int nextState, String method) throws IllegalStateException {
            if (state.getAndSet(nextState) >= nextState) {
                throw new IllegalStateException(method + " called too many times.");
            }
        }

        @Override
        public void accept(StreamItem<? extends T> item) throws IllegalStateException {
            checkNotUsed(item.isValue() ? STATE_SEEN_VALUE : STATE_SEEN_END_OR_NEXT, "accept");
            emit(item);
        }

        @Override
        public void next() throws IllegalStateException {
            checkNotUsed(STATE_SEEN_END_OR_NEXT, "next");
            Trampoline.this.next();
        }

    }
}
