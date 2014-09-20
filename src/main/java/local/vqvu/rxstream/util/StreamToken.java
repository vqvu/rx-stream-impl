package local.vqvu.rxstream.util;

import local.vqvu.rxstream.exception.EmitterTokenException;

import org.reactivestreams.Subscriber;

public abstract class StreamToken<T> {
    private StreamToken() {
    }

    /** Returns {@code true} if this is a {@code value} token. */
    public boolean isValue() {
        return this instanceof ValueStreamToken;
    }

    /** Returns {@code true} if this is an {@code error} token. */
    public boolean isError() {
        return this instanceof ErrorStreamToken;
    }

    /** Returns {@code true} if this is an {@code end} token. */
    public boolean isEnd() {
        return this instanceof EndStreamToken;
    }

    /**
     * Emits the wrapped value of this token to the specified
     * {@link Subscriber}.
     *
     * @param sub the subscriber to emit to.
     */
    public abstract void emit(Subscriber<? super T> sub);

    /**
     * Casts this token to a {@code StreamToken<S>} if this is an error or end
     * token. This method throws a {@link ClassCastException} otherwise. Note
     * that this method will always throw an exception for values even if
     * {@code S} is a superclass of {@code T}. To do that cast, use
     * {@link #safeCast(StreamToken)}.
     *
     * @return this token, casted to the appropriate type.
     * @throws ClassCastException if this is a value token.
     */
    public abstract <S> StreamToken<S> safeCast() throws ClassCastException;

    /**
     * Unwrap this {@link StreamToken} and return its underlying value. If the
     * token is not a value, throw an exception.
     *
     * @return the unwrapped value.
     */
    public abstract T unwrap();

    /**
     * Returns a {@code value} token that wraps the given value.
     *
     * @param val the value to wrap.
     */
    public static <T> ValueStreamToken<T> value(T val) {
        return new ValueStreamToken<T>(val);
    }

    /**
     * Returns an {@code error} token that wraps the given {@link Throwable}.
     *
     * @param t the error to wrap.
     * @throws NullPointerException if {@code t} is {@code null}.
     */
    public static <T> ErrorStreamToken<T> error(Throwable t) throws NullPointerException {
        return new ErrorStreamToken<T>(t);
    }

    /**
     * Returns an {@code end} token.
     */
    public static <T> EndStreamToken<T> end() {
        return new EndStreamToken<T>();
    }

    /**
     * Safely cast the specified {@link StreamToken} to the type {@code T}.
     * Unlike, {@link #safeCast()}, this method always succeed.
     *
     * @param token the token to cast.
     * @return the same token, casted to the appropriate type.
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamToken<T> safeCast(StreamToken<? extends T> token) {
        return (StreamToken<T>) token;
    }

    /**
     * Throws the {@link Throwable} in an un-checked way by wrapping within a
     * {@link RuntimeException} when necessary. This method does not actually
     * return anything. The declared return value is to enable the following
     * call
     * <pre>
     * throw StreamToken.exception(error);
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

    public static final class ValueStreamToken<T> extends StreamToken<T> {
        private final T value;

        private ValueStreamToken(T value) {
            this.value = value;
        }

        @Override
        public void emit(Subscriber<? super T> sub) {
            sub.onNext(value);
        }

        @Override
        public <S> StreamToken<S> safeCast() throws ClassCastException {
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
            if (!(obj instanceof ValueStreamToken))
                return false;

            ValueStreamToken<?> other = (ValueStreamToken<?>) obj;
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

    public static final class ErrorStreamToken<T> extends StreamToken<T> {
        private final Throwable error;

        private ErrorStreamToken(Throwable error) {
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
        public <S> ErrorStreamToken<S> safeCast() {
            return (ErrorStreamToken<S>) this;
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

            if (!(obj instanceof ErrorStreamToken)) {
                return false;
            }
            ErrorStreamToken<?> other = (ErrorStreamToken<?>) obj;
            return error.equals(other.error);
        }

        @Override
        public String toString() {
            return String.format("ERROR<%s>", error);
        }
    }

    public static final class EndStreamToken<T> extends StreamToken<T> {
        private EndStreamToken() {
        }

        @Override
        public void emit(Subscriber<? super T> sub) {
            sub.onComplete();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S> EndStreamToken<S> safeCast() {
            return (EndStreamToken<S>) this;
        }

        @Override
        public T unwrap() {
            throw new EmitterTokenException("Attempted to unwrap an end token.");
        }

        @Override
        public int hashCode() {
            return 31;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof EndStreamToken;
        }

        @Override
        public String toString() {
            return "END<>";
        }
    }
}
