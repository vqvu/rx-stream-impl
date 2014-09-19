package local.vqvu.rxstream.util;

import org.reactivestreams.Subscriber;

public class StreamItem<T> {
    private final Type type;
    private final T value;
    private final Throwable error;

    private StreamItem(Type type, T value, Throwable error) {
        this.type = type;
        this.value = value;
        this.error = error;
    }

    public Type getType() {
        return type;
    }

    public boolean isValue() {
        return getType() == Type.VALUE;
    }

    public boolean isError() {
        return getType() == Type.ERROR;
    }

    public boolean isEnd() {
        return getType() == Type.END;
    }

    public T getValue() {
        return value;
    }

    public Throwable getError() {
        return error;
    }

    public void emit(Subscriber<? super T> sub) {
        switch (type) {
        case END: sub.onComplete(); break;
        case VALUE: sub.onNext(value); break;
        case ERROR: sub.onError(error); break;
        }
    }

    @SuppressWarnings("unchecked")
    public <R> StreamItem<R> castIfNotValue() {
        if (!isValue()) {
            // Cast is safe because there is no value.
            return (StreamItem<R>) this;
        } else {
            throw new ClassCastException();
        }
    }

    public void throwIfError() {
        if (!isError())
            return;

        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        } else if (error instanceof Error) {
            throw (Error) error;
        } else {
            throw new RuntimeException("Error in underlying Publisher.", error);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((error == null) ? 0 : error.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof StreamItem))
            return false;
        StreamItem<?> other = (StreamItem<?>) obj;
        if (type != other.type)
            return false;

        switch (type) {
        case END: return true;
        case VALUE:
            if (value == null) {
                return other.value == null;
            } else {
                return value.equals(other.value);
            }
        case ERROR:
            if (error == null) {
                return other.error == null;
            } else {
                return error.equals(other.error);
            }
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        switch (type) {
        case END: return "END<>";
        case VALUE: return String.format("VALUE<%s>", value);
        case ERROR: return String.format("ERROR<%s>", error);
        default: throw new RuntimeException("Bad thing happened.");
        }
    }

    public static <T> StreamItem<T> end() {
        return new StreamItem<T>(Type.END, null, null);
    }

    public static <T> StreamItem<T> error(Throwable t) {
        return new StreamItem<T>(Type.ERROR, null, t);
    }

    public static <T> StreamItem<T> value(T val) {
        return new StreamItem<T>(Type.VALUE, val, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> StreamItem<T> safeCast(StreamItem<? extends T> item) {
        return (StreamItem<T>) item;
    }

    public enum Type {
        VALUE, ERROR, END
    }
}
