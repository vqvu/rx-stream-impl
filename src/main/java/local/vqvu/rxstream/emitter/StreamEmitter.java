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
 */
public interface StreamEmitter<T> {
    /**
     * Request that the emitter emit one {@link StreamItem} object via the
     * callback. This method may be called from any thread. It must eventually
     * emit exactly one item (or call {@link EmitCallback#retry()}) for every
     * call. However, it needs not do so before the call returns, and it may
     * emit the item on any thread (not just the calling thread). The caller is
     * required to ensure the following:
     * <ul>
     * <li>Only one request can be "in flight" at a time. That is, once this
     * method is called once, it must not be called again until it emits to the
     * callback (i.e., calls one of the callback methods).
     * <li>Once this method is called once, it must not be called again until it
     * returns <em>even if it synchronously emits to the callback</em>. Thus,
     * this method need not be reentrant.
     * <li>Once this method emits an item with {@link Type} {@link Type#ERROR}
     * or {@link Type#END}, it must not be called again.
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
         * Emit the {@code item} to the callback, with an option to also emit a
         * {@link Type#END} item on the same thread.
         * <p>
         * If {@code item} is a value, then this method is equivalent to calling
         * {@code
         *     accept(item);
         *     accept(StreamItem.end());
         * }
         * but with the benefit that of being atomic. That is, calling
         * {@link #accept(StreamItem)} twice can introduce a race condition
         * where {@link StreamEmitter#emitOne(EmitCallback)} may be called again
         * in between the two {@code accept()} calls. This method is useful when
         * an emitter knows that it has run out of data and does not wish to
         * keep extra state just to
         * <p>
         * If {@code item} is an error or end item, then this method is
         * equivalent to calling {@code accept(item)}.
         *
         * @param item the item to emit.
         * @param emitEnd if {@code true} also emit an end item.
         */
        void accept(StreamItem<? extends T> item, boolean emitEnd);

        /**
         * Request that the owner of the callback retry its call to
         * {@link StreamEmitter#emitOne(EmitCallback)}. This method is useful
         * for implementing transforming emitters that must pull from another
         * {@link StreamEmitter} source.
         */
        void retry();
    }
}
