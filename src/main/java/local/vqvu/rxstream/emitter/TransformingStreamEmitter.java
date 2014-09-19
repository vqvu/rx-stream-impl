package local.vqvu.rxstream.emitter;


public abstract class TransformingStreamEmitter<T, R> implements StreamEmitter<R> {
    private final StreamEmitter<? extends T> source;

    public TransformingStreamEmitter(StreamEmitter<? extends T> source) {
        this.source = source;
    }

    protected StreamEmitter<? extends T> getSource() {
        return source;
    }
}
