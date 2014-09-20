package local.vqvu.rxstream.exception;

public class EmitterItemException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EmitterItemException(String message) {
        super(message);
    }

    public EmitterItemException(String message, Throwable cause) {
        super(message, cause);
    }
}
