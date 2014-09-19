package local.vqvu.rxstream.exception;

public class StreamEmitterException extends RuntimeException {
    private static final long serialVersionUID = 8885632127222752995L;

    public StreamEmitterException(String message) {
        super(message);
    }

    public StreamEmitterException(String message, Throwable cause) {
        super(message, cause);
    }
}
