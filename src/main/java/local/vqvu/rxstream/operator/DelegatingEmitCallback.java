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

    @Override
    public void acceptLastValue(StreamItem<? extends T> item) throws IllegalArgumentException {
        if (!item.isValue()) {
            throw new IllegalArgumentException("Item must be a VALUE.");
        }
        accept(item, true);
    }

    protected abstract void accept(StreamItem<? extends T> item, boolean isLast);

    protected void emitValue(D value, boolean isLast) {
        emitItem(StreamItem.value(value), isLast);
    }

    protected void emitError(Throwable error) {
        getDelegate().accept(StreamItem.error(error));
    }

    protected void emitEnd() {
        getDelegate().accept(StreamItem.end());
    }

    protected void emitItem(StreamItem<? extends D> item) {
        emitItem(item, false);
    }

    protected void emitItem(StreamItem<? extends D> item, boolean isLast) {
        if (isLast && item.isValue()) {
            getDelegate().acceptLastValue(item);
        } else {
            getDelegate().accept(item);
        }
    }

    @Override
    public void next() throws IllegalStateException {
        getDelegate().next();
    }
}
