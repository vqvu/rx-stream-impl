package local.vqvu.rxstream.util;

import local.vqvu.rxstream.exception.EmitterItemException;

import org.reactivestreams.Subscriber;

public abstract class StreamItem<T> {
    private StreamItem() {
    }

    /** Returns {@code true} if this is a {@code value} item. */
    public boolean isValue() {
        return this instanceof ValueStreamItem;
    }

    /** Returns {@code true} if this is an {@code error} item. */
    public boolean isError() {
        return this instanceof ErrorStreamItem;
    }

    /** Returns {@code true} if this is an {@code end} item. */
    public boolean isEnd() {
        return this instanceof EndStreamItem;
    }

    /**
     * Emits this item to the specified {@link Subscriber}.
     * @param sub the subscriber to emit to.
     */
    public abstract void emit(Subscriber<? super T> sub);

    /**
     * Casts this item to a {@code StreamItem<S>} if this is an error or end
     * item. This method throws a {@link ClassCastException} otherwise. Note
     * that this method will always throw an exception for values even if
     * {@code S} is a superclass of {@code T}. To do that cast, use
     * {@link #safeCast(StreamItem)}.
     *
     * @return this item, casted to the appropriate type.
     * @throws ClassCastException if this is a value item.
     */
    public abstract <S> StreamItem<S> safeCast() throws ClassCastException;

    /**
     * Unwrap this {@link StreamItem} and return its underlying value. If the
     * item is not a value, throw an exception.
     *
     * @return the unwrapped value.
     */
    public abstract T unwrap();

    /**
     * Returns a {@code value} item that wraps the given value.
     *
     * @param val the value to wrap.
     */
    public static <T> ValueStreamItem<T> value(T val) {
        return new ValueStreamItem<T>(val);
    }

    /**
     * Returns an {@code error} item that wraps the given {@link Throwable}.
     *
     * @param t the error to wrap.
     * @throws NullPointerException if {@code t} is {@code null}.
     */
    public static <T> ErrorStreamItem<T> error(Throwable t) throws NullPointerException {
        return new ErrorStreamItem<T>(t);
    }

    /**
     * Returns an {@code end} item.
     */
    public static <T> EndStreamItem<T> end() {
        return new EndStreamItem<T>();
    }

    /**
     * Safely cast the specified {@link StreamItem} to the type {@code T}.
     * Unlike, {@link #safeCast()}, this method always succeed.
     *
     * @param item the item to cast.
     * @return the same item, casted to the appropriate type.
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamItem<T> safeCast(StreamItem<? extends T> item) {
        return (StreamItem<T>) item;
    }

    /**
     * Throws the {@link Throwable} in an un-checked way by wrapping within a
     * {@link RuntimeException} when necessary. This method does not actually
     * return anything. The declared return value is to enable the following
     * call
     * <pre>
     * throw StreamItem.exception(error);
     * </pre>
     *
     * @param t the {@link Throwable} to throw.
     */
    public static RuntimeException exception(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new RuntimeException("Error in underlying Publisher.", t);
        }
    }

    public static final class ValueStreamItem<T> extends StreamItem<T> {
        private final T value;

        private ValueStreamItem(T value) {
            this.value = value;
        }

        @Override
        public void emit(Subscriber<? super T> sub) {
            sub.onNext(value);
        }

        @Override
        public <S> StreamItem<S> safeCast() throws ClassCastException {
            throw new ClassCastException();
        }

        @Override
        public T unwrap() {
            return value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ValueStreamItem))
                return false;

            ValueStreamItem<?> other = (ValueStreamItem<?>) obj;
            if ((value == null) != (other.value == null))
                return false;
            if (value != null)
                return value.equals(other.value);
            return true;
        }

        @Override
        public String toString() {
            return String.format("VALUE<%s>", value);
        }
    }

    public static final class ErrorStreamItem<T> extends StreamItem<T> {
        private final Throwable error;

        private ErrorStreamItem(Throwable error) {
            if (error == null) {
                throw new NullPointerException();
            }

            this.error = error;
        }

        public Throwable getError() {
            return error;
        }

        @Override
        public void emit(Subscriber<? super T> sub) {
            sub.onError(error);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S> ErrorStreamItem<S> safeCast() {
            return (ErrorStreamItem<S>) this;
        }

        @Override
        public T unwrap() {
            throw exception(error);
        }

        @Override
        public int hashCode() {
            return error.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof ErrorStreamItem)) {
                return false;
            }
            ErrorStreamItem<?> other = (ErrorStreamItem<?>) obj;
            return error.equals(other.error);
        }

        @Override
        public String toString() {
            return String.format("ERROR<%s>", error);
        }
    }

    public static final class EndStreamItem<T> extends StreamItem<T> {
        private EndStreamItem() {
        }

        @Override
        public void emit(Subscriber<? super T> sub) {
            sub.onComplete();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S> EndStreamItem<S> safeCast() {
            return (EndStreamItem<S>) this;
        }

        @Override
        public T unwrap() {
            throw new EmitterItemException("Attempted to unwrap an end item.");
        }

        @Override
        public int hashCode() {
            return 31;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof EndStreamItem;
        }

        @Override
        public String toString() {
            return "END<>";
        }
    }
}
