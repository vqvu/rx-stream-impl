package local.vqvu.rxstream.emitter;

import java.util.Iterator;
import java.util.function.Consumer;

import local.vqvu.rxstream.util.StreamItem;
import local.vqvu.rxstream.util.StreamItem.Type;

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
     * callback. This method may be called from any thread and must eventually
     * do one of the following:
     * <ul>
     * <li>If the emitter is not ready to emit but has more data, call
     * {@link EmitCallback#next()}.
     * <li>If the emitter knows that it has exactly one value left and is ready
     * to emit that value, call {@link EmitCallback#accept(StreamItem, boolean)}
     * with the {@code value} item.
     * <li>If the emitter is ready to emit a value (and knows nothing about the
     * rest of the stream), call {@link EmitCallback#accept(StreamItem)} with
     * the {@code value} item and followed by {@link EmitCallback#next()}.
     * <li>If the emitter has no values left or has encountered an error, call
     * {@link EmitCallback#accept(StreamItem)} with the {@code error} or
     * {@code end} item.
     * </ul>
     * The emitter may perform the above actions asynchronously. That is, it
     * needs not emit anything before this method returns, and it may emit on
     * any thread (not just the calling thread).
     * <p>
     * The caller of this method is required to ensure the following:
     * <ul>
     * <li>Only one request can be "in flight" at a time. That is, once this
     * method is called once, it must not be called again until it calls
     * {@link EmitCallback#next()}.
     * <li>Once this method is called once, it must not be called again until it
     * returns <em>even if it synchronously emits to the callback</em>. Thus,
     * this method need not be reentrant.
     * <li>Once this method emits an item with {@link Type#ERROR} or
     * {@link Type#END}, it must not be called again.
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
         * Emit the item to the callback.
         *
         * @param item the item to emit.
         */
        @Override
        void accept(StreamItem<? extends T> item);

        /**
         * Emit the {@code item} to the callback, along with a {@link Type#END}
         * item on the same thread. Item must be {@link Type#VALUE}.
         * <p>
         * This method is equivalent to calling {@code
         *     accept(item);
         *     accept(StreamItem.end());
         * } over two invocations of {@link StreamEmitter#emitOne(EmitCallback)}
         * but with the benefit of not having to keep extra if the emitter knows
         * that it has no values left.
         *
         * @param item the item to emit.
         * @param emitEnd if {@code true} also emit an end item.
         * @throws IllegalArgumentException if {@code item} is not a
         *             {@code value}.
         */
        void acceptLastValue(StreamItem<? extends T> item) throws IllegalArgumentException;

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
    }
}
