package local.vqvu.rxstream.emitter;

import local.vqvu.rxstream.util.StreamToken;

/**
 * A specialization of the {@link StreamEmitter} for the synchronous case.
 *
 * @author vqvu
 *
 * @param <T>
 */
public interface SyncStreamEmitter<T> extends StreamEmitter<T> {
    /**
     * Request that the emitter emit one {@link StreamToken} object via the
     * callback. Unlike the more general
     * {@link StreamEmitter#emitOne(EmitCallback)}, this method must
     * synchronously emit to the callback before it returns. All caller and
     * callee responsibilities remain the same.
     *
     * @param cb the callback to push the emitted token to.
     * @see StreamEmitter#emitOne(EmitCallback)
     */
    @Override
    void emitOne(EmitCallback<? super T> cb);
}