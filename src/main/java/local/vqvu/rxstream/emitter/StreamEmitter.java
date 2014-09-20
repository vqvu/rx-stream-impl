package local.vqvu.rxstream.emitter;

import java.util.Iterator;
import java.util.function.Consumer;

import local.vqvu.rxstream.util.StreamItem;

/**
 * A {@code StreamEmitter} is the asynchronous dual of an {@link Iterator}.
 * Instead of having an {@link Iterator#next()} method that returns data, it has
 * an {{@link #emitOne(EmitCallback)} method that eventually pushes a value to a
 * specified {@link EmitCallback}.
 * <p>
 * This type is intended to be covariant, so it is always safe to upcast a
 * {@code StreamEmitter<T>} to {@code StreamEmitter<R>} where R is a superclass
 * of T.
 *
 * @author vqvu
 *
 * @param <T>
 *
 * @see StreamItem
 */
public interface StreamEmitter<T> {
    /**
     * Request that the emitter emit one {@link StreamItem} object via the
     * callback. This method may be called from any thread and must cause one of
     * the following to eventually happen:
     * <ul>
     * <li>If the emitter is not ready to emit but has more data, call
     * {@link EmitCallback#next()}.
     * <li>If the emitter is ready to emit a value, call
     * {@link EmitCallback#accept(StreamItem)} with the {@code value} item.
     * Then, it must call either {@link EmitCallback#accept(StreamItem)} with an
     * end item or call {@link EmitCallback#next()}.
     * <li>If the emitter encountered an error, it must call
     * {@link EmitCallback#accept(StreamItem)} with the error.
     * </ul>
     * After the emitter has performed one of the above actions, it must not
     * make call any other {@link EmitCallback} methods until this method is
     * called again. However, the emitter may perform the above actions
     * asynchronously. That is, it needs not emit anything before this method
     * returns, and it may emit on any thread (not just the calling thread).
     * <p>
     * The caller of this method is required to ensure the following:
     * <ul>
     * <li>Only one request can be "in flight" at a time. That is, once this
     * method is called once, it must not be called again until it calls
     * {@link EmitCallback#next()}.
     * <li>Once this method is called once, it must not be called again until it
     * returns <em>even if it synchronously emits to the callback</em>. Thus,
     * this method need not be reentrant.
     * <li>Once this method emits an {@code error} or {@code end} item, it must
     * not be called again.
     * </ul>
     *
     * @param cb the callback to push the emitted item to.
     */
    void emitOne(EmitCallback<? super T> cb);

    /**
     * Safely casts the emitter from some subtype of {@code T} to {@code T}.
     *
     * @param emitter the emitter to cast.
     * @return an emitter with generic type {@code T}
     */
    @SuppressWarnings("unchecked")
    static <T> StreamEmitter<T> safeCast(StreamEmitter<? extends T> emitter) {
        return (StreamEmitter<T>) emitter;
    }

    /**
     * The callback object to use with the {@link StreamEmitter} interface.
     *
     * @author vqvu
     *
     * @param <T>
     */
    interface EmitCallback<T> extends Consumer<StreamItem<? extends T>> {
        /**
         * Emit the item to the callback. Same as {@code accept(item, false)}.
         * This method may optionally throw an {@link IllegalStateException} if
         * the {@link StreamEmitter} did not follow the contract outlined in
         * {@link StreamEmitter#emitOne(EmitCallback)}.
         *
         * @param item the item to emit.
         * @throws IllegalStateException if this method is called in a way that
         *             violates the contract outlined in
         *             {@link StreamEmitter#emitOne(EmitCallback)}.
         * @see StreamEmitter#emitOne(EmitCallback)
         */
        @Override
        void accept(StreamItem<? extends T> item) throws IllegalStateException;

        /**
         * Signals to the owner of the callback that the emitter is ready for
         * another call to {@link StreamEmitter#emitOne(EmitCallback)}. This
         * method may optionally throw an {@link IllegalStateException} if the
         * {@link StreamEmitter} did not follow the contract outlined in
         * {@link StreamEmitter#emitOne(EmitCallback)}.
         *
         * @throws IllegalStateException if this method is called in a way that
         *             violates the contract outlined in
         *             {@link StreamEmitter#emitOne(EmitCallback)}.
         * @see StreamEmitter#emitOne(EmitCallback)
         */
        void next() throws IllegalStateException;

        /**
         * Calls {@link #accept(StreamItem)} with the specified value.
         *
         * @param value the value to emit.
         * @throws IllegalStateException if this method is called in a way that
         *             violates the contract outlined in
         *             {@link StreamEmitter#emitOne(EmitCallback)}.
         * @see {@link #accept(StreamItem)}
         * @see StreamEmitter#emitOne(EmitCallback)
         */
        default void acceptValue(T value) throws IllegalStateException {
            accept(StreamItem.value(value));
        }

        /**
         * Calls {@link #accept(StreamItem)} with the specified value.
         *
         * @param error the error to emit.
         * @throws IllegalStateException if this method is called in a way that
         *             violates the contract outlined in
         *             {@link StreamEmitter#emitOne(EmitCallback)}.
         * @see {@link #accept(StreamItem)}
         * @see StreamEmitter#emitOne(EmitCallback)
         */
        default void acceptError(Throwable error) throws IllegalStateException {
            accept(StreamItem.error(error));
        }

        /**
         * Calls {@link #accept(StreamItem)} with an {@code end} item.
         *
         * @throws IllegalStateException if this method is called in a way that
         *             violates the contract outlined in
         *             {@link StreamEmitter#emitOne(EmitCallback)}.
         * @see {@link #accept(StreamItem)}
         * @see StreamEmitter#emitOne(EmitCallback)
         */
        default void acceptEnd() throws IllegalStateException {
            accept(StreamItem.end());
        }

    }
}
