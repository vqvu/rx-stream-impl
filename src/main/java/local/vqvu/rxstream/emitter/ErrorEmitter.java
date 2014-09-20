package local.vqvu.rxstream.emitter;

import local.vqvu.rxstream.util.StreamItem;

/**
 * A {@link StreamEmitter} that immediately emits an error.
 *
 * @author vqvu
 *
 * @param <T>
 */
public class ErrorEmitter<T> implements SyncStreamEmitter<T> {
    private final Throwable error;

    public ErrorEmitter(Throwable error) {
        this.error = error;
    }

    @Override
    public void emitOne(EmitCallback<? super T> cb) {
        cb.accept(StreamItem.error(error));
    }
}
