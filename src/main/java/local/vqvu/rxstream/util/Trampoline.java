package local.vqvu.rxstream.util;

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
    private boolean emitEndNext;

    private final Object lock;

    public Trampoline(StreamEmitter<? extends T> emitter,
                    Consumer<StreamItem<? extends T>> consumer) {
        this.emitter = emitter;
        this.consumer = consumer;

        this.done = false;
        this.paused = true;
        this.inEventLoop = false;
        this.waitingOnEmit = false;
        this.emitEndNext = false;

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
            if (emitEndNext) {
                emit(StreamItem.end());
                emitEndNext = false;
            } else {
                waitingOnEmit = true;
                emitter.emitOne(new Callback());
            }
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
        }
    }

    /**
     * Signals that the {@link EmitCallback} has completed and this loop may
     * attempt to emit the next item.
     */
    private void next() {
        synchronized (lock) {
            if (emitEndNext || done || !waitingOnEmit) {
                throw new IllegalStateException("Next called too many times.");
            }

            waitingOnEmit = false;

            runEmitLoop();
        }
    }

    private void emit(StreamItem<? extends T> item) {
        consumer.accept(item);

        if (emitEndNext) {
            consumer.accept(StreamItem.end());
        }

        if (!item.isValue()) {
            stop();
        }
    }

    private class Callback implements EmitCallback<T> {
        @Override
        public void accept(StreamItem<? extends T> item) {
            emit(item);
        }

        @Override
        public void acceptLastValue(StreamItem<? extends T> item) throws IllegalArgumentException {
            if (!item.isValue()) {
                throw new IllegalArgumentException("Item must be a VALUE.");
            }

            emitEndNext = true;
            emit(item);
        }

        @Override
        public void next() throws IllegalStateException {
            Trampoline.this.next();
        }

    }
}
