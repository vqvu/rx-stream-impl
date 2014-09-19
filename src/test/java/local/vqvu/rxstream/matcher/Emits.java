package local.vqvu.rxstream.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import local.vqvu.rxstream.Publisher;
import local.vqvu.rxstream.util.StreamItem;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Emits<T> extends AsyncEmitterMatcher<Publisher<T>, T> {
    public Emits(List<StreamItem<T>> expected) {
        super(Publisher.class, expected);
    }

    @Override
    protected void computeActuals(Publisher<T> pub) {
        pub.subscribe(new Subscriber<T>() {
            Subscription sub;

            @Override
            public void onSubscribe(Subscription sub) {
                this.sub = sub;
                this.sub.request(1);
            }

            @Override
            public void onNext(T val) {
                addValue(val);
                sub.request(1);
            }

            @Override
            public void onError(Throwable t) {
                addError(t);
            }

            @Override
            public void onComplete() {
                addEnd();
            }
        });
    }

    public static <T> Emits<T> emitsNothing() {
        return emitsValues();
    }

    @SafeVarargs
    public static <T> Emits<T> emitsValues(T...vals) {
        return emitsValues(Arrays.asList(vals));
    }

    public static <T> Emits<T> emitsValues(List<T> vals) {
        List<StreamItem<T>> items = new ArrayList<>();
        for (T val : vals) {
            items.add(StreamItem.value(val));
        }
        return emits(items);
    }

    @SafeVarargs
    public static <T> Emits<T> emits(StreamItem<T>...items) {
        return emits(Arrays.asList(items));
    }

    public static <T> Emits<T> emits(List<StreamItem<T>> items) {
        return new Emits<T>(items);
    }
}
