package local.vqvu.rxstream.operator;

import local.vqvu.rxstream.emitter.StreamEmitter.EmitCallback;
import local.vqvu.rxstream.util.StreamItem;

public abstract class DelegatingEmitCallback<T,D> implements EmitCallback<T> {
    private final EmitCallback<? super D> delegate;

    public DelegatingEmitCallback(EmitCallback<? super D> delegate) {
        this.delegate = delegate;
    }

    protected EmitCallback<? super D> getDelegate() {
        return delegate;
    }

    @Override
    public void accept(StreamItem<? extends T> item) {
        accept(item, false);
    }

    protected void emitValue(D value) {
        getDelegate().accept(StreamItem.value(value));
    }

    protected void emitError(Throwable error) {
        getDelegate().accept(StreamItem.error(error));
    }

    protected void emitEnd() {
        getDelegate().accept(StreamItem.end());
    }

    @Override
    public void retry() {
        getDelegate().retry();
    }
}
