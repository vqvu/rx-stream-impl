package local.vqvu.rxstream.emitter;

import java.util.Iterator;

import local.vqvu.rxstream.util.StreamItem;

/**
 * A {@link StreamEmitter} backed by an {@link Iterator}.
 *
 * @author vqvu
 *
 * @param <T>
 */
public class IteratorEmitter<T> implements SyncStreamEmitter<T> {
    private final Iterator<? extends T> delegate;

    public IteratorEmitter(Iterator<? extends T> iter) {
        this.delegate = iter;
    }

    @Override
    public void emitOne(EmitCallback<? super T> cb) {
        StreamItem<T> item;
        if (delegate.hasNext()) {
            try {
                item = StreamItem.<T>value(delegate.next());
            } catch (RuntimeException e) {
                item = StreamItem.error(e);
            }
        } else {
            item = StreamItem.end();
        }
        cb.accept(item);
    }

}
