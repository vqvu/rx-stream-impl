package local.vqvu.rxstream.exception;

public class EmitterTokenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EmitterTokenException(String message) {
        super(message);
    }

    public EmitterTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
