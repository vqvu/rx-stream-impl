package local.vqvu.rxstream.emitter;

import java.util.Iterator;

import local.vqvu.rxstream.util.StreamToken;

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
        StreamToken<T> token;
        if (delegate.hasNext()) {
            try {
                token = StreamToken.<T>value(delegate.next());
            } catch (RuntimeException e) {
                token = StreamToken.error(e);
            }
        } else {
            token = StreamToken.end();
        }
        cb.accept(token);

        if (token.isValue()) {
            cb.next();
        }
    }

}
