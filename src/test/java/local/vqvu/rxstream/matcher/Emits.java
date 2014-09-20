package local.vqvu.rxstream.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import local.vqvu.rxstream.Publisher;
import local.vqvu.rxstream.util.StreamToken;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Emits<T> extends AsyncEmitterMatcher<Publisher<T>, T> {
    public Emits(List<StreamToken<T>> expected) {
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
        List<StreamToken<T>> tokens = new ArrayList<>();
        for (T val : vals) {
            tokens.add(StreamToken.value(val));
        }
        return emits(tokens);
    }

    @SafeVarargs
    public static <T> Emits<T> emits(StreamToken<T>...tokens) {
        return emits(Arrays.asList(tokens));
    }

    public static <T> Emits<T> emits(List<StreamToken<T>> tokens) {
        return new Emits<T>(tokens);
    }
}
